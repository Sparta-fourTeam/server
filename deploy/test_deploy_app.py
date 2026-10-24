import importlib.util, json, pathlib, tempfile, unittest
from unittest.mock import patch
import subprocess

spec = importlib.util.spec_from_file_location('deploy_app', pathlib.Path(__file__).with_name('deploy-app.py'))
m = importlib.util.module_from_spec(spec); spec.loader.exec_module(m)
NEW = 'sha256:' + 'a'*64
OLD = 'sha256:' + 'b'*64

class DeploymentTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory(); self.addCleanup(self.tmp.cleanup)
        self.root = pathlib.Path(self.tmp.name)
        (self.root/'data/20261024T084212Z/app_mysql-data/_data').mkdir(parents=True)
        self.config = self.root/'docker-compose.yml'
        self.config.write_text(json.dumps({'networks':{'data':{'internal':True}},'services':{'spring-green':{'image':OLD},'nextjs-green':{'image':OLD},'mysql':{'image':'mysql-untouched'},'redis':{'image':'redis-untouched'}}}))
        self.archive = self.root/'release-image.tar'; self.archive.write_bytes(b'synthetic image')
        self.sha = m.hashlib.sha256(self.archive.read_bytes()).hexdigest()
        self.proxy=self.root/'nginx.conf';self.proxy.write_text('location /api/ { proxy_pass http://spring-green:8080; }\nlocation / { proxy_pass http://nextjs-green:3000; }')
        self.commands = []; self.failure = False; self.fail_rollback = False; self.nginx_fail=False; self.arch='arm64'
    def run_command(self, args):
        self.commands.append(args)
        if args[-2:]==['nginx','-t'] and self.nginx_fail:
            self.nginx_fail=False;raise RuntimeError('nginx syntax error')
        if args[:3] == ['docker','image','inspect']: return json.dumps([{'Architecture':self.arch,'Os':'linux'}])
        if args[:2] == ['docker','inspect']: return json.dumps([{'Image':OLD}])
        return ''
    def health(self, args, **kwargs):
        self.commands.append(args)
        bad = self.failure and json.loads(self.config.read_text())['services'].get('spring-blue',{}).get('image') == NEW
        return subprocess.CompletedProcess(args, 1 if bad or self.fail_rollback else 0, '{"status":"UP"}', '')
    def invoke(self, service='spring-green', checksum=None):
        with patch.object(m,'NGINX',self.proxy), patch.object(m,'APP',self.root), patch.object(m,'__file__',str(self.root/'deploy-app.py')), patch.object(m.sys,'argv',['deploy',service,NEW,checksum or self.sha]), patch.object(m,'run',side_effect=self.run_command), patch.object(m.subprocess,'run',side_effect=self.health), patch.object(m.time,'sleep'):
            m.main()
    def test_success_only_app_no_db_start(self):
        self.invoke()
        up = [c for c in self.commands if 'up' in c]
        self.assertEqual(len(up),1); self.assertEqual(up[0][-1],'spring-blue'); self.assertIn('--no-deps',up[0])
        self.assertEqual(json.loads(self.config.read_text())['services']['mysql']['image'],'mysql-untouched')
        self.assertFalse(self.archive.exists())
        self.assertTrue(any('reload' in c for c in self.commands))
        stop=[c for c in self.commands if 'stop' in c];self.assertEqual(stop[0][-1],'spring-green')
        self.assertIn('http://spring-blue:8080',self.proxy.read_text())
        reload_index=next(i for i,c in enumerate(self.commands) if 'reload' in c)
        stop_index=next(i for i,c in enumerate(self.commands) if 'stop' in c)
        self.assertLess(reload_index,stop_index)
    def test_retired_host_refused_before_docker(self):
        import shutil; shutil.rmtree(self.root/'data')
        with self.assertRaisesRegex(RuntimeError,'Not the migrated'): self.invoke()
        self.assertEqual(self.commands,[])
    def test_corrupt_archive_no_docker(self):
        with self.assertRaisesRegex(RuntimeError,'checksum'): self.invoke(checksum='0'*64)
        self.assertEqual(self.commands,[])
    def test_database_service_refused(self):
        with self.assertRaisesRegex(RuntimeError,'Only app'): self.invoke('mysql')
        self.assertEqual(self.commands,[])
    def test_failed_health_restores_previous_image(self):
        self.failure=True
        with self.assertRaisesRegex(RuntimeError,'previous app kept serving'): self.invoke()
        self.assertEqual(json.loads(self.config.read_text())['services']['spring-green']['image'],OLD)
        self.assertEqual(len([c for c in self.commands if 'up' in c]),1)
        self.assertNotIn('spring-blue',json.loads(self.config.read_text())['services'])
        self.assertIn('http://spring-green:8080',self.proxy.read_text())
        self.assertFalse(any(c[-1]=='spring-green' and 'stop' in c for c in self.commands))
    def test_frontend_uses_existing_node_not_curl(self):
        self.invoke('nextjs-green')
        self.assertTrue(any('node' in c for c in self.commands)); self.assertFalse(any('curl' in c for c in self.commands))
    def test_noninternal_network_refused(self):
        d=json.loads(self.config.read_text());d['networks']['data']['internal']=False;self.config.write_text(json.dumps(d))
        with self.assertRaisesRegex(RuntimeError,'Private DB'):self.invoke()
        self.assertEqual(self.commands,[])
    def test_nginx_failure_keeps_old_app(self):
        self.nginx_fail=True
        with self.assertRaisesRegex(RuntimeError,'previous app kept serving'):self.invoke()
        self.assertIn('http://spring-green:8080',self.proxy.read_text())
        self.assertFalse(any(c[-1]=='spring-green' and 'stop' in c for c in self.commands))
    def test_wrong_architecture_never_starts_app(self):
        self.arch='amd64'
        with self.assertRaisesRegex(RuntimeError,'linux/arm64'):self.invoke()
        self.assertFalse(any('up' in c for c in self.commands))

    def test_second_deploy_blue_to_green(self):
        d=json.loads(self.config.read_text());d['services']['spring-blue']=d['services']['spring-green'].copy();self.config.write_text(json.dumps(d))
        self.proxy.write_text(self.proxy.read_text().replace('spring-green','spring-blue'))
        self.invoke()
        self.assertIn('http://spring-green:8080',self.proxy.read_text())
        self.assertTrue(any(c[-1]=='spring-green' and 'up' in c for c in self.commands))
        self.assertTrue(any(c[-1]=='spring-blue' and 'stop' in c for c in self.commands))

    def test_lock_collision_refused(self):
        with open(self.root/'.app-deployment.lock','a') as lock:
            m.fcntl.flock(lock,m.fcntl.LOCK_EX|m.fcntl.LOCK_NB)
            with self.assertRaises(BlockingIOError):self.invoke()
        self.assertEqual(self.commands,[])

if __name__ == '__main__': unittest.main()
