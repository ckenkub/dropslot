FROM eclipse-temurin:21-jre-alpine
ARG JAR_FILE=target/discovery-service-0.1.0-SNAPSHOT.jar
COPY ${JAR_FILE} /app/app.jar
ENTRYPOINT ["java","-jar","/app/app.jar"]
