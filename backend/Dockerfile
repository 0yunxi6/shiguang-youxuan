FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml ./
COPY lombok.config ./
COPY src ./src
RUN mvn -q -Dmaven.test.skip=true clean package

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /build/target/ecommerce-backend-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar", "--spring.profiles.active=prod"]
CMD []
