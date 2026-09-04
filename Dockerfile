# ---- build stage (Gradle preinstalled: no wrapper download) ----
FROM gradle:8.14-jdk17 AS build
WORKDIR /src
COPY build.gradle.kts settings.gradle.kts ./
RUN gradle --no-daemon dependencies > /dev/null 2>&1 || true
COPY src ./src
RUN gradle --no-daemon clean bootJar -x test

# ---- runtime stage ----
FROM eclipse-temurin:17-jre
WORKDIR /app
# FileUtils writes uploads to <user.dir>/src/main/resources/static/files - keep that layout.
RUN mkdir -p /app/src/main/resources/static/files /app/logs
COPY --from=build /src/build/libs/app.jar /app/app.jar
ENV JAVA_OPTS="-Xms256m -Xmx1024m" \
    LOGGING_FILE_NAME=/app/logs/app.log
EXPOSE 8088
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -Dfile.encoding=UTF-8 -jar /app/app.jar"]
