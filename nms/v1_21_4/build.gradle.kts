// TODO: update to gradle 8.12
plugins {
    id("java")
    //id("io.papermc.paperweight.userdev") version "2.0.0-beta.14"
}

// TODO: set this values equal to parent project
group = "com.dre.brewery"
version = "3.4.8"

repositories {
    mavenCentral()
    //maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    implementation(project(":"))
    //paperweight.paperDevBundle("1.21.4-R0.1-SNAPSHOT")
}

//java {
//    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
//}
