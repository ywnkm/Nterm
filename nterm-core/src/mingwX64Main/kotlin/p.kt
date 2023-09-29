package nterm

import kotlinx.cinterop.memScoped
import platform.windows.CreateProcess


public actual fun foo() {
    println("Hello from mingw")
}