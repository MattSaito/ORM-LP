plugins {
    kotlin("jvm") version "2.1.20"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("redis.clients:jedis:4.4.3") // Cliente Redis
    implementation("org.slf4j:slf4j-simple:1.7.36")
    implementation("org.jetbrains.kotlin:kotlin-reflect") // Para reflexão (útil no ORM)
}

tasks.test {
    useJUnitPlatform()
}