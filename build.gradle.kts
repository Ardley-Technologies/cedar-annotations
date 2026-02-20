plugins {
    java
    `maven-publish`
}

allprojects {
    group = "com.ardley"
    version = "1.0.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "maven-publish")

    java {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        withSourcesJar()
        withJavadocJar()
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    publishing {
        publications {
            create<MavenPublication>("maven") {
                from(components["java"])

                pom {
                    name.set(project.name)
                    description.set("Cedar Authorization Framework for JAX-RS")
                    url.set("https://github.com/Ardley-Technologies/cedar-annotations")

                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }

                    developers {
                        developer {
                            id.set("ardley")
                            name.set("Ardley Technologies")
                            email.set("engineering@ardley.com")
                        }
                    }

                    scm {
                        connection.set("scm:git:git://github.com/Ardley-Technologies/cedar-annotations.git")
                        developerConnection.set("scm:git:ssh://github.com:Ardley-Technologies/cedar-annotations.git")
                        url.set("https://github.com/Ardley-Technologies/cedar-annotations")
                    }
                }
            }
        }

        repositories {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/Ardley-Technologies/cedar-annotations")
                credentials {
                    username = System.getenv("GITHUB_ACTOR")
                    password = System.getenv("GITHUB_TOKEN")
                }
            }
        }
    }
}
