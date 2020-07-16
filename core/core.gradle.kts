import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java-library")
    id("org.jetbrains.kotlin.jvm")
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
}

repositories {
    jcenter()
    mavenCentral()
}

dependencies {
    val vertxVersion = "4.0.0-milestone5"
    val guiceVersion = "4.2.3"
    val guavaVersion = "28.2-jre"
    val jacksonVersion = "2.10.3"
    val kotlinVersion = "1.3.72"
    val junitVersion = "5.6.2"
    val mockKVersion = "1.9.3"
    val byteBuddyVersion = "1.10.11"
    val reflectasmVersion = "1.11.9"
    val kotlinxVersion = "1.3.7"
    val apiGuadianVersion = "1.1.0"
    val log4jVersion = "2.13.3"
    val slf4jVersion = "1.7.30"
    // This dependency is exported to consumers, that is to say found on their compile classpath.
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$kotlinxVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinxVersion")
    implementation("com.esotericsoftware:reflectasm:$reflectasmVersion")
    api("org.apache.commons:commons-math3:3.6.1")
    api("io.vertx:vertx-core:$vertxVersion")
    api("io.vertx:vertx-redis-client:$vertxVersion")
    api("io.vertx:vertx-web:$vertxVersion")
    api("io.vertx:vertx-web-templ-thymeleaf:$vertxVersion")
    api("io.vertx:vertx-lang-kotlin-coroutines:$vertxVersion")
    api("io.vertx:vertx-codegen:$vertxVersion")
    api("javax.inject:javax.inject:1")
    api("com.google.inject:guice:$guiceVersion")
    api("com.github.blindpirate:annotation-magic:0.1")
    api("org.slf4j:slf4j-api:$slf4jVersion")
    // https://mvnrepository.com/artifact/javax.ws.rs/jsr311-api
    compileOnly("javax.ws.rs:jsr311-api:1.1.1")
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
    implementation("org.apache.commons:commons-lang3:3.10")
    implementation("commons-io:commons-io:2.7")
    implementation("com.google.guava:guava:$guavaVersion")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:$log4jVersion")
    implementation("org.apache.logging.log4j:log4j-core:$log4jVersion")
    implementation("com.fasterxml.jackson.core:jackson-core:$jacksonVersion")
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.fasterxml.jackson.core:jackson-annotations:$jacksonVersion")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:$jacksonVersion")
    implementation("net.bytebuddy:byte-buddy-agent:$byteBuddyVersion")
    implementation("net.bytebuddy:byte-buddy:$byteBuddyVersion")
    implementation("org.apiguardian:apiguardian-api:$apiGuadianVersion")

    implementation("com.google.guava:guava:$guavaVersion")

    testImplementation("io.vertx:vertx-unit:$vertxVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:$junitVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitVersion")
    testImplementation("io.mockk:mockk:$mockKVersion")
    testImplementation("io.github.glytching:junit-extensions:2.4.0")
}

val test by tasks.getting(Test::class) {
    useJUnitPlatform {
        includeEngines("junit-jupiter", "junit-vintage")
    }
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
}