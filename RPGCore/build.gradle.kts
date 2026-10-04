plugins {
    java
}

group = "com.hcs"
version = "0.4.9"

repositories {
    mavenCentral()

    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }

    maven("https://maven.blamejared.com/")
    maven("https://maven.nucleoid.xyz/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.2.build.+")
    compileOnly(fileTree("/srv/minecraft/server/plugins") {
        include("PlaceholderAPI*.jar")
    })

    // 기존 플러그인 API

    // BetterModel 3.5.0-SNAPSHOT-519 API
    compileOnly(files("/srv/minecraft/server/plugins/bettermodel-3.5.0-SNAPSHOT-519-paper.jar"))

    // MythicMobs 5.13.0 API
    compileOnly(files("/srv/minecraft/server/plugins/MythicMobs-5.13.0.jar"))
    compileOnly(files("/srv/minecraft/server/plugins/VaultUnlocked-2.20.3.jar"))

    // CraftEngine 26.9.1 API
    compileOnly(files("/srv/minecraft/server/plugins/craft-engine-paper-plugin-26.9.1.jar"))

    // ItemsAdder 4.0.18 API
    compileOnly(files("/srv/minecraft/server/plugins/ItemsAdder_4.0.18.jar"))

    // Citizens 2.0.43 build 4246 API
    compileOnly(files("/srv/minecraft/server/plugins/Citizens-2.0.43-b4246.jar"))

    implementation("com.zaxxer:HikariCP:7.0.2")
    implementation("org.mariadb.jdbc:mariadb-java-client:3.5.8")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.release.set(25)
    }

    processResources {
        filteringCharset = "UTF-8"

        filesMatching("plugin.yml") {
            expand(
                "version" to project.version
            )
        }
    }

    jar {
        archiveBaseName.set("RPGCore")
    }
}
