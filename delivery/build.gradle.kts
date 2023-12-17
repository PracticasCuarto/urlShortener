import kotlinx.kover.gradle.plugin.dsl.GroupingEntityType
plugins {
    id("urlshortener.spring-library-conventions")
    kotlin("plugin.spring")
    id("org.jetbrains.kotlinx.kover") version "0.7.4"
}

repositories {
    mavenCentral()
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
    implementation(project(":core"))
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-hateoas")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("commons-validator:commons-validator:${Version.COMMONS_VALIDATOR}")
    implementation("com.google.guava:guava:${Version.GUAVA}")
    implementation("com.github.ua-parser:uap-java:1.5.4")
    implementation("com.maxmind.geoip2:geoip2:2.14.0")
    implementation("org.springframework.boot:spring-boot-actuator:3.1.3")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.mockito.kotlin:mockito-kotlin:${Version.MOCKITO}")
    testImplementation(project(mapOf("path" to ":mss")))
}
