FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY backend/pom.xml backend/pom.xml
COPY backend/src backend/src
RUN cd backend && mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/backend/target/online-voting-system-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8082
ENV PORT=8082
ENTRYPOINT ["java", "-jar", "app.jar"]
