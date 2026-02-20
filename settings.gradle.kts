rootProject.name = "cedar-authorization-framework"

include("cedar-core")
include("cedar-jaxrs")

// Dependency version management
dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            version("jackson", "2.15.2")
            version("slf4j", "2.0.9")
            version("jaxrs", "3.1.0")
            version("jakarta-annotation", "2.1.1")
            version("aws-sdk", "2.20.0")
            version("junit", "5.10.0")
            version("mockito", "5.5.0")
            version("jersey", "3.1.3")

            library("jackson-databind", "com.fasterxml.jackson.core", "jackson-databind").versionRef("jackson")
            library("jackson-annotations", "com.fasterxml.jackson.core", "jackson-annotations").versionRef("jackson")
            library("slf4j-api", "org.slf4j", "slf4j-api").versionRef("slf4j")
            library("jaxrs-api", "jakarta.ws.rs", "jakarta.ws.rs-api").versionRef("jaxrs")
            library("jakarta-annotation-api", "jakarta.annotation", "jakarta.annotation-api").versionRef("jakarta-annotation")
            library("aws-verifiedpermissions", "software.amazon.awssdk", "verifiedpermissions").versionRef("aws-sdk")
            library("aws-core", "software.amazon.awssdk", "sdk-core").versionRef("aws-sdk")

            library("junit-jupiter", "org.junit.jupiter", "junit-jupiter").versionRef("junit")
            library("mockito-core", "org.mockito", "mockito-core").versionRef("mockito")
            library("mockito-junit-jupiter", "org.mockito", "mockito-junit-jupiter").versionRef("mockito")
            library("jersey-server", "org.glassfish.jersey.core", "jersey-server").versionRef("jersey")
            library("jersey-common", "org.glassfish.jersey.core", "jersey-common").versionRef("jersey")
        }
    }
}
