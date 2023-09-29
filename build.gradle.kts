

plugins {
    kotlin("multiplatform") version Versions.kotlinCompiler apply false
}

allprojects {

    repositories {
        mavenCentral()
    }

}

subprojects {
    apply(plugin = "kotlin-multiplatform")
}

