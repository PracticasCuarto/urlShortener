plugins {
    id("urlshortener.kotlin-common-conventions")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-actuator:3.1.3")
    implementation("com.github.kenglxn.QRGen:javase:3.0.1")
    implementation("io.micrometer:micrometer-registry-prometheus:1.3.5")
    implementation("org.springframework.boot:spring-boot-actuator:3.1.3")
    implementation("com.bucket4j:bucket4j-core:8.7.0")

    runtimeOnly("io.micrometer:micrometer-registry-prometheus")
}