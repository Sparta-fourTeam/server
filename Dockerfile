# Java 버전은 프로젝트에 맞추세요 (17 또는 21)
FROM eclipse-temurin:21-jre
WORKDIR /app
# Actions에서 빌드한 jar를 app.jar로 복사해 둔 것을 사용
COPY app.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
