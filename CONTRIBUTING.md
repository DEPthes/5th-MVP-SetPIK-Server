# SetPIK 서버 개발 규칙

이 문서는 세 명의 백엔드 개발자가 같은 구조와 규칙으로 병렬 개발하기 위한 최소 기준입니다.

## 패키지 구조

기능별 패키지 안에서 계층을 나눕니다.

```text
com.setpik.server
├─ common       공통 응답, 예외, 설정, 공통 도메인
├─ auth         SetPIK 인증과 Refresh Token
├─ member       회원
├─ spotify      Spotify 계정과 권한
├─ artist       아티스트와 장르
├─ playlist     플레이리스트와 트랙
├─ analysis     플레이리스트 분석
├─ performance  공연, 장소, 티켓, 매칭
├─ favorite     관심 공연과 조회 이력
├─ calendar     캘린더 일정
└─ prestudy     예습 플레이리스트
```

새 기능은 아래 계층을 필요한 만큼 추가합니다.

```text
playlist
├─ controller   HTTP 요청과 응답 처리
├─ service      비즈니스 로직과 트랜잭션
├─ repository   DB 접근
├─ domain       Entity와 Enum
└─ dto          Request와 Response DTO
```

## Entity와 Flyway

- DB 구조의 기준은 Flyway SQL입니다.
- DB 컬럼은 `snake_case`, Java 필드는 같은 의미의 `camelCase`를 사용합니다.
- 모든 DB 컬럼은 `@Column(name = "db_column")`으로 이름을 명시합니다.
- 공통 Entity는 여러 팀원이 동시에 수정하지 말고 PR 전에 공유합니다.
- 초기 Entity는 팀 간 결합을 줄이기 위해 FK를 `Long ...Id`로 매핑했습니다.
- 객체 탐색이 필요한 기능에서만 단방향 `@ManyToOne(fetch = LAZY)` 관계를 추가합니다.
- 양방향 연관관계와 무분별한 `CascadeType.ALL`은 사용하지 않습니다.
- Entity 전체에 public setter를 만들지 않습니다. 생성자와 의미가 드러나는 변경 메서드를 추가합니다.
- 이미 공유된 마이그레이션 파일은 수정하지 않고 새 버전 파일을 추가합니다.

예시:

```text
V2__add_performance_index.sql
V3__alter_playlist_analysis.sql
```

## API 응답과 예외

- Controller 응답은 `ApiResponse<T>`로 통일합니다.
- 성공 조회·수정·삭제는 코드 `1000`, 생성은 `1100`을 사용합니다.
- 예상 가능한 실패는 `BusinessException`과 `ErrorCode`를 사용합니다.
- Controller에서 직접 `try-catch`로 공통 오류 응답을 만들지 않습니다.
- 요청 DTO에는 Bean Validation을 적용합니다.
- Entity를 JSON으로 직접 반환하지 않고 Response DTO로 변환합니다.

## 인증과 공개 API

- 인증 API는 `/api/v1/auth/**` 아래에 작성합니다.
- 인증이 필요한 API는 `Authorization: Bearer {accessToken}`을 사용합니다.
- Refresh Token은 JSON이 아니라 Secure, HttpOnly, SameSite 쿠키로 전달합니다.
- 인증 필요 Controller에는 `@SecurityRequirement(name = SwaggerConfig.BEARER_AUTH)`를 추가합니다.
- JWT 필터가 추가되기 전까지 `SecurityConfig`의 공개 범위를 임의로 넓히지 않습니다.
- 테스트 때문에 인증을 우회해야 한다면 테스트 프로필 또는 Mock 인증을 사용합니다.

## Controller와 Service

- Controller는 입력 검증, Service 호출, 응답 변환만 담당합니다.
- 트랜잭션은 Service 계층에서 관리합니다.
- 조회 전용 로직에는 `@Transactional(readOnly = true)`를 사용합니다.
- URL, HTTP Method, DTO 필드명은 API 명세서를 기준으로 합니다.
- 날짜·시간 API 값은 ISO 8601 offset 형식을 사용합니다.
- 페이지 번호는 0부터 시작하고 기본 크기 20, 최대 크기 100을 사용합니다.

## 브랜치와 PR

- 기능 브랜치는 최신 `develop`에서 생성합니다.
- 브랜치 예시는 `feature/auth`, `feature/spotify-playlist`, `feature/performance`입니다.
- 하나의 PR에는 가능한 한 하나의 기능 또는 2~3개의 밀접한 API만 포함합니다.
- 공통 코드와 공용 Entity 변경은 PR 설명에 별도로 표시합니다.
- PR 전에 Windows는 `./gradlew.bat test`, macOS는 `./gradlew test`를 실행합니다.
- 다른 사람의 PR을 합치기 전에 최소 한 명이 리뷰합니다.

## 완료 기준

- API 명세서의 HTTP 상태와 JSON 형식을 만족합니다.
- 정상·실패 시나리오 테스트가 있습니다.
- Swagger UI에서 요청과 응답을 확인할 수 있습니다.
- Flyway 및 JPA 검증 오류 없이 애플리케이션이 실행됩니다.
- 비밀키, 토큰, `.env` 파일을 Git에 올리지 않습니다.
