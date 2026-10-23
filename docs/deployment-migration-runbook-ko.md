# 점검 기능 배포와 동일 버전 EC2 이전 실행안

상태: **검토·승인용 절차**. 이 문서는 운영 실행 승인이 아니다. 로컬 구현·격리 검증과 운영 적용을 구분한다. 실행자는 각 단계의 대상·성공 기준·중단 기준을 확인하고 해당 단계가 승인된 뒤에만 명령을 수행한다.

## 1. 대상과 변경 범위

| 대상 | 현재 역할 / 실행 전 확인 |
|---|---|
| 원본 운영 계정, 서울 | 현재 운영. `/home/ec2-user/app/docker-compose.yml`, project `app`. 이전 조사상 green API/Web 활성, blue 종료. 현재 상태는 다시 확인 |
| 개인 이전 계정, 서울 | 이전 목적지. `aws-project`는 이 계정이며 원본용 CLI로 사용하지 않음 |
| 런타임 | t4g.small ARM64, 암호화 gp3 30GiB. MySQL **8.0.46**, Redis **7.4.11**, nginx/API/Web은 확인된 digest 고정. 저장소 MySQL8.4로 업그레이드하지 않음 |
| 기존 주소 | `doodle-defense.duckdns.org`: 팀원 소유, 장기 의존을 없애야 함 |
| 팀 관리 주소 | `doodledefense.duckdns.org`: 이전 결정 유지. 과거 등록 결과는 목적지 IP이며 이번 문서 작업에서 DNS/TLS를 재조회·변경하지 않음 |

서버 변경은 공개 status·GAME gate·기존 WS SEND와 예약 정리/초기화 guard·공유 정책 예제다. 앱은 시작/인증 직전/신규 전투/복귀/이어하기 검사, metadata 전송, 기존 팝업 확장, 결과·refresh 보존이다. 별도 QA 빌드 도구·localhost HTTP·합성 provider는 제품 릴리스 설정이 아니다. 일반 릴리스에는 `NOVA_POLICY_QA`, `NOVA_POLICY_LOCAL`, debug package/signing/localhost 예외를 넣지 않는다.

현재 일반 앱 `ServerConfig.BaseUrl`은 **기존 주소**다. 새 주소로의 제품 코드 변경은 아직 하지 않았다. 새 주소를 채택하려면 이 상수/환경 선택과 관련 검증을 별도로 승인·수정해야 한다. API/status/WS/SSE는 같은 기본 주소에서 파생된다. Next.js의 빌드/런타임 API origin, WEB OAuth callback·cookie/CORS, 인증서 갱신, GitHub Actions `EC2_HOST`/SSH target, deploy 경로·GHCR 권한, DNS updater도 변경 대상을 확인한다. 확인되지 않은 값은 추정해 적용하지 않는다.

## 2. 준비 완료와 단계별 차단 조건

### 지금 준비된 것

- 분리된 client/server feature 브랜치의 변경. PR 검토와 운영 배포는 별도 단계다.
- 정책·인증·전투·nginx 공유 파일과 실제 BlueStacks↔격리 서버 핵심 흐름 검증 근거. QA APK는 제품/스토어 업데이트 검증을 대신하지 않음.
- 동일 MySQL/Redis 버전의 목적지 빈 데이터 기동 기록, 원본 volume/쓰기 주체/암호화 콜드 백업 후보 절차.
- 로컬 시험 stack·DB volume·reverse·중계기 정리 근거. 운영 writer를 중단했다는 뜻은 아님.

### 서버를 차단 OFF로 배포하기 전에 필요한 것

- 릴리스 source commit·image digest·현재 운영 image/구성과의 차이 확정 및 리뷰. 최신 기준과 충돌 검증. 알려지지 않은 schema/시작 DDL 변경이 있으면 별도 대응 전 배포 금지.
- 두 Spring 색상 모두 새 gate 포함, 기존 environment/volume 유지, 같은 정책 **디렉터리** readonly mount. nginx status 예제는 기존 HTTPS/proxy 전체를 대체하지 않음.
- 최초 정책 NORMAL / `enforcementEnabled=false` / `allowLegacy=true`, 운영 `nginx -t`, 외부 nginx와 양쪽 Spring status의 revision·전체 정책 일치.
- 이전 image/config·정책 원복본, 승인한 배포 담당/시각, 직전 running/queued Actions·수동 배포 없음 확인. 팀원이 배포하지 않는다고 했으므로 공통 lock 새 구현을 필수로 강요하지 않음.
- API 계약을 기존 명세 관리 절차에 반영. 실제 배포·원격 명세 수정은 별도 승인.

### 차단 활성화/앱 출시 전에 필요한 것

- 제품 package·서명·설치 build·실제 배포 트랙/대상/지역 확정. QA package와 code4를 제품 minimum으로 사용하지 않음.
- 선택한 제품 주소의 HTTPS/status/API/WS/SSE 연결 및 갱신 경로 확인. 새 주소가 빈 목적지를 가리키는 상태로 제품 앱을 먼저 배포하지 않음.
- 해당 사용자가 새 앱을 실제로 설치/업데이트할 수 있는지 확인 후 공지·유예. 이 확인 전 minimum 상향/legacy 차단 금지.
- 구버전은 새 팝업을 소급 표시할 수 없음. `allowLegacy=false`의 일괄 차단 영향과 기존 주소 종료 전환 계획 승인.

### 첫 운영 백업 전에 추가로 필요한 것

- 운영에 gate/status 배포·정책 일치 검증 완료, 공지·점검 시간·기존 전투 유예/연결 종료 영향 승인.
- 최근 progress/end·처리 중 HTTP/트랜잭션·배포 경합을 판단할 실제 관찰 수단. 현재 운영 drain 시간/최대 전투 시간은 미측정. **battle_session 행0은 기준 아님.** 엄격한 무손실 drain이 확인되지 않으면 연기하거나 남은 접속 단절 영향을 명시적으로 승인받음.
- 모든 Spring/Web 및 WEB 관리자·WS·예약 작업·DB event·외부 writer 확인. 과거 event0/외부 tablespace·keyring 없음은 실행 직전 재확인 필요.
- 여유 용량, 정상 종료·원본 재개 절차, 사용자 직접 GPG 암호 입력과 비운영 encrypt/decrypt 시험, 암호화 파일 보관 위치·접근자·기간 확정. 암호를 채팅/argv/env/문서로 받지 않음.

### 최종 전환 전에만 필요한 것

- 격리 **현재 데이터** 복원 리허설 성공. 로컬 합성 시험/빈 DB 시험/오래된 AMI는 이 조건을 충족하지 않음.
- 대상 TLS·서비스 설정·비밀 설정 이전 범위·이미지 접근, 새 주소와 구주소 전환/종료 협조, 양쪽 동시 쓰기 방지·되돌림 담당 확정.
- 운영 tutorial 정의·보상/upgrade 참조와 사용자 기록 등 핵심 데이터가 복원 전후 보존됨 확인. 로컬 tutorial404는 fixture 누락으로 분류되며 운영 정의 부재의 증거는 아님.

전체 폰/기기 조합, iOS(이번 출시 대상이 Android만이면), 모든 tutorial/콘텐츠·WS/SSE 메시지 QA, 성능·다중 인스턴스/ECS/RDS 개선은 별도 단계다. 단, 릴리스 대상의 실제 업데이트 가능 여부·새 주소 HTTPS·기존 전투/결과 복구는 해당 차단/전환 전에 생략하지 않는다.

## 3. 구버전·주소 전환의 최소 순서

1. 기존 운영 주소에서 서버/status를 차단 OFF로 먼저 배포한다. 앱/DB를 이전하거나 minimum을 올리는 작업과 묶지 않는다.
2. 새 팀 관리 주소를 **현재 정상 운영 원본에서도 제공할 전환 경로**를 승인·검증한다. 단순 권장안은 새 DNS를 원본 IP로 임시 전환하고 원본 nginx에 새 hostname의 유효 TLS/갱신을 마련하는 것이다. 현재 새 DNS는 목적지 IP이므로 이 DNS·인증서·nginx 변경은 별도 승인 대상이며 아직 미실행이다. 다른 임시 proxy 경로를 택하면 데이터 전달/보안 범위부터 승인한다.
3. 정상 제공된 새 주소를 사용하는 일반 제품 앱을 빌드·검증·제공한다. 기존 팝업과 실제 store URL/package/track 확인 후 공지·유예한다. QA localhost APK를 배포하지 않는다.
4. 구주소 앱의 잔존 기간·팀원 협조·구주소 인증서 유지/종료일을 정한다. 최종 전환 후 필요하면 구주소 nginx를 새 단일 원본으로 임시 proxy하는 방안을 별도 승인한다. 단순 redirect로 POST·인증·WS가 보존된다고 가정하지 않는다. 구주소의 원본 Spring은 재개해 이중 쓰기하지 않는다.
5. 최종 전환 때 새 주소를 목적지로 바꾼다. 목적지 TLS는 미리 검증 가능한 방식으로 준비한다. 독립 인증서 발급/기존 인증서 비밀 전송은 각각 별도 승인. 전체 EC2/nginx가 꺼지거나 DNS/TLS가 실패하면 정적 안내도 조회할 수 없다.

새 주소 도입과 이전을 분리하면 사용자는 같은 새 주소를 계속 사용하고 마지막에 DNS 대상만 바뀐다. 팀원 소유 구주소를 영구 유지하는 계획이 아니다.

## 4. 단계별 실행·진행/중단 기준

| 단계 | 승인 후 실행 | 성공 기준 / 실패 시 |
|---|---|---|
| A. OFF 배포 | 확정 image·공유 디렉터리·nginx 구성 적용 | NORMAL/OFF/legacy, 양 색상·nginx 정책 일치. 불일치면 활성화하지 않고 이전 구성/image로 복구 |
| B. 앱/공지 준비 | 새 주소 HTTPS·제품 앱 제공·공지/유예 | 실제 업데이트 가능, 안내/복귀/기존 결과 복구 확인. 미달이면 minimum·점검 활성화 보류 |
| C. DRAINING | 새 GAME 로그인/가입/battle start 차단, 기존 progress/end/refresh 유지 | 실제 신규 차단·기존 접수/요청 종료 확인. minimum을 동시에 올리지 않음. 엄격 drain 불명은 성공으로 간주하지 않음 |
| D. 쓰기 종료 | 승인한 시점 MAINTENANCE → 모든 Spring blue/green·Next blue/green·관리자/외부 writer 정상 종료 | nginx는 정적 안내용 유지. gate만으로 동결됐다고 가정하지 않음. 강제 종료/불명은 백업 금지 |
| E. 첫 콜드 백업 | MySQL·Redis 정상 종료 → 두 volume 전체를 같은 점검의 암호화 세트로 읽음 | engine 종료/파일 안정·pipeline·checksum·복호화 archive 읽기 검증. 실패하면 산출물 성공 취급 금지, 원본 재개 우선 |
| F. 원본 재개 | 기존 MySQL/Redis → 직전 활성 green API/Web → 상태/정책 일치 → 승인한 NORMAL | healthy/PONG·기존 image/upstream·공개 경로 확인. 오류면 M 유지. 원본 재개 뒤 첫 백업은 즉시 과거 시점 자료가 됨 |
| G. 격리 복원 리허설 | 별도 승인 후 암호화 백업 전송·복호화·volume 복원·합성 쓰기 검증 | source 운영은 계속. 대상은 외부 egress/웹 차단·관리 SSH만, DNS updater/cron/시작 배포·외부 side effect를 **기동 전** 막음. DB/세션/소유권/결과 멱등·tutorial 등 보존 검증 |
| H. 최종 점검/전환 | 다시 C~E로 최신 백업 → 검증된 목적지 복원 → 양쪽 M에서 health/데이터/TLS 확인 → 새 주소 전환 → 목적지만 NORMAL | 첫 리허설 백업 재사용 금지. source는 쓰기 중단 상태 유지. 단일 쓰기 원본과 두 주소의 실제 요청 도착점 확인 |

### 콜드 백업 범위와 명령 원칙

이전 조사상 source의 실제 데이터는 `/var/lib/docker/volumes/app_mysql-data/_data`, `/var/lib/docker/volumes/app_redis-data/_data`다. 실행 직전에 mount·경로·여유 공간을 다시 확인한다. MySQL datadir 외 파일/keyring/binlog가 새로 발견되면 백업 세트에 포함할 별도 승인/계획 없이 진행하지 않는다.

- MySQL8.0.46·Redis7.4.11 ARM64 digest를 보존한다. Redis `/data` 전체, AOF base/increment/manifest·RDB를 함께 보존한다. 세션·JTI·차단 상태를 임의 삭제하지 않는다. TTL은 실제 시간 경과로 만료될 수 있으며 WS/SSE 연결 자체는 복원되지 않는다.
- 앱 → DB 엔진 정상 종료 순서를 지킨다. `docker stop --time=-1` 같은 정상 종료 대기 방식과 별도 관찰/중단 기준을 사용한다. timeout 뒤 자동 SIGKILL을 정상 종료로 취급하지 않는다. 응답이 없으면 강제 kill 없이 상태를 확인하고 추가 조치를 승인받는다.
- DB 종료0·OOM false·정상 shutdown 근거 확인 후 GNU tar의 numeric owner/권한/ACL/xattr/sparse 보존 스트림을 GPG AES256에 연결한다. 평문 archive를 staging에 쓰지 않는다. `pipefail`, 암호화 파일 mode600·폴더700, ciphertext SHA256과 복호화/archive 읽기 성공을 확인한다. 이것은 실제 restore 성공의 대체가 아님.
- backup에는 계정/인증 테이블·Redis 상태가 포함된다. `.env`·개인키·인증서 등 별도 비밀 설정은 DB volume 백업에 끼워 넣지 않는다. 이 설정들의 이전은 별도 범위·전달 경로 승인 후 수행한다.
- 성공/실패 모두 원본을 먼저 재개한다. 기존 컨테이너 `start`로 직전 image/설정을 복구하며 `pull`, `deploy.sh`, `up --build`, 기존 volume 덮어쓰기를 섞지 않는다. 이전 조사와 현재 실행 서비스가 다르면 hardcoded green 명령을 쓰지 않고 갱신한다.

첫 점검30~60분, 최종 전환60~90분은 **미측정 계획 가정**이다. 실제 drain·암호화·전송·복원·기동을 측정해 확정한다. 단계별 UTC/KST 시각과 실패/재개 시간을 기록하고 마감 시 불명 상태를 성공으로 선언하지 않는다.

## 5. rollback 경계

| 시점 | 허용할 복구 방향 |
|---|---|
| 정책/새 서버 배포 실패, 데이터 이전 전 | 이전 검증 policy revision 또는 NORMAL/OFF/legacy로 atomic 복구. DB가 꺼져 있으면 정책부터 열지 않음. image/config rollback은 schema/데이터 호환 확인 후 |
| 첫 백업 실패 | 백업 산출물에 의존하지 않고 기존 DB/Redis·직전 활성 API/Web 재개. 실패 파일은 격리 표시하며 승인 없이 삭제하지 않음 |
| 원본 재개 후 격리 리허설 실패 | 원본을 유지하고 목적지 시험만 중단/격리. 첫 백업으로 현재 원본을 덮어쓰지 않음 |
| 최종 전환 검증 중, 목적지 새 쓰기 전 | 양쪽 M 유지, 목적지에 새 사용자 쓰기가 없음을 확인한 뒤 DNS/proxy와 원본을 복구 가능. 상태 불명이면 이 경로를 사용하지 않음 |
| 목적지가 새 쓰기를 받은 후 | DNS만 과거 원본으로 되돌리거나 오래된 volume을 덮어쓰지 않음. 양쪽 신규 진입·writer를 동결하고 목적지 최신 데이터를 보존한 뒤 forward-fix 우선 또는 승인된 역이전 절차 수립. 두 DB의 임의 병합/선별 SQL 복구 금지 |

기존 source volume·암호화 백업·목적지 리허설 데이터를 자동 삭제하지 않는다. 정상화 확인·보관 기간·정확한 삭제 대상을 별도 승인받는다. 이 단계에서 snapshot/새 AWS 자원/권한·키 생성은 필수가 아니며 승인 없이 만들지 않는다.

## 6. 최소 사용자 결정 묶음

이미 정한 무료 팀 관리 DuckDNS와 동일 버전 EC2 방향은 다시 선택받지 않는다. 실행 전 필요한 결정은 다음 세 묶음이다.

1. **배포/주소 준비:** 확정 source/image 검토 후 OFF 서버 배포, 새 주소의 원본 임시 제공·TLS와 제품 주소 변경/앱 제공을 단계별로 진행할지. 각 원격 쓰기는 구체 결과를 검토한 뒤 승인.
2. **첫 점검:** KST 날짜·시간/상한, 공지 대상·문구·유예, 남은 전투가 있으면 연기할지 또는 연결 종료 영향을 수용할지. 첫 점검은 백업 후 원본 재개하며 최종 전환을 함께 하지 않는 안을 권장.
3. **데이터 취급:** MySQL/Redis 전체 암호화 세트·사용자 직접 암호 입력·원본 staging/Mac 비동기화 보관 위치·접근자·기간(7일은 제안), 이후 격리 목적지 전송/복원 범위. 최종 주소 전환·원본 종료/삭제는 별도 승인.

현재 가장 가까운 행동은 **릴리스 변경/commit·image 후보 확정과 새 주소 제공 경로 승인 준비**다. 이 문서 작성으로 운영 배포·백업·중단·DNS 변경·store/push가 실행된 것은 아니다.
