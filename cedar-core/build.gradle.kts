plugins {
    `java-library`
}

dependencies {
    api(libs.slf4j.api)
    api(libs.jackson.databind)
    api(libs.jackson.annotations)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit.jupiter)
}
