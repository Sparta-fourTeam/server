# 앱 시작 점검·최소 버전 정책

앱 시작 시 점검 여부와 최소 앱 버전을 확인하는 정책이다. API 명세는 ApiDog가 기준이다.

## 정책 기본값

로그인 없이 `GET /api/client/status`로 현재 정책을 조회한다. 인증·DB·Redis가 필요 없고 `/api/data/version`과는 독립적이다.

최초 예제인 `deploy/client-policy/policy.json`과, `APP_CLIENT_POLICY_FILE`을 **설정하지 않았을 때**의 서버 기본값은 모두 **NORMAL, enforcementEnabled=false, allowLegacy=true**다. 즉 차단 OFF이고 기존 앱도 허용한다.

- `platforms`에 android/ios별 `minimumBuild`, `recommendedBuild`, `storeUrl`을 둔다. 배포 트랙에서 설치 가능함을 확인한 HTTPS 스토어 URL만 넣는다.
- build는 Android versionCode, iOS CFBundleVersion이다. 1~10자리 숫자 성분 최대 4개를 숫자로 비교한다(`1.10 > 1.9`, `1 == 1.0`).
- `enforcementEnabled`, `allowLegacy`는 생략하지 않는 JSON boolean이다. `revision`은 1~128자의 영문자·숫자·`.`·`_`·`:`·`-`만 허용한다.
- 변수 미설정은 위의 차단 OFF 기본값이다. 반대로 **변수로 파일을 명시했는데** 그 파일이 없거나 손상되면 OFF로 되돌아가지 않고 fail-closed다. 서버는 점검 상태로 차단하고 앱은 상태 확인 실패로 막는다. 경로 오타나 mount 누락이 곧 점검 상태가 되므로 활성화 전에 파일을 읽는지 확인한다.
- 앱 GAME 요청은 `X-Game-Platform`, `X-Game-Build`, `X-Game-Version` 헤더를 보낸다. 이 헤더는 호환 정책용이며 앱 무결성 검증이 아니다.

## 차단 범위

| 상태 | GAME 동작 |
|---|---|
| 차단 OFF | 모든 앱 허용. 헤더가 없거나 잘못돼도 통과 |
| NORMAL + 차단 ON | 플랫폼 minimum 미만·잘못된 헤더·미지원 플랫폼은 426 `CLIENT_UPDATE_REQUIRED`. 헤더 없는 구버전은 `allowLegacy`를 따름 |
| DRAINING + 차단 ON | 새 battle/start와 새 GAME 로그인·가입은 503 `DRAINING`. 진행 중이던 전투의 progress/open/state/abandon/end/cancel과 필요한 refresh는 최소 버전과 무관하게 허용 |
| MAINTENANCE + 차단 ON | GAME HTTP 요청은 503 `MAINTENANCE`로 응답. 기존 WebSocket SEND도 차단. 예약 정리와 DataVersion 초기화 쓰기는 응답 없이 실행을 건너뛰거나 미룬다 |

- 공개 status·health·WEB 로그인·WEB 관리자 경로는 GAME gate 대상이 아니다.
- 최소 버전은 기존 전투 결과의 동일 payload 재전송을 막지 않는다.
- **MAINTENANCE는 백업용 쓰기 동결이 아니다.** 이미 통과한 요청·트랜잭션, WEB 관리자 쓰기, Hibernate ddl-auto, 수동 SQL은 막지 못한다. 백업 전에는 앱 컨테이너와 DB를 정상 종료하고 진행 중 작업을 확인한다.
- 헤더 없는 구버전에는 새 팝업이 없다. `allowLegacy=false`는 해당 앱을 일반 로그인 실패로 막으므로 영향 범위를 확인한 뒤 별도로 결정한다.
- 서버 gate 없이 클라이언트 화면만으로는 차단되지 않는다.

## 공유 정책 디렉터리 연결

nginx와 서버의 정책이 어긋나지 않도록 같은 파일을 읽게 한다.

1. `deploy/client-policy`의 예제를 검토해 호스트의 **한 디렉터리**에 둔다. nginx와 두 Spring(blue/green) 컨테이너에 그 디렉터리를 읽기 전용으로 mount한다. 단일 파일 bind mount는 교체 시 오래된 inode가 남을 수 있어 쓰지 않는다.
2. 두 Spring에 `APP_CLIENT_POLICY_FILE=/etc/client-policy/policy.json`을 설정하고, nginx HTTPS server block에 `nginx-status.conf.example`을 include한다. 기존 proxy 설정을 대체하지 않으며, 적용 전 `nginx -t`로 확인한다.
3. 외부 nginx status와 blue/green 내부 Spring status를 각각 조회해 revision, mode, enforcementEnabled, allowLegacy, 플랫폼 규칙이 같은지 비교한다. 다르거나 구버전 서버 이미지가 섞여 있으면 활성화하지 않는다.
4. 정책 변경은 같은 디렉터리의 임시 파일에 쓰고 검증한 뒤 **원자적 rename**으로 교체한다. 정책 파일 복사본을 따로 두지 않는다.
5. nginx status는 `no-store`로 제공하고 etag·조건부 캐시·`open_file_cache`를 끈다. Spring은 요청마다 파일을 읽는다. nginx의 status 파일이 없으면 503이다.

nginx만 살아 있으면 Spring/DB가 멈춰도 status는 응답한다. EC2·nginx 전체나 DNS/TLS 장애에서는 안내도 조회할 수 없다.

## 활성화와 rollback

1. 서버와 nginx를 **차단 OFF·legacy 허용**으로 먼저 배포하고 위 일치 확인을 마친다.
2. 새 앱을 대상 플랫폼의 배포 트랙에 제공하고, 설치·업데이트가 되는지 확인한다.
3. 제공 확인 뒤에도 바로 차단하지 않는다. 사용자 공지와 유예 기간을 거친 다음에 해당 플랫폼의 minimum/recommended를 설정해 차단을 켠다. 점검도 사전 공지 후 진행한다.
4. 점검은 DRAINING → 신규 진입 차단 확인 → 기존 전투 결과 처리 확인 → MAINTENANCE → 앱 writer 정상 종료 → DB 정상 종료 순서다.
5. 재개는 DB/Redis 정상 확인 → 앱 health → status와 서버 gate의 revision 일치 확인 → NORMAL 전환 순서다. nginx는 계속 둔다.
6. **rollback**: 같은 공유 파일을 이전에 검증한 revision 또는 NORMAL·차단 OFF·legacy 허용으로 되돌린다. DB·진행 중 결과·세션은 지우지 않는다.

빈 DB 첫 기동은 Spring 한 인스턴스만 시작한다. `DataVersionInitializer`가 `DataTable`의 초기 버전 행(각 key와 `_revision`)을 commit했고 프로세스가 정상인 것을 확인한 뒤 두 번째 색상을 시작한다. 동시 최초 기동은 초기화 경합으로 안전하지 않다.
