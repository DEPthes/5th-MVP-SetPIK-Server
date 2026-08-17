# 5th-MVP-SetPIK-Server

팀 개발 규칙과 패키지 구조는 [CONTRIBUTING.md](CONTRIBUTING.md)를 먼저 확인해 주세요.

SetPIK 서버 공동개발 환경 레포입니다.

팀원이 Windows와 macOS를 함께 사용하더라도 같은 방식으로 개발할 수 있도록 아래 기준으로 환경을 통일했습니다.

- Java 17
- Spring Boot 3.5
- Gradle Wrapper
- Docker Compose
- MySQL 8
- Flyway
- `.env` 기반 환경변수 관리
- `local`, `test` 프로필 분리

## 공통 원칙

- 전역 Gradle 설치는 하지 않습니다.
- 애플리케이션 실행은 항상 `gradlew` 또는 `gradlew.bat`를 사용합니다.
- 로컬 DB는 Docker Compose로 실행합니다.
- 스키마 변경은 Flyway 마이그레이션으로 관리합니다.
- 민감한 값은 `.env`에 두고, 공통 형식은 `.env.example`로 공유합니다.

## 사전 준비

모든 팀원이 아래 3가지를 먼저 설치해야 합니다.

- Git
- JDK 17
- Docker Desktop

권장 확인 명령어:

```bash
git --version
java -version
docker --version
docker compose version
```

## Windows 개발환경 설정

### 1. JDK 17 확인

PowerShell에서 아래 명령어를 실행합니다.

```powershell
java -version
```

### 2. Docker Desktop 실행

```powershell
docker ps
```

### 3. 환경변수 파일 생성

```powershell
Copy-Item .env.example .env
```

### 4. DB 실행

```powershell
docker compose up -d
docker compose ps
```

### 5. 서버 실행

```powershell
.\gradlew.bat bootRun
```

### 6. 테스트 실행

```powershell
.\gradlew.bat test
```

## macOS 개발환경 설정

### 1. JDK 17 확인

```bash
java -version
```

### 2. Docker Desktop 실행

```bash
docker ps
```

### 3. 환경변수 파일 생성

```bash
cp .env.example .env
```

### 4. DB 실행

```bash
docker compose up -d
docker compose ps
```

### 5. 서버 실행

처음 한 번은 실행 권한이 필요할 수 있습니다.

```bash
chmod +x ./gradlew
./gradlew bootRun
```

### 6. 테스트 실행

```bash
./gradlew test
```

## 공통 실행 확인

서버가 정상 실행되면 아래 주소로 확인합니다.

- [http://localhost:8080/api/v1/health](http://localhost:8080/api/v1/health)
- [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)
- [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)

DB 관련 기본 포트:

- MySQL: `localhost:3306`
- phpMyAdmin: [http://localhost:8081](http://localhost:8081)

phpMyAdmin 접속 정보:

- Server: `mysql`
- Username: `.env`의 `DB_USERNAME`
- Password: `.env`의 `DB_PASSWORD`

## 환경변수 설명

`.env.example`에 들어있는 기본 항목은 아래와 같습니다.

- `DB_NAME`
- `DB_USERNAME`
- `DB_PASSWORD`
- `DB_ROOT_PASSWORD`
- `DB_PORT`
- `DB_HOST`
- `SERVER_PORT`
- `MYSQL_VERSION`
- `SPOTIFY_CLIENT_ID`: Spotify 개발자 앱 Client ID
- `SPOTIFY_CLIENT_SECRET`: Spotify 개발자 앱 Client Secret
- `SPOTIFY_REDIRECT_URI`: Spotify Dashboard에 등록한 백엔드 콜백 URI. 로컬에서는 `localhost` 대신 `127.0.0.1`을 사용
- `SPOTIFY_COOKIE_SECURE`: HTTPS 환경에서는 `true`, 로컬 HTTP 환경에서는 `false`
- `TOKEN_ENCRYPTION_KEY`: Spotify 토큰 암호화용 Base64 32바이트 키
- `REFRESH_TOKEN_EXPIRATION`: SetPIK Refresh Token 유효기간(기본값 `14d`)
- `OAUTH_SUCCESS_REDIRECT_URI`: Spotify 로그인 성공 후 이동할 프론트엔드 주소
- `OAUTH_FAILURE_REDIRECT_URI`: Spotify 로그인 실패 후 이동할 프론트엔드 주소
- `AUTH_COOKIE_SECURE`: HTTPS 환경에서는 `true`, 로컬 HTTP 환경에서는 `false`
- `JWT_SECRET`: Access Token 서명에 사용하는 Base64 32바이트 이상 비밀키
- `ACCESS_TOKEN_EXPIRATION`: Access Token 유효기간(기본값 `30m`)
- `KOPIS_API_KEY`: KOPIS Open API 인증키
- `KOPIS_DETAIL_CONCURRENCY`: 공연·공연장 상세 조회 최대 동시 요청 수(기본값 `5`)
- `KOPIS_BATCH_SIZE`: 한 트랜잭션에서 저장할 공연 수(기본값 `50`)
- `KOPIS_RETRY_MAX_ATTEMPTS`: 일시적인 KOPIS 오류의 최대 시도 횟수(기본값 `3`)
- `KOPIS_RETRY_DELAY`: KOPIS 재시도 기본 대기시간(기본값 `500ms`)
- `KOPIS_CONNECT_TIMEOUT`: KOPIS 연결 제한시간(기본값 `3s`)
- `KOPIS_READ_TIMEOUT`: KOPIS 응답 제한시간(기본값 `10s`)
- `KOPIS_SYNC_ENABLED`: 운영 자동 동기화 활성화 여부(기본값 `false`)
- `KOPIS_SYNC_CRON`: 자동 동기화 cron. 기본값은 매일 오전 3시
- `KOPIS_SYNC_FUTURE_DAYS`: 자동 동기화 시 오늘부터 조회할 기간(기본값 `1일`)
- `JPA_BATCH_SIZE`: JPA가 INSERT·UPDATE를 묶어 전송할 크기(기본값 `50`)

## Flyway

- MySQL 마이그레이션 파일 위치:
  - `src/main/resources/db/migration/mysql`
- 현재 초기 스키마 파일:
  - `V1__create_setpik_schema.sql`

새 스키마 변경은 반드시 새 버전 파일로 추가합니다.

예시:

- `V2__add_user_profile_table.sql`
- `V3__add_playlist_indexes.sql`

## 테스트 정책

- 애플리케이션 로컬 실행은 MySQL을 사용합니다.
- 테스트 실행은 H2 메모리 DB를 사용합니다.
- 테스트 프로필에서는 Flyway를 비활성화했습니다.

## CI

GitHub Actions는 `main` 대상 Pull Request와 `main` 브랜치 Push에서 자동으로 실행됩니다.

- Java 17 및 Gradle Wrapper 사용
- 전체 테스트 실행
- Spring Boot 실행 JAR 생성 검증
- 실패 시 GitHub Actions의 Artifacts에서 테스트 리포트 확인 가능

PR을 병합하기 전에 GitHub의 `Test and build` 검사가 통과했는지 확인합니다.

## EC2 운영 배포

운영 배포는 로컬 개발용 `docker-compose.yml`과 분리된
`docker-compose.prod.yml`을 사용합니다. 운영 구성에서는 MySQL과
phpMyAdmin 포트를 외부에 노출하지 않고 Spring Boot의 `8080` 포트만
공개합니다.

### 최초 수동 배포

EC2에서 저장소를 받은 뒤 운영 환경변수 파일을 생성합니다.

```bash
cd /opt/setpik
git clone https://github.com/DEPthes/5th-MVP-SetPIK-Server.git app
cd app
cp .env.production.example .env
chmod 600 .env
nano .env
```

`.env`의 `CHANGE_ME` 값은 모두 실제 운영 값으로 변경합니다. JWT와 토큰
암호화 키는 서로 다른 값으로 생성합니다.

```bash
openssl rand -base64 32
openssl rand -base64 32
```

HTTPS 연결 전에는 Health API 검증만 수행합니다. Spotify OAuth 운영
Redirect URI는 API Gateway 또는 도메인으로 만든 HTTPS 백엔드 주소가
준비된 후 입력하고 Spotify Dashboard에도 동일하게 등록합니다.

구성을 검증하고 컨테이너를 실행합니다.

```bash
docker compose --env-file .env -f docker-compose.prod.yml config --quiet
docker compose --env-file .env -f docker-compose.prod.yml up -d --build
docker compose --env-file .env -f docker-compose.prod.yml ps
```

애플리케이션 상태와 로그를 확인합니다.

```bash
curl --fail http://127.0.0.1:8080/api/v1/health
docker compose --env-file .env -f docker-compose.prod.yml logs --tail=100 app
```

외부에서는 EC2 보안 그룹에 등록된 관리자 IP에서만 아래 주소로 초기
상태를 확인합니다.

```text
http://EC2_ELASTIC_IP:8080/api/v1/health
http://EC2_ELASTIC_IP:8080/swagger-ui/index.html
```

컨테이너를 중지할 때는 MySQL 볼륨을 보존하기 위해 `-v` 옵션을 사용하지
않습니다.

```bash
docker compose --env-file .env -f docker-compose.prod.yml down
```

## 주요 파일

- `build.gradle`: 서버 의존성 및 빌드 설정
- `docker-compose.yml`: 로컬 MySQL / phpMyAdmin 실행 설정
- `.env.example`: 팀 공용 환경변수 템플릿
- `src/main/resources/application.yml`: 공통 설정
- `src/main/resources/application-local.yml`: 로컬 개발 설정
- `src/main/resources/application-test.yml`: 테스트 설정
- `src/main/resources/db/migration/mysql/V1__create_setpik_schema.sql`: 초기 스키마

## 추천 팀 규칙

- Java 버전은 전원 17로 통일
- DB 접속 정보는 `.env`로만 관리
- 실행 검증은 PR 전에 `gradlew test` 기준으로 통일
- 기능 개발 전 `docker compose up -d`로 로컬 인프라 먼저 실행
- 스키마 변경은 Flyway 파일 추가로만 반영
