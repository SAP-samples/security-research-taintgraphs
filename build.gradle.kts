import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    // built-in
    java
    application
    kotlin("jvm") version "1.5.10"
}

application {
    mainClassName = "com.thecout.cpg.ApplicationKt"
}


group = "com.thecout.cpg"


repositories {
    mavenCentral()

    ivy {
        setUrl("https://download.eclipse.org/tools/cdt/releases/10.3/cdt-10.3.2/plugins")
        metadataSources {
            artifact()
        }
        patternLayout {
            artifact("/[organisation].[module]_[revision].[ext]")
        }
    }

    maven {
        url = uri("https://jitpack.io")
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

val versions = mapOf(
    "junit5" to "5.6.0",
    "neo4j-ogm" to "3.2.27",
    "cpg" to "4.2.0"
)

dependencies {


    // JUnit
    testImplementation("org.junit.jupiter", "junit-jupiter-api", versions["junit5"])
    testImplementation("org.junit.jupiter", "junit-jupiter-params", versions["junit5"])
    testRuntimeOnly("org.junit.jupiter", "junit-jupiter-engine", versions["junit5"])

    implementation("org.eclipse.jgit:org.eclipse.jgit:6.0.0.202111291000-r")

    implementation("org.apache.logging.log4j:log4j-slf4j18-impl:2.17.1")
    implementation("org.neo4j.driver:neo4j-java-driver:4.1.1")

    api("org.neo4j", "neo4j-ogm-core", versions["neo4j-ogm"])
    api("org.neo4j", "neo4j-ogm", versions["neo4j-ogm"])
    api("org.neo4j", "neo4j-ogm-bolt-driver", versions["neo4j-ogm"])

    // Command line interface support
    api("info.picocli:picocli:4.6.2")
    annotationProcessor("info.picocli:picocli-codegen:4.6.1")
    //api("com.github.Fraunhofer-AISEC.cpg:cpg-core:master-SNAPSHOT")
    api("com.github.anon767.cpg:cpg-core:6003fc7f3c")
    api("com.github.ptnplanet:Java-Naive-Bayes-Classifier:1.0.7")
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "11"
}

val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "11"
}

