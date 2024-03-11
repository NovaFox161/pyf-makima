import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import org.gradle.api.tasks.wrapper.Wrapper.DistributionType.ALL

plugins {
    // Kotlin
    kotlin("jvm") version "1.9.23"

    // Spring
    kotlin("plugin.spring") version "1.9.23"
    id("org.springframework.boot") version "3.2.3"
    id("io.spring.dependency-management") version "1.1.4"

    // Tooling
    id("com.gorylenko.gradle-git-properties") version "2.4.1"
    id("com.google.cloud.tools.jib") version "3.4.1"
}

buildscript {
    dependencies {
        classpath("com.squareup:kotlinpoet:1.16.0")
    }
}

val makimaVersion = "1.0.9"
val gradleWrapperVersion = "8.6"
val javaVersion = "19"
val d4jVersion = "3.3.0-M2"
val d4jStoresVersion = "3.2.2"
val discordWebhooksVersion = "0.8.4"
val orgJsonVersion = "20240303"
val springMockkVersion = "4.0.2"
val logbackContribVersion = "0.1.5"

group = "nova"
version = makimaVersion

val buildVersion = if (System.getenv("GITHUB_RUN_NUMBER") != null) {
    "$version.b${System.getenv("GITHUB_RUN_NUMBER")}"
} else {
    "$version.d${System.currentTimeMillis().div(1000)}" //Seconds since epoch
}

val kotlinSrcDir: File = layout.buildDirectory.dir("src/main/kotlin").map(Directory::getAsFile).get()

java {
    sourceCompatibility = JavaVersion.toVersion(javaVersion)
    targetCompatibility = sourceCompatibility
}

kotlin {
    sourceSets {
        all {
            kotlin.srcDir(kotlinSrcDir)
        }
    }
}

repositories {
    mavenCentral()

    maven("https://repo.maven.apache.org/maven2/")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
}

dependencies {
    // Tools
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")

    // Spring
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // Database
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-mysql")
    implementation("io.asyncer:r2dbc-mysql")
    implementation("com.mysql:mysql-connector-j")


    // Serialization
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("org.json:json:$orgJsonVersion")

    // Observability
    implementation("ch.qos.logback.contrib:logback-json-classic:$logbackContribVersion")
    implementation("ch.qos.logback.contrib:logback-jackson:$logbackContribVersion")
    implementation("io.micrometer:micrometer-registry-prometheus")

    // Discord
    implementation("com.discord4j:discord4j-core:$d4jVersion")
    implementation("com.discord4j:stores-redis:$d4jStoresVersion")
    implementation("club.minnced:discord-webhooks:$discordWebhooksVersion") {
        // Due to vulnerability in older versions: https://github.com/advisories/GHSA-rm7j-f5g5-27vv
        exclude(group = "org.json", module = "json")
    }

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(module = "junit")
        exclude(module = "mockito-core")
    }
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("com.ninja-squad:springmockk:$springMockkVersion")
}

jib {
    to {
        image = "rg.nl-ams.scw.cloud/dreamexposure/pyf-makima"
        tags = mutableSetOf("latest", makimaVersion, buildVersion)
    }

    from.image = "eclipse-temurin:19-jre-alpine"
}

gitProperties {
    extProperty = "gitPropertiesExt"

    customProperty("makima.version", buildVersion)
    customProperty("makima.version.d4j", d4jVersion)
}

tasks {
    generateGitProperties {
        doLast {
            @Suppress("UNCHECKED_CAST")
            val gitProperties = ext[gitProperties.extProperty] as Map<String, String>
            val enumPairs = gitProperties.mapKeys { it.key.replace('.', '_').uppercase() }

            val enumBuilder = TypeSpec.enumBuilder("GitProperty")
                .primaryConstructor(
                    com.squareup.kotlinpoet.FunSpec.constructorBuilder()
                        .addParameter("value", String::class)
                        .build()
                )

            val enums = enumPairs.entries.fold(enumBuilder) { accumulator, (key, value) ->
                accumulator.addEnumConstant(
                    key, TypeSpec.anonymousClassBuilder()
                    .addSuperclassConstructorParameter("%S", value)
                    .build()
                )
            }

            val enumFile = FileSpec.builder("nova.pyfmakima", "GitProperty")
                .addType(
                    // https://github.com/square/kotlinpoet#enums
                    enums.addProperty(
                        PropertySpec.builder("value", String::class)
                            .initializer("value")
                            .build()
                    ).build()
                ).build()

            enumFile.writeTo(kotlinSrcDir)
        }
    }

    compileKotlin {
        dependsOn(generateGitProperties)

        kotlinOptions {
            freeCompilerArgs = listOf("-Xjsr305=strict")
            jvmTarget = java.targetCompatibility.majorVersion
        }
    }

    test {
        useJUnitPlatform()
    }

    wrapper {
        distributionType = ALL
        gradleVersion = gradleWrapperVersion
    }
}
