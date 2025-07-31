group = "com.github.webmorph"
version = "1.1.7"

plugins {
    `java-library`
    `maven-publish`
}

repositories {
    mavenCentral()
    maven("https://repo.billmarssoft.com/public/")
}

dependencies {
    // Reactor
    api("io.projectreactor:reactor-core:3.7.7")
    // Spring
    api("org.springframework.boot:spring-boot-starter-security:3.5.0")
    // Mixin
    api("net.lenni0451.classtransform:core:1.14.1")
    // Permissions
    api("com.github.webmorph:permission:1.0.8")
    // JWT
    api("com.auth0:java-jwt:4.2.1")
    // EventBus
    api("com.github.webmorph:eventbus:1.0.0")
    // Caffeine
    api("com.github.ben-manes.caffeine:caffeine:3.2.1")

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
        enabled = true
    }
}


publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifact(tasks["sourcesJar"])
            artifact(tasks["javadocJar"])
            artifactId = "security"
        }
    }
    repositories {
        maven {
            name = "BillmarsSoft"
            url = uri("https://repo.billmarssoft.com/releases/")
            credentials {
                username = System.getenv("REPOSITORY_USERNAME")
                password = System.getenv("REPOSITORY_PASSWORD")
            }
        }
    }
}