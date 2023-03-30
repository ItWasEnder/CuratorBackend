plugins {
    id("java")
}

group = "tv.ender"
version = "1.0.0-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_17

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "tv.ender.App"
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<Javadoc>{
    options.encoding = "UTF-8"
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("org.projectlombok:lombok:1.18.26")
    annotationProcessor("org.projectlombok:lombok:1.18.26")

    implementation("com.google.guava:guava:31.1-jre")
    implementation("com.discord4j:discord4j-core:3.2.4")

    testImplementation("junit:junit:4.13.2")
    implementation("com.google.firebase:firebase-admin:9.1.1")
}