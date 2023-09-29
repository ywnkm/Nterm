package nterm.internal

import kotlinx.cinterop.*
import nterm.pty.PtyProcessOption
import platform.posix.*

@OptIn(ExperimentalForeignApi::class)
internal fun createSubProcess(option: PtyProcessOption): Pair<Int, Int> {
    val ptm = open("/dev/ptmx", O_RDWR or O_CLOEXEC)
    if (ptm < 0) throw Exception("open /dev/ptmx failed")
    if (grantpt(ptm) < 0) throw Exception("grantpt failed")
    if (unlockpt(ptm) < 0) throw Exception("unlockpt failed")
    val ptsName = ptsname(ptm)?.toKString() ?: throw Exception("ptsname failed")
    println("ptsName $ptsName")


    memScoped {
        val tios = alloc<termios>()
        tcgetattr(ptm, tios.ptr)
        tios.c_iflag = tios.c_iflag or IUTF8.toUInt()
        tios.c_iflag = tios.c_iflag and (IXON or IXOFF).inv().toUInt()
        tcsetattr(ptm, TCSANOW, tios.ptr)

        val sz = alloc<winsize> {
            ws_row = option.initRow.toUShort()
            ws_col = option.initCol.toUShort()
        }
        if (ioctl(ptm, TIOCSWINSZ.toULong(), sz.ptr) < 0) throw Exception("ioctl TIOCSWINSZ failed")
    }

    val pid = fork()
    if (pid < 0) throw Exception("fork failed")
    if (pid > 0) {
        return ptm to pid
    } else {
        println("sub start")
        close(ptm)
        setsid()

        val pts = open(ptsName, O_RDWR)
        if (pts < 0) throw Exception("open pts failed")

        dup2(pts, 0)
        dup2(pts, 1)
        dup2(pts, 2)

        if (chdir(option.dir) < 0) throw Exception("chdir failed")


        println("aaa")
        // put env
        for ((k, v) in option.env) {
            putenv("$k=$v".cstr)
        }

        println("bbb")
        memScoped {

            println("alloc ${option.args.size}")
            val arr = allocArray<CPointerVar<ByteVar>>(option.args.size)
            println(sizeOf<CPointerVar<ByteVar>>())
            for ((i, s) in option.args.withIndex()) {
                arr[i] = s.cstr.getPointer(this)
            }
            println("execv ${option.command[0]}")
            execv(option.command, arr)
        }

        println("sub suc")

        exit(0)
        error("")
    }
}
