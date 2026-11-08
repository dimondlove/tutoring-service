# Build stage: Gradle is already installed in this image, so the build does not
# need to download the Gradle distribution through services.gradle.org.
FROM gradle:9.8.2-jdk21-ubi9 AS build
WORKDIR /home/gradle/project
COPY --chown=gradle:gradle . .
RUN gradle --no-daemon bootJar -x test

# Runtime stage: keep the final image small and run the app as a non-root user.
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd -r appuser
COPY --from=build /home/gradle/project/build/libs/*.jar app.jar
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
