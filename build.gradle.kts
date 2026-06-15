import org.jreleaser.model.Active

buildscript {
    repositories {
        mavenCentral()
    }
    dependencyLocking {
        lockAllConfigurations()
    }
}

plugins {
    `java-library`
    jacoco
    `maven-publish`
    signing
    id("org.jreleaser") version "latest.release"
    id("org.sonarqube") version "latest.release"
    id("com.diffplug.spotless") version "latest.release"
}

group = "de.cronn"
version = "2.20.0-SNAPSHOT"

System.getenv("BUILD_NUMBER")?.let { buildNumber ->
    version = "$version-SNAPSHOT-b$buildNumber"
}

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:all,-serial,-overloads,-classfile", "-Werror"))
}

tasks.jacocoTestReport {
    reports {
        xml.required = true
    }
    dependsOn(tasks.test)
}

tasks.wrapper {
    gradleVersion = "9.5.1"
    distributionType = Wrapper.DistributionType.ALL
}

val sourcesJar by tasks.registering(Jar::class) {
    archiveClassifier = "sources"
    from(sourceSets.main.get().allSource)
    dependsOn(tasks.classes)
}

val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier = "javadoc"
    from(tasks.javadoc.get().destinationDir)
    dependsOn(tasks.javadoc)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()
            pom {
                name = project.name
                description = "Java Reflection Utility Classes"
                url = "https://github.com/cronn/reflection-util"

                licenses {
                    license {
                        name = "The Apache Software License, Version 2.0"
                        url = "http://www.apache.org/licenses/LICENSE-2.0.txt"
                        distribution = "repo"
                    }
                }

                developers {
                    developer {
                        id = "benedikt.waldvogel"
                        name = "Benedikt Waldvogel"
                        email = "benedikt.waldvogel@cronn.de"
                    }
                    developer {
                        id = "mark.s.fischer"
                        name = "Mark S. Fischer"
                        email = "reflection-util.x.msf@spam-en.de"
                    }
                }

                scm {
                    url = "https://github.com/cronn/reflection-util"
                }
            }

            from(components["java"])

            artifact(sourcesJar)
            artifact(javadocJar)

            versionMapping {
                usage("java-api") {
                    fromResolutionOf("runtimeClasspath")
                }
                usage("java-runtime") {
                    fromResolutionResult()
                }
            }
        }
    }
    repositories {
        maven {
            name = "staging"
            url = uri(layout.buildDirectory.dir("staging-deploy"))
        }
    }
}

signing {
    useGpgCmd()
    sign(publishing.publications["mavenJava"])
}

jreleaser {
    signing {
        signing {
            active = Active.NEVER
        }
    }
    deploy {
        maven {
            mavenCentral {
                create("sonatype") {
                    active = Active.RELEASE
                    sign = false
                    url = "https://central.sonatype.com/api/v1/publisher"
                    stagingRepository("build/staging-deploy")
                }
            }
        }
    }
}

tasks.test {
    useJUnitPlatform()
    maxHeapSize = "256m"
}

spotless {
    java {
        googleJavaFormat()
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }
    kotlinGradle {
        ktlint()
        trimTrailingWhitespace()
        endWithNewline()
    }
}

dependencyLocking {
    lockAllConfigurations()
}

dependencies {
    implementation("net.bytebuddy:byte-buddy:latest.release")
    implementation("org.objenesis:objenesis:latest.release")

    testImplementation("org.junit.jupiter:junit-jupiter-api:latest.release")
    testImplementation("org.junit.jupiter:junit-jupiter-params:latest.release")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:latest.release")
    testImplementation("org.mockito:mockito-core:latest.release")
    testImplementation("org.assertj:assertj-core:latest.release")
    testImplementation("jakarta.validation:jakarta.validation-api:latest.release")
    testImplementation("org.apache.commons:commons-lang3:latest.release")
    testImplementation("org.javassist:javassist:latest.release")
    testImplementation("nl.jqno.equalsverifier:equalsverifier:latest.release")

    testImplementation("org.hibernate.orm:hibernate-core:latest.release")
    testRuntimeOnly("com.h2database:h2:latest.release")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    testImplementation("org.openjdk.jmh:jmh-core:latest.release")
    testRuntimeOnly("org.openjdk.jmh:jmh-generator-annprocess:latest.release")

    // no real transitive dependency but we use it to annotate method contracts to help the IDE understand the code
    compileOnly("org.jetbrains:annotations:latest.release")
    testCompileOnly("org.jetbrains:annotations:latest.release")

    components {
        all {
            if (id.version.matches(Regex("(?i).+(-|\\.)(CANDIDATE|RC|BETA|ALPHA|PR|M\\d+|CR\\d+).*"))) {
                status = "milestone"
            }
        }
    }
}
