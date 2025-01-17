rootProject.name = "BreweryX"


plugins {
    // add toolchain resolver
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}
include(":nms:v1_21_3", ":nms:v1_21_4")
