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
    implementation("io.vertx:vertx-web:4.3.4")
    implementation("io.vertx:vertx-unit:4.3.4")
    implementation("ch.qos.logback:logback-classic:1.4.4")
    testImplementation("junit:junit:4.13.2")
}