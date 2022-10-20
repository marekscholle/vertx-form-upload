plugins {
    application
}

repositories {
    mavenCentral()
}

java {
    targetCompatibility = JavaVersion.VERSION_11
    sourceCompatibility = JavaVersion.VERSION_11
}

dependencies {
    implementation("io.vertx:vertx-web:3.9.5")
    implementation("io.vertx:vertx-unit:3.9.5")
    implementation("ch.qos.logback:logback-classic:1.4.4")
    testImplementation("junit:junit:4.13.2")
}

tasks.withType<Test> {
    minHeapSize = "32m"
    maxHeapSize = "64m"
}
