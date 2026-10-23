# 앱 시작 점검·업데이트 정책

이 변경은 로컬 구현이다. 운영 배포·공지·정책 변경·차단·백업은 수행하지 않았다. ApiDog가 API 명세의 기준이므로 이 계약을 배포 전에 담당자가 ApiDog에 반영해야 한다. 이번 권한은 로컬 코드/검증까지라 외부 명세를 수정하지 않았다.

실제 배포·첫 백업·복원 리허설·최종 전환 순서는 [배포·이전 실행안](deployment-migration-runbook-ko.md)을 따른다. 이 문서는 정책 계약이며 운영 실행 승인이 아니다.

## 계약과 안전한 기본값

로그인 없이 `GET /api/client/status`를 호출한다. `deploy/client-policy/policy.json`이 최초 예제이며 **NORMAL, enforcementEnabled=false, allowLegacy=true**다. 서버의 `APP_CLIENT_POLICY_FILE`도 미설정이면 동일한 차단 OFF 정책이다. 데이터 테이블 `/api/data/version`과는 독립적이다.

```json
{
  "schemaVersion": 1,
  "revision": "operator-selected-revision",
  "enforcementEnabled": false,
  "mode": "NORMAL",
  "message": "",
  "estimatedEnd": null,
  "retryAfterSeconds": 30,
  "allowLegacy": true,
  "platforms": {}
}
```

`platforms`에는 android/ios별 `minimumBuild`, `recommendedBuild`, `storeUrl`을 둔다. 실제 배포 트랙·앱 식별자·다운로드 가능 여부를 검증한 HTTPS Play/App Store URL만 등록한다. 표시용 `versionName`과 설치 build는 다르다. Android는 설치 패키지의 versionCode, iOS는 CFBundleVersion을 사용한다. build는 1~10자리 ASCII 숫자 성분 최대4개를 숫자로 비교한다. `1.10 > 1.9`, `1 == 1.0`이며 음수·접미사·공백은 거부한다. Android 유효 versionCode의 상위 범위도 처리한다.

`enforcementEnabled`와 `allowLegacy`는 생략하지 않는 JSON boolean이다. `revision`은 1~128자의 영문자·숫자·점·밑줄·콜론·하이픈만 허용한다. 누락된 boolean을 false로 추정하거나 줄바꿈이 포함된 revision을 응답 헤더에 넣지 않는다. 잘못된 파일은 서버에서 점검 상태, 앱에서는 조회 실패로 처리한다.

앱 GAME API/SSE/WS 요청은 `X-Game-Platform`, `X-Game-Build`, `X-Game-Version`을 보낸다. GET status는 인증·DB·Redis가 필요 없다. 서버 gate의 WEB 예외는 검증된 토큰/관리자·전용 WEB 경로로 판단하며 임의 헤더로 WEB을 선언해 우회할 수 없다. build 헤더는 앱 호환 정책용이며 앱 무결성 검증을 대신하지 않는다.

| 상태 | GAME 동작 |
|---|---|
| 차단 OFF | 기존 앱·missing/invalid metadata도 호환 유지. 권장 안내는 건너뛸 수 있음 |
| NORMAL + 차단 ON | 해당 플랫폼 minimum 이상 통과. 낮은/invalid/미지원 metadata는 426 `CLIENT_UPDATE_REQUIRED`. 양쪽 헤더 없는 구버전은 allowLegacy에 따름 |
| DRAINING + 차단 ON | 새 battle/start와 새 GAME 로그인/가입은 503 `DRAINING`. 기존 battle progress/open/state/abandon/end/cancel 및 필요한 refresh는 최소 버전 차단보다 우선해 허용 |
| MAINTENANCE + 차단 ON | GAME HTTP 요청·기존 WS SEND·예약 정리 및 DataVersion 초기화 쓰기 차단. 503 `MAINTENANCE` |

공개 status·health·전용 WEB 로그인·WEB 관리자 경로는 GAME gate 대상이 아니다. 최소 버전은 기존 전투 결과의 동일 payload 재전송을 차단하지 않는다. 기존 end 멱등 처리 코드는 변경하지 않았다.

**이 gate는 백업용 쓰기 동결이 아니다.** 이미 통과한 요청·트랜잭션, WEB 관리자 쓰기, 시작 시 Hibernate ddl-auto, 수동 SQL·다른 프로세스는 별도다. 백업 전에는 모든 Spring/Next 컨테이너와 DB 엔진을 정상 종료하고 진행 중 작업을 확인해야 한다. 이번에 이 동작을 실행하지 않았다.

## 클라이언트 UI와 보존 규칙

기존 LoadingFlow·StoreAppUpdateChecker·ConfirmPopupView를 사용한다. 권장은 나중에/닫기 허용. 필수 업데이트/점검/상태 확인 실패는 닫기·취소·다른 일반 팝업으로 통과할 수 없다. 업데이트 버튼은 스토어를 여는 동작이며 허가가 아니다. 매번 정책을 다시 읽고, foreground 복귀 시 열려 있는 정책 모달도 재검사한다. 앱 수명 취소는 진입 성공으로 바뀌지 않는다.

신규 전투와 저장된 전투 이어하기도 상태를 다시 확인한다. DRAINING에서 기존 전투 Resume는 허용한다. foreground의 진행 중 전투는 기존 StageManager.Pause/Resume를 사용하며 강제 포기·로그아웃·씬 이탈·pending payload 삭제를 하지 않는다. 점검 중인 동안 모달이 유지된다. token refresh의 명시적 정책 거절은 아직 소비하지 않은 refresh 토큰을 보존한다. 정책 426도 최종 데이터 거절로 취급하지 않아 기존 멱등 키를 버리지 않는다.

DRAINING의 기존 전투 이어하기는 최소 build보다 우선한다. 권장 안내를 보는 동안 foreground 재검사가 요청돼도 이전 정상 응답으로 진입하지 않고 다시 조회한다. foreground 검사 취소·실패는 자동 Resume 허가가 아니며, 해당 검사가 직접 Pause한 전투만 정상 허가 후 Resume한다.

네트워크/타임아웃/잘못된 JSON은 '상태 확인 실패'로 막고 재시도한다. 정상 점검이라고 거짓 표시하지 않는다. 최초 status 404만 기존 서버 rollout 전 advisory 호환으로 처리하며, 같은 실행에서 지원되는 status를 본 뒤의 404는 실패다. 따라서 API gate 없이 클라이언트 화면만 보안 수단으로 삼으면 안 된다. Local 인증 backend는 정책 네트워크 조회를 하지 않는다. 스토어 자체의 권장 업데이트 조회 실패는 기존처럼 skip 가능하다.

미지원 구버전에 새 점검/업데이트 팝업을 소급 적용할 수 없다. metadata 없는 앱을 막기 위해 allowLegacy=false를 올리면 기존 앱은 일반 로그인 실패/재시도만 볼 수 있다. 헤더 없는 앱은 플랫폼·정확한 build를 구분할 수 없으므로 이 일괄 차단 영향은 별도 승인해야 한다. 실제 설치 앱과 배포 트랙 상태는 이번에 가정하지 않았다.

## nginx와 서버 정책을 일치시키는 적용 계약

1. 예제 파일을 검토해 호스트의 **한 디렉터리**에 둔다. nginx와 두 Spring 색상에 같은 디렉터리를 읽기 전용으로 mount한다. 단일 파일 bind mount는 atomic rename 시 오래된 inode를 유지할 수 있어 금지한다.
2. 두 Spring 모두 `APP_CLIENT_POLICY_FILE=/etc/client-policy/policy.json`을 설정한다. nginx HTTPS server block에 `nginx-status.conf.example`을 검토해 include한다. 이 예제는 기존 proxy 설정 전체를 대체하지 않는다. 실제 환경에서 `nginx -t`가 필요하다.
3. 첫 적용은 반드시 차단 OFF다. 외부 nginx status와 blue/green의 내부 Spring status를 직접 비교해 **revision, mode, enforcementEnabled, allowLegacy, 플랫폼 규칙이 동일**한지 확인한다. 미설정 Spring의 `initial-off`와 nginx의 다른 정책이 섞이면 활성화하지 않는다. 구버전 서버 이미지가 섞여도 활성화하지 않는다.
4. 승인한 정책을 같은 디렉터리의 임시 파일에 쓰고 검증한 뒤 atomic rename으로 교체한다. 모든 독자에게 동시에 원자적 트랜잭션을 제공하는 것은 아니므로 요청 경계의 짧은 경쟁은 남는다. 엄격한 동결은 단계 전환 후 앱 중단으로 확보한다. 정책 파일 복사본을 따로 관리하지 않는다.
5. nginx는 `no-store`, etag/조건부 캐시/open_file_cache OFF로 정적 status를 제공한다. Spring도 요청마다 현재 파일을 읽는다. 명시한 파일이 없거나 손상되면 서버는 차단하고 새 앱은 조회 실패로 막는다. nginx status 파일 누락은 503이다.

**nginx가 살아 있으면 Spring/DB를 멈춰도 이 정적 경로가 남는다. 전체 EC2·nginx가 멈추거나 DNS/TLS가 실패하면 안내도 조회할 수 없다.** 이를 피하는 별도 정적 hosting은 선택적인 후속 작업이며 이번에 만들지 않았다. 새 hosting이 기본 필수 조건은 아니다.

예제 기반 격리 nginx의 구문·공유 readonly mount·정책 전환·upstream 중단 시 정적 응답은 로컬에서 검증했다. 운영 nginx 전체 구성의 `nginx -t`·mount·reload는 아직 확인/적용하지 않았다. 단순히 정책 JSON만 nginx에 넣고 서버 gate 연결을 생략하면 미지원 구버전의 요청이 계속 통과할 수 있다.

## 배포·활성화·rollback

빈 DB의 첫 기동은 Spring 한 인스턴스만 시작한다. 공개 health만으로 완료를 판정하지 말고, `DataVersionInitializer`의 초기 버전 행 전체(`DataTable`의 각 key와 `_revision`)가 commit됐고 프로세스가 계속 정상인지 확인한 뒤 두 번째 색상을 시작한다. 현 코드의 `existsById → save`는 동시 최초 기동에 안전하지 않다. 두 자동 DDL writer의 테이블/외래키 경합 경고와 `_revision` 중복 INSERT로 인한 프로세스 종료를 격리 MySQL에서 재현했다. 점검 OFF에서도 이 기존 초기화 특성은 남는다. 이번에는 초기화 알고리즘이나 스키마 마이그레이션 체계를 바꾸지 않고 순차 기동을 검증했다. `/actuator/health/readiness`가 익명 접근 가능하다고 가정하거나 이를 위해 보안 예외를 임의로 추가하지 않는다.

1. 로컬 검증·코드 검토·ApiDog 계약 반영. 서버/status/nginx 구성은 차단 OFF·legacy 허용 상태로 승인 후 배포한다.
2. 새 앱을 실제 이용자의 플랫폼·지역·기기·배포 트랙에 제공한다. 설치/업데이트 가능 여부, build 헤더, 시작 화면·스토어 복귀·오프라인·이어하기·결과 재전송을 실기기 QA한다.
3. 공지/유예 후 해당 플랫폼 minimum/recommended를 설정한다. allowLegacy=false는 구버전 UI 한계를 설명하고 별도 승인 후 적용한다. 무턱대고 최소값을 올리지 않는다.
4. 점검은 DRAINING → 신규 진입 차단 확인 → 기존 progress/end 결과와 진행 요청을 확인 → 승인한 시점에 MAINTENANCE → 모든 앱 writer 정상 종료 → DB 정상 종료 순서다. battle_session 행0을 기준으로 삼지 않는다.
5. 정상 재개는 MySQL/Redis healthy·PONG → 직전 활성 API/Web health → status/gate revision 일치 확인 → 승인한 NORMAL 전환이다. nginx는 정적 안내를 위해 유지한다. 정책 전환 자체와 서버 중단은 별도 실행 승인이 필요하다.
6. 정책 오설정은 동일 공유 파일의 이전 검증 revision 또는 NORMAL/차단OFF/legacy허용으로 rollback한다. DB·pending 결과·세션을 지우거나 구버전 앱에 새 popup이 생겼다고 가정하지 않는다. 이미 새 데이터가 쓰인 뒤 DB를 과거 백업으로 덮어쓰는 rollback은 이 절차에 포함하지 않는다.

## 로컬 검증 방법

```bash
# 서버 worktree. 테스트는 H2/모킹이며 운영 .env를 복사하지 않는다.
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home \
  ./gradlew spotlessApply checkstyleMain checkstyleTest test --tests 'com.novaserver.clientpolicy.*' \
  --tests 'com.novaserver.global.config.SecurityConfigTest' \
  --tests 'com.novaserver.battle.service.BattleServiceTest' --offline
```

Unity 테스트는 분리 worktree를 `-projectPath`로 지정한다. 원본 Editor를 닫거나 원본 Assets를 수정하지 않는다. 실행한 테스트 필터·XML 결과와 미실행 항목은 작업 결과 문서에 기록한다. 실제 앱 배포·서명·스토어·운영 nginx 검증은 여기의 단위 테스트 통과와 구분한다.
