package ai.platon.exotic.standalone.agents

import java.nio.file.Path
import java.time.Duration

class LauncherOptions(
    val name: String,
    val executablePath: Path,
    val args: List<String> = emptyList(),
    val ttl: Duration = Duration.ofDays(3650),
    var startupWaitTime: Duration = DEFAULT_STARTUP_WAIT_TIME,
    var shutdownWaitTime: Duration = DEFAULT_SHUTDOWN_WAIT_TIME,
    var threadWaitTime: Duration = THREAD_JOIN_WAIT_TIME
) {
    companion object {
        /** Default startup wait time in seconds. */
        val DEFAULT_STARTUP_WAIT_TIME = Duration.ofSeconds(60)
        /** Default shutdown wait time in seconds. */
        val DEFAULT_SHUTDOWN_WAIT_TIME = Duration.ofSeconds(60)
        /** 5 seconds wait time for threads to stop. */
        val THREAD_JOIN_WAIT_TIME = Duration.ofSeconds(5)
    }
}

