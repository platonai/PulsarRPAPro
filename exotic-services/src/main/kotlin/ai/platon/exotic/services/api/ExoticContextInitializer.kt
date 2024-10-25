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
        val hsqlPath = listOf(
            "/var/lib/tomcat9/work/Catalina",
            "/opt",
            "/mnt",
            "/tmp",
            "~",
        )
            .map { Paths.get("$it/.pulsar/data/exotic/hsql/services.lobs") }
            .firstOrNull { Files.isWritable(it) }

        logger.info("HSQL path: $hsqlPath")
        if (hsqlPath != null) {
            val home = hsqlPath.toAbsolutePath().toString().removeSuffix(".lobs")
            System.setProperty("spring.datasource.url", "jdbc:hsql:file:$home")
        }
    }
}
