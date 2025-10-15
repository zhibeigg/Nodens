import io.izzel.taboolib.gradle.*
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val publishUsername: String by project
val publishPassword: String by project
val build: String by project

plugins {
    java
    `maven-publish`
    kotlin("jvm") version "2.1.20"
    kotlin("plugin.serialization") version "2.1.20"
    id("io.izzel.taboolib") version "2.0.23"
}

taboolib {
    env {
        install(Basic)
        install(Bukkit)
        install(BukkitFakeOp)
        install(BukkitHook)
        install(BukkitUI)
        install(BukkitUtil)
        install(BukkitNMS)
        install(BukkitNMSUtil)
        install(BukkitNMSItemTag)
        install(BukkitNMSDataSerializer)
        install(I18n)
        install(CommandHelper)
        install(MinecraftChat)
        install(Metrics)
        install(Database)
        install(Kether)
        install(JavaScript)
        install(Jexl)
        //repoTabooLib = "https://www.mcwar.cn/nexus/repository/maven-public/"
    }
    description {
        name = "Nodens"
        desc("稳定高效的属性插件")
        contributors {
            name("纸杯")
        }
        dependencies {
            name("DragonCore").optional(true)
            name("DragonArmourers").optional(true)
            name("GlowAPI").optional(true)
        }
    }
    relocate("com.github.benmanes.caffeine", "org.gitee.nodens.caffeine")
    relocate("com.eatthepath.uuid", "org.gitee.nodens.eatthepath.uuid")
    relocate("kotlinx.serialization", "org.gitee.nodens.serialization")
    relocate("org.xerial.snappy", "org.gitee.nodens.xerial.snappy")
    version { taboolib = "6.2.3-b217935" }
}

repositories {
    mavenCentral()
    maven("https://repo.tabooproject.org/repository/releases")
    maven("https://www.mcwar.cn/nexus/repository/maven-public/")
}

dependencies {
    compileOnly("ink.ptms.core:v12004:12004:mapped")
    compileOnly("ink.ptms.core:v12004:12004:universal")
    compileOnly("ink.ptms.core:v11200:11200")
    compileOnly("ink.ptms:nms-all:1.0.0")

    compileOnly("com.gitee.redischannel:RedisChannel:latest.release:api")
    compileOnly("org.eldergod.ext:DragonCore:2.6.2.9")
    compileOnly("org.eldergod.ext:DragonArmourers:6.72")
    compileOnly("org.eldergod.ext:MythicMobs:4.11.0")
    compileOnly("org.eldergod.ext:GlowAPI:1.4.6")
    compileOnly("org.gitee.orryx:Orryx:latest.release:api")

    compileOnly("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
    compileOnly("com.eatthepath:fast-uuid:0.2.0")
    compileOnly("com.github.ben-manes.caffeine:caffeine:2.9.3")
    compileOnly("org.xerial.snappy:snappy-java:1.1.10.7")

    compileOnly(kotlin("stdlib"))
    compileOnly(kotlin("reflect"))
    compileOnly(fileTree("libs"))
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<Jar> {
    destinationDirectory.set(File(build))
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_1_8)
        freeCompilerArgs.set(listOf("-Xjvm-default=all"))
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

kotlin {
    sourceSets.all {
        languageSettings {
            languageVersion = "2.0"
        }
    }
}

publishing {
    repositories {
        maven {
            url = uri("https://www.mcwar.cn/nexus/repository/maven-releases/")
            credentials {
                username = publishUsername
                password = publishPassword
            }
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }
    publications {
        create<MavenPublication>("library") {
            from(components["java"])
            artifact(tasks["kotlinSourcesJar"]) {
                classifier = "sources"
            }
            artifact("${build}/${rootProject.name}-${version}-api.jar") {
                classifier = "api"
            }
            groupId = project.group.toString()
        }
    }
}