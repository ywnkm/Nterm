package nterm.pty

import kotlinx.cinterop.*
import platform.posix.*

private fun createPty(): Pair<Int, String> {
    val ptm = open("/dev/ptmx", O_RDWR or O_NOCTTY)
    if (ptm < 0) throw Exception("open /dev/ptmx failed")
    if (grantpt(ptm) < 0) throw Exception("grantpt failed")
    if (unlockpt(ptm) < 0) throw Exception("unlockpt failed")
    val ptsName = ptsname(ptm)?.toKString() ?: throw Exception("ptsname failed")
    return ptm to ptsName

}

internal fun createPtyProcess(option: PtyProcessOption): Int {
    val (ptm, ptsName) = createPty()
    memScoped {
        val tios = alloc<termios>()
        if (tcgetattr(ptm, tios.ptr) < 0) throw Exception("tcgetattr failed")
        tios.c_iflag = tios.c_iflag or IUTF8.toUInt()
        tios.c_oflag = tios.c_oflag and (IXON or IXOFF or IXANY).inv().toUInt()
        tcsetattr(ptm, TCSANOW, tios.ptr)

        val winsize = alloc<winsize>()
        winsize.ws_row = option.initRow.toUShort()
        winsize.ws_col = option.initCol.toUShort()
        ioctl(ptm, TIOCSWINSZ.toULong(), winsize.ptr)
    }
    val pid = fork()
    if (pid < 0) {
        close(ptm)
        throw IllegalStateException("fork failed")
    }
    if (pid > 0) {
        return ptm
    } else { // child
        close(ptm)
        chdir(option.dir)
        setsid()
        val pts = open(ptsName, O_RDWR)
        if (pts < 0) throw IllegalStateException("open pts failed")

        println("pts $pts")
        dup2(pts, STDIN_FILENO)
        println("1")
        dup2(pts, STDOUT_FILENO)
        println("2")
        dup2(pts, STDERR_FILENO)
        println("3")
        close(pts)

        memScoped {
            val dir = opendir("/proc/self/fd") ?: throw Exception("opendir failed")
            val selfDirFd = dirfd(dir)
            var entry: CPointer<dirent>?
            while (true) {
                entry = readdir(dir) ?: break
                val fd = atoi(entry.pointed.d_name.toKString())
                if (fd == ptm || fd == selfDirFd) continue
                close(fd)
            }
            closedir(dir)

            val envp = allocArray<CPointerVar<ByteVar>>(option.env.size)
            for ((i, e) in option.env.entries.withIndex()) {
                val str = "${e.key}=${e.value}".cstr.getPointer(this)
                envp[i] = str
            }
            val args = allocArray<CPointerVar<ByteVar>>(option.args.size)
            for ((i, e) in option.args.withIndex()) {
                val str = e.cstr.getPointer(this)
                args[i] = str
            }
            execve(option.command, args, envp)
        }
        exit(0);
        error("Unreachable")
    }
}
