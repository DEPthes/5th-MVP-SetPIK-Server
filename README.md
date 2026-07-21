# 5th-MVP-SetPIK-Server

SetPIK 서버 공동개발 환경 레포입니다.

팀원이 Windows와 macOS를 함께 사용하더라도 같은 방식으로 개발할 수 있도록 아래 기준으로 환경을 통일했습니다.

- Java 17
- Spring Boot 3.5
- Gradle Wrapper
- Docker Compose
- PostgreSQL
- `.env` 기반 환경변수 관리
- `local`, `test` 프로필 분리

## 공통 원칙

- 전역 Gradle 설치는 하지 않습니다.
- 애플리케이션 실행은 항상 `gradlew` 또는 `gradlew.bat`를 사용합니다.
- 로컬 DB는 Docker Compose로 실행합니다.
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

Java 17이 아니라면 JDK 17을 먼저 설치합니다.

### 2. Docker Desktop 실행

Docker Desktop을 켠 뒤, 아래 명령어로 정상 동작 여부를 확인합니다.

```powershell
docker ps
```

### 3. 환경변수 파일 생성

프로젝트 루트에서 아래 명령어를 실행합니다.

```powershell
Copy-Item .env.example .env
```

필요하면 `.env` 값을 팀 규칙에 맞게 수정합니다.

### 4. DB 실행

```powershell
docker compose up -d
```

정상 실행 확인:

```powershell
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

터미널에서 아래 명령어를 실행합니다.

```bash
java -version
```

Java 17이 아니라면 JDK 17을 먼저 설치합니다.

### 2. Docker Desktop 실행

Docker Desktop을 켠 뒤, 아래 명령어로 정상 동작 여부를 확인합니다.

```bash
docker ps
```

### 3. 환경변수 파일 생성

프로젝트 루트에서 아래 명령어를 실행합니다.

```bash
cp .env.example .env
```

필요하면 `.env` 값을 팀 규칙에 맞게 수정합니다.

### 4. DB 실행

```bash
docker compose up -d
```

정상 실행 확인:

```bash
docker compose ps
```

### 5. 서버 실행

처음 한 번은 실행 권한이 필요할 수 있습니다.

```bash
chmod +x ./gradlew
```

그 다음 서버를 실행합니다.

```bash
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

DB 관련 기본 포트:

- PostgreSQL: `localhost:5432`
- Adminer: [http://localhost:8081](http://localhost:8081)

Adminer 접속 정보:

- System: `PostgreSQL`
- Server: `postgres`
- Username: `.env`의 `DB_USERNAME`
- Password: `.env`의 `DB_PASSWORD`
- Database: `.env`의 `DB_NAME`

## 환경변수 설명

`.env.example`에 들어있는 기본 항목은 아래와 같습니다.

- `DB_NAME`
- `DB_USERNAME`
- `DB_PASSWORD`
- `DB_PORT`
- `DB_HOST`
- `SERVER_PORT`
- `POSTGRES_VERSION`

## 테스트 정책

- 애플리케이션 로컬 실행은 PostgreSQL을 사용합니다.
- 테스트 실행은 H2 메모리 DB를 사용합니다.
- 따라서 `gradlew test`는 Docker 없이도 실행됩니다.

## 주요 파일

- `build.gradle`: 서버 의존성 및 빌드 설정
- `docker-compose.yml`: 로컬 DB/관리 도구 실행 설정
- `.env.example`: 팀 공용 환경변수 템플릿
- `Dockerfile`: 서버 컨테이너 이미지 빌드 설정
- `src/main/resources/application.yml`: 공통 설정
- `src/main/resources/application-local.yml`: 로컬 개발 설정
- `src/main/resources/application-test.yml`: 테스트 설정

## 추천 팀 규칙

- Java 버전은 전원 17로 통일
- DB 접속 정보는 `.env`로만 관리
- 실행 검증은 PR 전에 `gradlew test` 기준으로 통일
- 기능 개발 전 `docker compose up -d`로 로컬 인프라 먼저 실행
