import kotlinx.kover.gradle.plugin.dsl.GroupingEntityType
plugins {
    id("urlshortener.spring-library-conventions")
    kotlin("plugin.jpa")
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
//    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
}
