plugins {
    `java-library`
}

dependencies {
    api(project(":cedar-core"))
    api(libs.jaxrs.api)
    api(libs.jakarta.annotation.api)
    api(libs.slf4j.api)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit.jupiter)
    testImplementation(libs.jersey.server)
    testImplementation(libs.jersey.common)
}
