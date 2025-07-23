# Security RSocket

Security package for **WebMorph** ecosystem or external Spring-based applications.
Implements RSocket JWT-based security using LuckPerms and Spring's ReactiveMethodSecurity.

<p align="center">
<a href="https://github.com/web-morph/security?tab=LGPL-3.0-1-ov-file"><img alt="License" src="https://img.shields.io/github/license/web-morph/security"></a>
<a href="https://docs.gradle.org/8.14/release-notes.html"><img src="https://img.shields.io/badge/Gradle-8.14-brightgreen.svg?colorB=469C00&logo=gradle"></a>
<a href="https://repo.billmarssoft.com/api/maven/latest/file/releases/com/github/webmorph/security-rsocket?extension=jar" target="_blank"><img alt="Download" src="https://repo.billmarssoft.com/api/badge/latest/releases/com/github/webmorph/security-rsocket"></a>
<a href="https://repo.billmarssoft.com/javadoc/releases/com/github/webmorph/security-rsocket/latest" target="_blank"><img alt="Download" src="https://img.shields.io/badge/javadoc-latest-red"></a>
</p>

---

## ⚙️ Requirements

* Java 17 or above

## 📦 Installation

⚙️ Gradle (Kotlin DSL – build.gradle.kts)

```kts
repositories {
    maven("https://repo.billmarssoft.com/public/")
}

dependencies {
    implementation("com.github.webmorph:security-rsocket:<version>")
}
```

⚙️ Gradle (Groovy DSL – build.gradle)

```groovy
repositories {
    maven {
        url 'https://repo.billmarssoft.com/public/'
    }
}

dependencies {
    implementation "com.github.webmorph:security-rsocket:<version>"
}
```

# 🛠️ Contributing

Contributions are welcome! Feel free to open an issue or submit a pull request.

## 🧍 Author

### [CKATEPTb](https://github.com/CKATEPTb), [fakeivchenko](https://github.com/fakeivchenko)
