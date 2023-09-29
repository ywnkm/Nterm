

plugins {
    kotlin("multiplatform")
}


kotlin {

    explicitApi()

    jvm("jvm")

    mingwX64("mingwX64")

    linuxX64("linuxX64")


    sourceSets {
        all {
            languageSettings {
                optIn("kotlinx.cinterop.ExperimentalForeignApi")
            }
        }

        val commonMain by getting {
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        val unixCommon by creating {
            dependsOn(commonMain)
        }

        val linuxX64Main by getting {
            dependsOn(unixCommon)
        }

        val mingwX64Main by getting {

        }
    }

}
