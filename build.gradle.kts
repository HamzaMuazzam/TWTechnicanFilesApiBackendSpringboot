import com.github.gradle.node.npm.task.NpmTask

plugins {
    java
    id("org.springframework.boot") version "3.5.16"
    id("io.spring.dependency-management") version "1.1.7"
    id("com.github.node-gradle.node") version "7.1.0"
}

group = "com.trackingworld"
version = "1.0.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

val jjwtVersion = "0.12.7"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-aop")

    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")

    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.17")

    implementation("io.jsonwebtoken:jjwt-api:$jjwtVersion")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:$jjwtVersion")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:$jjwtVersion")

    implementation("com.google.firebase:firebase-admin:9.11.0") {
        // Only Auth + Messaging are used; Firestore/Storage add ~60 MB to the jar.
        exclude(group = "com.google.cloud", module = "google-cloud-firestore")
        exclude(group = "com.google.cloud", module = "google-cloud-storage")
    }
    // FirebaseOptions still resolves JacksonFactory reflectively; not pulled transitively, so declare it.
    implementation("com.google.http-client:google-http-client-jackson2:2.2.0")
    implementation("com.googlecode.libphonenumber:libphonenumber:9.0.40")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// Fixed jar name so the Windows service / Dockerfile never need updating on version bumps.
tasks.bootJar {
    archiveFileName.set("app.jar")
}

// ---------------------------------------------------------------------------
// Admin dashboard (frontend/) -> bundled into the jar at classpath:/static/admin
// The frontend folder is optional: the backend builds fine without it, and
// `-PskipFrontend` skips it explicitly.
// ---------------------------------------------------------------------------
val frontendDir = layout.projectDirectory.dir("frontend")
val frontendPresent = frontendDir.file("package.json").asFile.exists() && !project.hasProperty("skipFrontend")

node {
    download.set(true)
    version.set("22.23.3")
    nodeProjectDir.set(frontendDir)
    workDir.set(layout.buildDirectory.dir("nodejs"))
    npmWorkDir.set(layout.buildDirectory.dir("npm"))
}

tasks.named("nodeSetup") { onlyIf { frontendPresent } }
tasks.named("npmSetup") { onlyIf { frontendPresent } }

val npmInstallFrontend by tasks.registering(NpmTask::class) {
    description = "Installs admin dashboard dependencies (npm ci in frontend/)."
    group = "frontend"
    onlyIf { file("frontend/package.json").exists() && !project.hasProperty("skipFrontend") }
    workingDir.set(frontendDir.asFile)
    args.set(listOf("ci"))
    inputs.file(frontendDir.file("package.json")).optional()
    inputs.file(frontendDir.file("package-lock.json")).optional()
    outputs.dir(frontendDir.dir("node_modules"))
}

val buildFrontend by tasks.registering(NpmTask::class) {
    description = "Builds the admin dashboard (npm run build in frontend/)."
    group = "frontend"
    dependsOn(npmInstallFrontend)
    onlyIf { file("frontend/package.json").exists() && !project.hasProperty("skipFrontend") }
    workingDir.set(frontendDir.asFile)
    args.set(listOf("run", "build"))
    inputs.files(fileTree(frontendDir) { exclude("node_modules/**", "dist/**") })
    outputs.dir(frontendDir.dir("dist"))
}

tasks.processResources {
    dependsOn(buildFrontend)
    from(frontendDir.dir("dist")) {
        into("static/admin")
    }
}
