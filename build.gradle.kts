import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.0"
    kotlin("plugin.serialization") version "1.6.10"
    application
    `maven-publish`
}

repositories {
    mavenCentral()
}

val exposedVersion: String by project

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    //api group: 'org.jetbrains.kotlin', name: 'kotlin-reflect', version: '1.3.50'
    testImplementation("junit:junit:4.13.2")
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed", "exposed-java-time", exposedVersion)
    implementation("org.xerial:sqlite-jdbc:3.36.0.2")
    implementation("mysql:mysql-connector-java:8.0.25")
    implementation("com.discord4j:discord4j-core:3.2.1")
    implementation("org.slf4j", "slf4j-simple", "2.0.0-alpha5")
    implementation("com.charleskorn.kaml:kaml:0.38.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.1")
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    mainClass.set("Main")
}

val fatJar = task("fatJar", type = Jar::class) {
    val version = this.archiveVersion.get()
    this.setProperty("archiveFileName", "Pinnwand-$version.jar")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes["Implementation-Title"] = "Pinnwand"
        attributes["Implementation-Version"] = version
        attributes["Main-Class"] = "Main"
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    with(tasks.jar.get() as CopySpec)
}

publishing {
    publications {
        create<MavenPublication>("default") {
            from(components["java"])
            // Include any other artifacts here, like javadocs
        }
    }

    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/ChrisGold/Pinnwand3")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}