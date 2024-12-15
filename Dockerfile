FROM gradle:latest as builder

# 2
WORKDIR /app
COPY . .
RUN ./gradlew installDist

# 3
FROM openjdk:latest

# 4
WORKDIR /app
COPY --from=builder /app/build/install/serverlesskt ./
CMD ["./bin/serverlesskt"]