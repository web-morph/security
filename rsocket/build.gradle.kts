group = "com.github.webmorph"
version = "1.0.0"

plugins {
    `java-library`
    `maven-publish`
}

repositories {
    mavenCentral()
    maven("https://repo.jyraf.com/repository/maven-public/")
}

dependencies {
    compileOnly(project(":security"))

    api("org.springframework.boot:spring-boot-starter-rsocket:3.5.0")
    api("org.springframework.boot:spring-boot-starter-security:3.5.0")
    api("org.springframework.security:spring-security-messaging:6.5.0")
    api("org.springframework.security:spring-security-rsocket:6.5.0")

    // Lombok
    compileOnly("org.projectlombok:lombok:1.18.38")
    annotationProcessor("org.projectlombok:lombok:1.18.38")
}

tasks {
    register<Jar>("sourcesJar") {
        archiveClassifier.set("sources")
        from(sourceSets.main.get().allSource)
    }
    register<Jar>("javadocJar") {
        archiveClassifier.set("javadoc")
        from(javadoc)
    }
    javadoc {
        options.encoding = "UTF-8"
        options.memberLevel = JavadocMemberLevel.PUBLIC
        isFailOnError = false
    }
    withType<JavaCompile> {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(17)
    }
    build {
        dependsOn("sourcesJar", "javadocJar")
    }
    jar {
        enabled = false
    }
}


publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifact(tasks["sourcesJar"])
            artifact(tasks["javadocJar"])
        }
    }
    repositories {
        maven {
            name = "jyrafRepo"
            url = uri("https://repo.jyraf.com/repository/maven-releases/")
            credentials {
                username = System.getenv("NEXUS_USERNAME")
                password = System.getenv("NEXUS_PASSWORD")
            }
        }
    }
}