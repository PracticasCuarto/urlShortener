plugins {
    id("urlshortener.spring-app-conventions")
    kotlin("plugin.spring")
}

dependencies {
    implementation(project(":core"))
    implementation(project(":delivery"))
    implementation(project(":repositories"))
    implementation(project(":mss"))
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.webjars:bootstrap:${Version.BOOTSTRAP}")
    implementation("org.webjars:jquery:${Version.JQUERY}")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-amqp")


    runtimeOnly("org.hsqldb:hsqldb")

    developmentOnly("org.springframework.boot:spring-boot-docker-compose")


    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-web")
    testImplementation("org.springframework.boot:spring-boot-starter-jdbc")
    testImplementation("org.mockito.kotlin:mockito-kotlin:${Version.MOCKITO}")
    testImplementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    testImplementation("org.apache.httpcomponents.client5:httpclient5")

    //esta comentada porque peta el arrancarlo asi con el docker automatico
    //developmentOnly("org.springframework.boot:spring-boot-docker-compose")
}
