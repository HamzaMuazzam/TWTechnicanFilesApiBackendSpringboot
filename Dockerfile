# ---- build stage (Gradle preinstalled: no wrapper download) ----
# Gradle 8.14 / JDK 17 matches gradle/wrapper (8.14.4) and Spring Boot 3.5.x; the admin dashboard
# (frontend/, optional) is built by the node-gradle plugin during processResources.
FROM gradle:8.14-jdk17 AS build
WORKDIR /src
COPY build.gradle.kts settings.gradle.kts gradle.properties ./
RUN gradle --no-daemon dependencies > /dev/null 2>&1 || true
COPY . .
RUN gradle --no-daemon clean bootJar -x test

# ---- runtime stage ----
FROM eclipse-temurin:17-jre
WORKDIR /app
# FileUtils writes uploads to <user.dir>/src/main/resources/static/files - keep that layout.
RUN mkdir -p /app/src/main/resources/static/files /app/logs /app/config
COPY --from=build /src/build/libs/app.jar /app/app.jar
ENV JAVA_OPTS="-Xms256m -Xmx1024m" \
    LOGGING_FILE_NAME=/app/logs/app.log
EXPOSE 8089
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -Dfile.encoding=UTF-8 -jar /app/app.jar"]
