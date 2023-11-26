plugins {
    id("urlshortener.kotlin-common-conventions")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-actuator:3.1.3")
    implementation("com.github.kenglxn.QRGen:javase:3.0.1")
    implementation("io.micrometer:micrometer-registry-prometheus:1.3.5")
    implementation("org.springframework.boot:spring-boot-actuator:3.1.3")

    runtimeOnly("io.micrometer:micrometer-registry-prometheus")
}
