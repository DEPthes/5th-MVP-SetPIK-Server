FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /app

COPY gradlew gradlew.bat settings.gradle build.gradle ./
COPY gradle gradle
COPY src src

RUN chmod +x gradlew && ./gradlew bootJar

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

RUN apt-get update \
	&& apt-get install -y --no-install-recommends curl \
	&& rm -rf /var/lib/apt/lists/* \
	&& useradd --system --uid 10001 --create-home setpik

COPY --from=builder --chown=setpik:setpik /app/build/libs/*.jar app.jar

USER setpik

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
