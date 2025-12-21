# ---- Build stage ----
FROM gradle:8.7-jdk17 AS build
WORKDIR /app

COPY build.gradle* settings.gradle* /app/
COPY src /app/src

RUN gradle clean bootJar --no-daemon

# ---- Run stage ----
FROM eclipse-temurin:17-jre
WORKDIR /app

RUN addgroup --system javauser && \
    adduser --system --shell /bin/false --ingroup javauser javauser

COPY --from=build /app/build/libs/*.jar app.jar
RUN chown javauser:javauser app.jar

USER javauser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
