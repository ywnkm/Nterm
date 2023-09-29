package nterm.pty

public class PtyProcessOption(
    public val command: String,
    public val args: Array<String>,
    public val env: Map<String, String>,
    public val dir: String,
    public val initRow: Int,
    public val initCol: Int,
)
