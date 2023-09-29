package nterm

import kotlinx.cinterop.*
import nterm.internal.createSubProcess
import nterm.pty.PtyProcessOption
import nterm.pty.createPtyProcess
import platform.posix.*

public actual fun foo() {
    val option = PtyProcessOption(
        "/bin/bash",
        arrayOf(),
        mapOf(),
        "/",
        100,
        100,
    )
    val ptm = createPtyProcess(option)
    println("ptm $ptm")

    memScoped {
        val thread = alloc<pthread_tVar>()
        val p = cValue<IntVar>()
        p.ptr[0] = ptm
        pthread_create(thread.ptr, null, staticCFunction { it ->
            memScoped {
                val buf = allocArray<ByteVar>(1024)
                while (true) {
                    read((it as CPointer<IntVar>)[0], buf, 1024u)
                    println(buf.toKString())
                }
            }
            null
        }, p)
        val buf = allocArray<ByteVar>(1)
        while (true) {
            val c  = fgetc(stdin)
            buf[0] = c.toByte()
            write(ptm, buf, 1u)
        }
    }
    sleep(10u)
}

