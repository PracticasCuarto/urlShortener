import kotlinx.kover.gradle.plugin.dsl.GroupingEntityType

plugins {
    id("urlshortener.kotlin-common-conventions")
    id("org.jetbrains.kotlinx.kover") version "0.7.4"
}

kover {
    excludeJavaCode()
}
koverReport {
    defaults {
        log {
            groupBy = GroupingEntityType.CLASS
        }
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-actuator:3.1.3")
    implementation("com.github.kenglxn.QRGen:javase:3.0.1")
    implementation("io.micrometer:micrometer-registry-prometheus:1.3.5")
    implementation("org.springframework.boot:spring-boot-actuator:3.1.3")
    implementation("com.bucket4j:bucket4j-core:8.7.0")
    implementation("com.github.ua-parser:uap-java:1.5.4")
    implementation("com.maxmind.geoip2:geoip2:2.14.0")

    runtimeOnly("io.micrometer:micrometer-registry-prometheus")
}