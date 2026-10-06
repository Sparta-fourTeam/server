# 서버 규칙

공통 작업 규칙(브랜치, 커밋, PR)은 조직 `.github` 레포의 `CONTRIBUTING.md`를 본다.

## API 명세

- 명세의 기준은 **ApiDog**다. springdoc(Swagger UI)은 참고용이다
- API를 추가·변경하면 ApiDog를 먼저 고치고, 클라이언트 담당에게 알린다

## 에러 응답

모든 에러는 같은 모양으로 내려간다. `@Valid` 검증 실패도 마찬가지다.

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "사용자를 찾을 수 없습니다",
  "path": "/api/users/1"
}
```

`error`는 `status`의 reason phrase로 자동으로 채워진다.

## 예외 처리

- 에러는 `global/error/ErrorCode`에 상태 코드와 메시지를 추가하고, `ErrorCode.XXX.exception(...)`으로 던진다
- 응답 형식은 `CustomErrorAttributes`가 공통 형식으로 맞춘다
- 예외는 원인이 되는 계층(주로 Service)에서 던진다. Repository와 엔티티에서는 던지지 않는다
- 500 에러는 `ResponseStatusException`으로 직접 던진 경우만 메시지가 클라에 나가고, 그 외에는 "서버 오류가 발생했습니다"로 가려진다
- 로그: 4xx는 `warn`, 5xx는 `error`

## JPA

- 연관관계는 **단방향**이 기본이다. 반대편에서 자주 탐색할 명확한 이유가 있을 때만 양방향을 추가하고, 주인과 `mappedBy`를 정확히 지정한다
- 엔티티를 응답으로 직접 반환하지 않는다. 양방향을 그대로 직렬화하면 순환 참조로 무한 루프가 난다
- 연관 엔티티는 필요할 때만 조회한다. N+1은 `fetch join`이나 `@EntityGraph`로 막는다

## 코드 스타일

포맷은 도구가 맞춘다. 직접 고치지 말고 커밋 전에 `./gradlew spotlessApply`를 실행한다. 포맷이 안 맞으면 컴파일부터 실패한다.

사람이 챙겨야 하는 것:

- 모든 public 클래스에 **한글 Javadoc**
- getter·setter·생성자는 Lombok으로 줄인다

## 커밋이 거부되는 경우

**커밋 자체가 막힘** (공통 훅)
- 커밋 메시지가 Conventional Commits 형식이 아님
- 5MB 넘는 파일 추가
- 병합 충돌 마커가 남아 있음
- API 키·토큰·비밀번호로 의심되는 문자열 (gitleaks)

**자동으로 고쳐지니 `git add` 후 다시 커밋** (Spotless)
- 줄 끝 공백, 파일 끝 개행, import 정렬·미사용 import, 들여쓰기·중괄호 위치

**직접 고쳐야 함** (Checkstyle, 경고 1개만 있어도 실패)
- Javadoc 누락: public 타입, protected 이상이면서 2줄 이상인 메서드. `@Override`, `@Test`, `@*Mapping`, `findBy*`·`save`·`delete*` 같은 리포지토리 메서드는 예외
- 한 줄 100자 초과 (package·import·URL 줄은 예외)
- 네이밍: 클래스는 PascalCase, 메서드·변수·파라미터는 camelCase
- star import (`import java.util.*;`)
- static import와 일반 import를 섞거나 알파벳순이 아님
- 한 줄 if·for·while의 중괄호 생략
- switch의 `default` 누락, `break` 없이 다음 case로 넘어감
- 주석 없는 빈 catch 블록
- `TODO:` 형식이 아닌 TODO 주석

## 테스트

- 클래스: `{대상 클래스}Test` (예: `UserServiceTest`)
- 메서드: 상황과 기대 결과가 드러나게. 한글 `@DisplayName` 또는 `shouldThrowException_whenUserNotFound()` 형식
- Given / When / Then으로 구간을 나눈다
- 단위 테스트: DB·Redis에 연결하지 않고 Mockito로 모킹한다
- 통합 테스트: `application-test.yaml` 프로파일과 H2 인메모리 DB를 쓴다
