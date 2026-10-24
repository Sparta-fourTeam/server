#!/usr/bin/env python3
"""Start inactive app, validate health, switch nginx, then stop the previous app. Run through the reviewed manual workflow, never on the retired host."""
import fcntl, hashlib, json, os, pathlib, re, subprocess, sys, tempfile, time
APP = pathlib.Path('/home/ec2-user/app')
NGINX = pathlib.Path('/opt/teamdoodle-https/nginx/default.conf')
ENV = '/home/ec2-user/migration-prep-20261023/config/.env'

def run(args):
    p = subprocess.run(args, capture_output=True, text=True)
    if p.returncode:
        raise RuntimeError('Command failed: ' + args[0])  # never expose env or application logs
    return p.stdout

def write(path, value):
    fd, name = tempfile.mkstemp(dir=path.parent, prefix='.deploy-', text=True)
    try:
        os.fchmod(fd, 0o600)
        with os.fdopen(fd, 'w') as out:
            if isinstance(value, str): out.write(value)
            else: json.dump(value, out, indent=2)
        os.replace(name, path)
    finally:
        if os.path.exists(name): os.unlink(name)

def main():
    service, expected_image, archive_hash = sys.argv[1:]
    if service not in ('spring-green', 'nextjs-green'):
        raise RuntimeError('Only app services are supported')
    if not re.fullmatch(r'sha256:[0-9a-f]{64}', expected_image) or not re.fullmatch(r'[0-9a-f]{64}', archive_hash):
        raise RuntimeError('Invalid image identity')
    # This directory exists only on the migrated production host. Abort before docker mutation.
    if not (APP / 'data/20261024T084212Z/app_mysql-data/_data').is_dir():
        raise RuntimeError('Not the migrated production host')
    with open(APP / '.app-deployment.lock', 'a') as lock:
        fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)  # shared by both repositories
        config = APP / 'docker-compose.yml'
        document = json.loads(config.read_text())
        if not document['networks']['data'].get('internal'):
            raise RuntimeError('Private DB network required')
        if document['services'][service].get('ports'):
            raise RuntimeError('App host ports must remain closed')
        archive = pathlib.Path(__file__).resolve().parent / 'release-image.tar'
        h = hashlib.sha256()
        with archive.open('rb') as data:
            for block in iter(lambda: data.read(1024 * 1024), b''): h.update(block)
        if h.hexdigest() != archive_hash: raise RuntimeError('Archive checksum mismatch')
        run(['docker', 'load', '-i', str(archive)])
        metadata = json.loads(run(['docker', 'image', 'inspect', expected_image]))[0]
        if metadata['Architecture'] != 'arm64' or metadata['Os'] != 'linux':
            raise RuntimeError('linux/arm64 image required')
        archive.unlink()  # only the transferred archive, not images or backups
        base = ['docker', 'compose', '--env-file', ENV, '-p', 'app', '-f', str(config)]
        original = json.loads(json.dumps(document))
        prefix = service.rsplit('-', 1)[0]
        proxy_before = NGINX.read_text()
        pattern = r'(proxy_pass\s+http://' + re.escape(prefix) + r')-(blue|green)(:\d+;)'
        matches = list(re.finditer(pattern, proxy_before))
        if len(matches) != 1:
            raise RuntimeError('Expected exactly one app upstream')
        active = prefix + '-' + matches[0].group(2)
        inactive = prefix + ('-blue' if active.endswith('-green') else '-green')
        # Preserve mounts, policy and DB configuration; add no host ports or dependencies.
        document['services'][inactive] = json.loads(json.dumps(document['services'][active]))
        document['services'][inactive]['image'] = expected_image
        document['services'][inactive]['pull_policy'] = 'never'
        proxy_after = re.sub(pattern, lambda m: m.group(1) + '-' + inactive.rsplit('-',1)[1] + m.group(3), proxy_before)
        def healthy():
            path = '/actuator/health' if service == 'spring-green' else '/api/health'
            port = '8080' if service == 'spring-green' else '3000'
            for _ in range(60):
                command = ['curl','-fsS','--max-time','3','http://localhost:' + port + path] if service == 'spring-green' else ['node','-e',"require('http').get('http://localhost:3000/api/health',r=>{r.resume();process.exitCode=r.statusCode===200?0:1}).on('error',()=>process.exit(1))"]
                p = subprocess.run(['docker','exec','app-' + inactive + '-1'] + command,capture_output=True,text=True,timeout=8)
                if p.returncode == 0:
                    if service != 'spring-green' or json.loads(p.stdout).get('status') == 'UP': return
                time.sleep(2)
            raise RuntimeError('Application health timeout')
        try:
            write(config, document)
            run(base + ['up','-d','--no-deps','--pull','never', inactive])
            healthy()  # old app continues serving throughout startup
            write(NGINX, proxy_after)
            run(['docker','exec','teamdoodle-preparation-nginx','nginx','-t'])
            run(['docker','exec','teamdoodle-preparation-nginx','nginx','-s','reload'])
        except Exception:
            # Restore routing while old app is still running. Never restart the active app or DB.
            write(NGINX, proxy_before)
            run(['docker','exec','teamdoodle-preparation-nginx','nginx','-t'])
            run(['docker','exec','teamdoodle-preparation-nginx','nginx','-s','reload'])
            run(base + ['stop','--timeout','120', inactive])
            write(config, original)
            raise RuntimeError('Deployment failed; previous app kept serving') from None
        # Keep old service definition/image for the next blue/green deployment; no pruning.
        # A failure here leaves both apps running, not a stale-data rollback.
        run(base + ['stop','--timeout','120', active])
        print(json.dumps({'service':inactive,'previous':active,'image':expected_image,'health':'UP','database_services_changed':False}))

if __name__ == '__main__': main()
