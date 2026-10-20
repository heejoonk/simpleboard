# CLAUDE.md — SimpleBoard (JdbcTemplate + MySQL, Spring Security 미사용)

## 프로젝트 개요
로그인 기반의 간단한 게시판. Thymeleaf 서버 렌더링 방식의 Spring Boot 애플리케이션.
- 데이터 접근은 **JdbcTemplate만** 사용하고, DB는 **로컬 MySQL**을 사용한다.
- 로그인/권한은 **Spring Security 없이** `HttpSession` + `HandlerInterceptor` 로 직접 구현한다.

## 기준 문서 (작업 전 항상 먼저 읽기)
- `docs/01-prd.txt` — 범위. **"범위 밖" 항목은 요청 없이 구현하지 않는다.**
- `docs/02-features.txt` — 기능 ID(F-xx), 검증 규칙, 완료 조건, 구현 순서
- `docs/03-screens.txt` — 화면 ID(S-xx), 화면 동작/문구
- `docs/wireframes/` — 화면 배치 (excalidraw PNG). 배치는 그림, 동작은 03-screens.txt를 따른다.
- `docs/04-erd.txt` — 테이블 설명, 주요 SQL 패턴
- `docs/05-api.txt` — URL(A-xx), 로그인 체크 대상, DTO 이름과 필드
- `docs/06-schema.sql` — DDL. **테이블 구조의 기준**

문서에 없거나 문서끼리 모순되는 부분은 추측하지 말고 먼저 질문한다.
구현 중 설계가 바뀌면 관련 문서도 함께 수정한다.

## 기술 스택
- Java 21, Spring Boot 4.x, Gradle (Groovy DSL)
- Spring Web, **Spring JDBC (JdbcTemplate / NamedParameterJdbcTemplate)**, Validation
- Thymeleaf
- MySQL 8 + mysql-connector-j
- Lombok
- 비밀번호 해시: `org.springframework.security:spring-security-crypto` (BCryptPasswordEncoder **클래스만** 사용)
- 테스트: JUnit 5, Spring Boot Test, MockMvc, AssertJ
- CSS: Bootstrap 5 (CDN)

**사용 금지**
- `spring-boot-starter-security`, `spring-security-test`, `thymeleaf-extras-springsecurity6`
  (starter-security를 넣으면 기본 로그인 페이지와 필터가 자동으로 켜진다. crypto 모듈만 쓴다)
- Spring Data JPA, Hibernate, MyBatis, QueryDSL, H2

새 라이브러리를 추가하기 전에는 이유를 설명하고 승인을 받는다.

## DB 설정
- **`application.yaml` 은 git으로 관리하지 않고 별도로 배포한다.** (`.gitignore` 등록됨, DB 계정 정보 포함)
  - 이 파일을 git에 추가하거나 `.gitignore` 에서 빼지 않는다.
  - 견본은 `application.yaml.example` 로 git에 커밋한다. 비밀번호 자리는 `여기에_비밀번호` 로 비워 둔다.
  - `application.yaml` 을 수정할 때는 **같은 변경을 `application.yaml.example` 에도 반영**한다 (비밀번호 제외).
  - `application.yaml` 을 수정했으면 작업 보고 마지막에 "application.yaml 변경 사항"으로 바뀐 줄을 따로 알려준다.
    (git 기록에 남지 않으므로 사용자가 별도 배포본에 반영해야 한다)
  - 비밀번호는 Java 코드, 문서, 테스트 코드, 커밋 메시지에 쓰지 않는다.
  ```yaml
  spring:
    datasource:
      url: jdbc:mysql://localhost:3306/simpleboard?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
      username: board
      password: 여기에_비밀번호
    sql:
      init:
        mode: always          # 시작 시 schema.sql 실행 (CREATE TABLE IF NOT EXISTS)
  server:
    servlet:
      session:
        timeout: 30m
        tracking-modes: cookie   # URL에 ;jsessionid 붙지 않게
        cookie:
          http-only: true
          same-site: lax         # CSRF 완화 (Spring Security CSRF 토큰 대신)
  ```
- 스키마: `src/main/resources/schema.sql` 은 `docs/06-schema.sql` 의 테이블 부분과 항상 같게 유지한다.
  테이블 변경 시 **둘 다** 수정하고, 기존 로컬 DB에 적용할 ALTER 문을 따로 알려준다.
- 테스트는 `simpleboard_test` DB를 사용한다 (`src/test/resources/application.yaml`, 이 파일도 git 제외 + `.example` 견본 관리).
  실제 데이터가 있는 `simpleboard` DB로 테스트를 돌리지 않는다.

## 패키지 구조 (도메인형)
```
com.himedia.board
 ├─ member/    controller(MemberController, LoginController), service, repository, domain, dto
 ├─ post/      controller, service, repository, domain, dto
 ├─ comment/   controller, service, repository, domain, dto
 └─ global/
     ├─ config/       WebConfig(인터셉터 등록), PasswordConfig(BCryptPasswordEncoder 빈)
     ├─ session/      SessionConst, LoginMember
     ├─ interceptor/  LoginCheckInterceptor
     ├─ web/          LoginMemberModelAdvice
     ├─ exception/    GlobalExceptionHandler, NotFoundException, ForbiddenException
     └─ common/       PageResult
```
템플릿: `src/main/resources/templates/{layout, member, post, error}/`

## 로그인 / 세션 규칙 (Spring Security 대체)
- 세션 키는 상수로만 사용한다: `SessionConst.LOGIN_MEMBER = "loginMember"`
- 세션에는 `LoginMember { Long id; String email; String nickname; }` 만 저장한다 (`Serializable`).
  **비밀번호, Member 도메인 객체 전체를 세션에 넣지 않는다.**
- 로그인 성공 시 `request.changeSessionId()` 로 세션 ID를 재발급한다 (세션 고정 공격 방지).
- 로그아웃은 POST `/logout` 에서 `session.invalidate()`.
- 세션 조회 시 `request.getSession(false)` 를 사용해 불필요한 세션을 만들지 않는다.
- **로그인 체크는 `LoginCheckInterceptor` 한 곳에서만** 한다 (Controller마다 if문으로 체크하지 않는다).
  - 대상/제외 경로는 `docs/05-api.txt` 의 "로그인 체크" 표를 따른다.
  - 비로그인 시 GET 요청은 `/login?redirectURL={원래 URI+쿼리}`, 그 외 요청은 `/login` 으로 redirect.
- `redirectURL` 은 `/` 로 시작하고 `//` 로 시작하지 않는 내부 경로만 허용한다. 아니면 `/` 로 보낸다 (오픈 리다이렉트 방지).
- Controller에서 로그인 사용자는 `@SessionAttribute(name = SessionConst.LOGIN_MEMBER) LoginMember loginMember` 로 받는다.
  비로그인도 접근 가능한 화면(목록, 상세)에서는 `required = false`.
- 헤더의 로그인 표시는 `LoginMemberModelAdvice`(@ControllerAdvice + @ModelAttribute("loginMember"))가 모든 화면 Model에 넣어 준다.
- 작성자 권한(본인 글/댓글)은 Service에서 `loginMember.id()` 와 작성자 id를 비교하고, 다르면 `ForbiddenException`.
- CSRF 토큰은 없다. 대신 **상태를 바꾸는 요청(등록/수정/삭제/로그아웃)은 반드시 POST로만** 받고, GET으로는 아무것도 변경하지 않는다.

## JdbcTemplate 규칙
- Repository는 `@Repository` 클래스로 직접 구현한다 (인터페이스 + 구현체 분리 불필요).
- 파라미터가 2개 이상이면 `NamedParameterJdbcTemplate` + `:name` 바인딩을 우선 사용한다.
- **SQL에 값을 문자열 연결(+, String.format)로 넣지 않는다.** 항상 바인딩한다.
- SQL은 Repository 안에 텍스트 블록(`"""`)으로 작성한다.
- RowMapper는 람다로 직접 매핑한다 (`BeanPropertyRowMapper` 사용 안 함). 컬럼 snake_case → 필드 camelCase.
- 단건 조회는 `Optional<T>`를 반환한다. `queryForObject` 대신 `query(...).stream().findFirst()` 사용.
- INSERT 후 생성된 id는 `GeneratedKeyHolder`로 받는다.
- UPDATE/DELETE는 영향받은 행 수를 반환하고, 필요하면 Service에서 0건 처리를 한다.
- 화면용 JOIN 결과는 도메인 객체가 아닌 조회 DTO로 바로 매핑한다 (예: `PostListResponse`).
- N+1 금지: 목록 화면에서 반복문 안에서 쿼리를 실행하지 않는다.

## 코딩 규칙
- Controller는 요청/응답 변환과 화면 선택만, 비즈니스 로직과 권한 검사는 Service에서 한다.
- 도메인 객체를 View로 직접 넘기지 않는다. 요청은 `*Form`/`*Request`, 응답은 `*Response` DTO.
- Service 클래스에 `@Transactional(readOnly = true)`, 변경 메서드에만 `@Transactional`.
- 작성자 권한 확인 실패 시 `ForbiddenException`, 대상 없음은 `NotFoundException`.
- 의존성 주입은 생성자 주입(`@RequiredArgsConstructor`)만 사용.
- 도메인 객체에 `@Setter` 대신 `@Builder` 또는 생성자를 사용한다. 세션/응답 DTO는 record 사용 가능.
- 비밀번호는 `BCryptPasswordEncoder`로 암호화하고 `matches()` 로 비교한다.
- Thymeleaf에서 `th:utext` 사용 금지 (XSS 방지).
- 화면에 노출되는 메시지는 한국어로 작성한다.

## 명령어
- 실행 전 확인: `src/main/resources/application.yaml` 이 있어야 한다 (없으면 `.example` 을 복사해 비밀번호 입력)
- 빌드: `./gradlew build`
- 테스트: `./gradlew test`
- 실행: `./gradlew bootRun`
- 접속: http://localhost:8080

## 테스트 규칙
- Repository: `@JdbcTest` + `@AutoConfigureTestDatabase(replace = NONE)` 로 simpleboard_test DB에서 SQL 검증
- Service: 정상/예외(404, 403, 중복, 로그인 실패) 케이스
- Controller: MockMvc 사용. 로그인 상태는 `MockHttpSession` 에 `SessionConst.LOGIN_MEMBER` 를 넣어 만든다.
  권한 시나리오 3가지(비로그인 → /login redirect / 타인 → 403 / 본인 → 성공)를 테스트한다.
- Interceptor: redirectURL 생성과 외부 URL 차단을 테스트한다.
- 테스트는 `@Transactional` 로 롤백되게 하여 데이터를 남기지 않는다.
- 테스트 메서드명에 기능 ID 포함. 예: `F06_작성자가_아니면_수정할_수_없다()`

## 작업 방식
1. 한 번에 하나의 기능(F-xx)만 작업한다. 순서는 02-features.txt의 권장 구현 순서를 따른다.
2. 코드 작성 전에 만들/수정할 파일 목록, 작성할 SQL, 계획을 먼저 제시한다.
3. 화면 작업 시 와이어프레임에서 읽은 구성 요소를 먼저 목록으로 보여준다.
4. 구현 후 테스트를 작성하고 `./gradlew test` 통과를 확인한다.
5. 완료 조건 체크리스트(02-features.txt) 기준으로 누락 여부를 보고한다.
6. 커밋 메시지 형식: `feat(F-01): 회원가입 구현`, `fix(F-06): 수정 권한 검사 누락 수정`
