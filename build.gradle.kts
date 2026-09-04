plugins {
    java
    id("org.springframework.boot") version "3.3.2"
    id("io.spring.dependency-management") version "1.1.3"
    kotlin("jvm") version "1.9.0"
    id("java")
}

group = "com.logicaldevs"
version = "0.0.1-SNAPSHOT"

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

dependencies {
//    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.projectlombok:lombok")
    implementation("org.postgresql:postgresql")
    annotationProcessor("org.projectlombok:lombok")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.2.0")
    implementation("org.springframework.boot:spring-boot-starter-mail")

    // HTTP Client for SOAP API calls
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Excel generation dependencies
    implementation("org.apache.poi:poi:5.2.3")
    implementation("org.apache.poi:poi-ooxml:5.4.0")

    // Test dependencies
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testImplementation("com.h2database:h2")
    // https://mvnrepository.com/artifact/com.microsoft.sqlserver/mssql-jdbc
    implementation("com.microsoft.sqlserver:mssql-jdbc:13.2.1.jre11")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// Fixed jar name so the Windows service / Dockerfile never need updating on version bumps.
tasks.bootJar {
    archiveFileName.set("app.jar")
}
