plugins {
    java
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

dependencies {
    implementation(platform(libs.spring.ai.bom))

    implementation(project(":shared-contracts"))

    implementation(libs.spring.boot.web)
    implementation(libs.spring.boot.kafka)
    implementation(libs.spring.boot.security)
    implementation(libs.spring.boot.oauth2.resource.server)
    implementation(libs.spring.boot.validation)
    implementation(libs.spring.boot.jpa)
    implementation(libs.spring.jdbc)
    implementation(libs.flyway)
    implementation(libs.flyway.postgres)
    implementation(libs.spring.ai.ollama)
    implementation(libs.spring.ai.pgvector)
    implementation(libs.jackson.databind)
    implementation(libs.mapstruct)
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    runtimeOnly(libs.postgres)
    annotationProcessor(libs.mapstruct.processor)

    testImplementation(libs.spring.boot.test)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.kafka)
}
