package ai.platon.exotic.standalone.agents

import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.concurrent.RuntimeShutdownHookRegistry
import org.apache.commons.collections4.queue.CircularFifoQueue
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.time.Duration
import java.time.Instant
import java.util.concurrent.Executors

class AgentStarter(
    val options: LauncherOptions
) : AutoCloseable {
    private val logger = getLogger(this::class)
    private val startTime = Instant.now()
    private var process: Process? = null
    private val shutdownWaitTime = Duration.ofSeconds(60)
    private val shutdownHookRegistry = RuntimeShutdownHookRegistry()
    private val shutdownHookThread = Thread { close() }
    private val executor = Executors.newSingleThreadExecutor()
    
    val name = options.name
    val pidPath get() = AppPaths.TMP_DIR.resolve("$name.pid")
    val elapsedTime get() = DateTimes.elapsedTime(startTime)
    var lastOutput: String? = null
        private set
    val outputs = CircularFifoQueue<String>(100)
    val isExpired get() = elapsedTime > options.ttl
    /**
     * TODO: check if the process is busy
     * */
    val isBusy get() = Files.exists(pidPath.resolveSibling("$name.busy.txt"))
    val isAlive get() = process?.isAlive ?: false

    fun start() {
        if (Files.exists(pidPath)) {
            logger.warn("The agent [$name] might not close properly")
            Files.move(pidPath, pidPath.resolveSibling("${pidPath.fileName}.bak"), StandardCopyOption.REPLACE_EXISTING)
        }
        
        executor.submit(this::run)
    }

    fun run() {
        try {
            println(options.executablePath)
            println(options.args)
            
            shutdownHookRegistry.register(shutdownHookThread)
            process = ProcessLauncher.launch(options.executablePath.toString(), options.args)

            process?.also { p ->
                Files.writeString(pidPath, p.pid().toString(), StandardOpenOption.CREATE)
                readInputStream(p)
            }

            waitFor()
        } catch (e: IllegalStateException) {
            shutdownHookRegistry.remove(shutdownHookThread)
            throw e
        } catch (e: IOException) {
            // Unsubscribe from registry on exceptions.
            shutdownHookRegistry.remove(shutdownHookThread)
            throw e
        } catch (e: Exception) {
            // Close the process if failed to start, it throws nothing by design.
            close()
            throw e
        }
    }
    
    fun waitFor() {
        while (process == null) {
            sleepSeconds(1)
        }
        process?.waitFor()
    }
    
    /**
     * No exception.
     * */
    fun stop() = close()
    
    /**
     * No exception.
     * */
    override fun close() {
        val p = process?.takeIf { it.isAlive } ?: return
        runCatching { doClose(p) }.onFailure { warnForClose(this, it) }
    }
    
    private fun doClose(process: Process) {
        logger.info("Closing agent [$name] ...")
        
        Runtimes.destroyProcess(process, shutdownWaitTime)
        kotlin.runCatching { shutdownHookRegistry.remove(shutdownHookThread) }.onFailure { warnUnexpected(this, it) }
        Files.deleteIfExists(pidPath)
        executor.shutdown()
        
        logger.info("Agent [$name] is closed")
    }

    /**
     * Waits for DevTools server is up on chrome process.
     *
     * @param process Chrome process.
     * @return DevTools listening port.
     */
    private fun readInputStream(process: Process) {
        val readLineThread = Thread {
            BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                var line: String? = reader.readLine()?.toByteArray(Charset.defaultCharset())?.toString()
                while (line != null) {
                    if (line.isNotBlank()) {
                        outputs.add(line)
                    }

                    lastOutput = line

                    line = reader.readLine()
                }
            }
        }
        readLineThread.start()
        
        try {
            readLineThread.join(options.startupWaitTime.toMillis())
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            logger.error("Interrupted while waiting for devtools server, close it", e)
            close(readLineThread)
        }
    }
    
    private fun close(thread: Thread) {
        try {
            thread.join(options.threadWaitTime.toMillis())
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }
}
