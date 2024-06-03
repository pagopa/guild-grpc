# build stage
FROM maven:3.9.6-amazoncorretto-21 as builder

WORKDIR /app

COPY pom.xml .

RUN mvn dependency:go-offline

COPY src ./src

RUN mvn package -DskipTests=true

# execution stage
FROM amazoncorretto:21.0.3

WORKDIR /app

COPY --from=builder /app/target/booking*.jar booking-service.jar

CMD ["java", "-jar", "booking-service.jar"]
