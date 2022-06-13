package ai.platon.exotic.services.api

import ai.platon.pulsar.common.getLogger
import org.apache.commons.lang3.SystemUtils
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.support.AbstractApplicationContext
import java.nio.file.Files
import java.nio.file.Paths

class ExoticContextInitializer(
    private val isServlet: Boolean = false
) : ApplicationContextInitializer<AbstractApplicationContext> {
    private val logger = getLogger(this)

    override fun initialize(applicationContext: AbstractApplicationContext) {
        val isTomcat = SystemUtils.USER_NAME == "tomcat"
        if (isTomcat) {
            detectDatabasePathOnTomcat()
        }
        reportEnvironment()
    }

    private fun reportEnvironment() {
        val env = System.getenv().toMutableMap()
        env["USER_HOME"] = SystemUtils.USER_HOME
        env["USER_DIR"] = SystemUtils.USER_DIR
        env["JAVA_HOME"] = SystemUtils.JAVA_HOME
        val report = env.entries.joinToString("\n") {
            String.format("%20s: %s", it.key, it.value)
        }
        logger.info(report)
    }

    /**
     * Tomcat enables the sandbox mode, write permissions are granted for only specified directories.
     * */
    private fun detectDatabasePathOnTomcat() {
        val h2Path = listOf(
            "/var/lib/tomcat9/work/Catalina/h2",
            "/opt/exotic",
            "/mnt/exotic",
            "/tmp",
            "~",
        )
            .map { Paths.get("$it/exotic-h2.mv.db") }
            .firstOrNull { Files.isWritable(it) }

        logger.info("H2database path: $h2Path")
        if (h2Path != null) {
            val name = h2Path.toAbsolutePath().toString().removeSuffix(".mv.db")
            System.setProperty("spring.datasource.url", "jdbc:h2:file:$name")
        }
    }
}
