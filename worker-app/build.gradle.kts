plugins {
    java
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

dependencies {
    implementation(project(":shared-contracts"))
    implementation(project(":api-app"))

    implementation(libs.spring.boot)
    implementation(libs.spring.boot.jpa)
    implementation(libs.spring.boot.kafka)
    implementation(libs.jackson.databind)
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    runtimeOnly(libs.postgres)

    testImplementation(libs.spring.boot.test)
}
