# 서버 문서

코드가 원본이다. 이 폴더에는 코드보다 문서로 보는 게 편한 것만 둔다. 처음이라면 [architecture.md](architecture.md)부터 읽는다.

| 문서 | 내용 |
| --- | --- |
| [architecture.md](architecture.md) | 계층 구조, 클라이언트와 나누는 기준 |
| [conventions.md](conventions.md) | API·예외·JPA·코드 스타일·테스트 규칙 |

- 공통 작업 규칙(브랜치, 커밋, PR): 조직 `.github` 레포의 `CONTRIBUTING.md`
- 스테이지 흐름과 실패·복구 처리: 클라이언트 레포 `docs/flows.md`
- API 명세: ApiDog

## 스택

Spring Boot 4.1.1, Java 21, Gradle / Spring Web MVC, Spring Data JPA, Spring Data Redis, Validation, Spring Security / MySQL, JWT(jjwt), Lombok, springdoc-openapi

## 자주 쓰는 명령어

| 목적 | 명령어 |
| --- | --- |
| 실행 | `./gradlew bootRun` |
| 커밋 전 포맷 | `./gradlew spotlessApply` |
| PR 전 전체 검증 (테스트 + 포맷 + 린트) | `./gradlew check` |
| 전체 테스트 | `./gradlew test` |
| 특정 테스트 | `./gradlew test --tests "패키지.클래스명"` |
| 린트만 | `./gradlew checkstyleMain checkstyleTest` |
| 빌드 | `./gradlew build` |

규칙을 바꾸고 싶으면 GitHub 이슈나 회의에서 정하고, 결정과 이유는 노션 회의록에 남긴 뒤 해당 문서를 고친다.
