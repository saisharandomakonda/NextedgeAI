FROM maven:3.9.6-eclipse-temurin-11 AS build

WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

FROM eclipse-temurin:11-jdk

WORKDIR /app
COPY --from=build /app/arcade/target/*.jar app.jar

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]