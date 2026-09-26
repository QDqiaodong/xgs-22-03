FROM ${DOCKER_REGISTRY}maven:3.9-eclipse-temurin-17 AS builder

RUN mkdir -p /root/.m2
COPY settings.xml /root/.m2/settings.xml

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -q

COPY src ./src
RUN mvn package -DskipTests -q

FROM ${DOCKER_REGISTRY}openjdk:17-jdk-slim

WORKDIR /app

COPY --from=builder /app/target/light-manager-1.0.0.jar app.jar

RUN mkdir -p /var/data/transfer

EXPOSE 8127

CMD ["java", "-jar", "app.jar"]