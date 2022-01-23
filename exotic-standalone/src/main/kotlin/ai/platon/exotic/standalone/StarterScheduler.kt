package ai.platon.exotic.standalone

import ai.platon.pulsar.common.DateTimes.MILLIS_OF_DAY
import ai.platon.pulsar.common.DateTimes.MILLIS_OF_SECOND
import ai.platon.pulsar.common.ProcessLauncher
import ai.platon.pulsar.common.Runtimes
import ai.platon.pulsar.common.browser.Browsers
import org.apache.commons.lang3.SystemUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.awt.Desktop
import java.net.URI

@Component
@EnableScheduling
class StarterScheduler {
    @Value("\${server.port}")
    private val serverPort: Int = 0

    @Value("\${server.servlet.context-path}")
    private val serverContextPath: String = "/extoic"

    @Scheduled(initialDelay = 20 * MILLIS_OF_SECOND, fixedDelay = 1000 * MILLIS_OF_DAY)
    fun openWebConsole() {
        val chromeBinary = Browsers.searchChromeBinary()
        val contextPath = serverContextPath.removePrefix("/")
        val url = "http://localhost:$serverPort/$contextPath/crawl/rules/"
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().browse(URI.create(url))
        } else if (SystemUtils.IS_OS_LINUX) {
            Runtimes.exec("$chromeBinary $url")
        } else {
            ProcessLauncher.launch("$chromeBinary", listOf(url))
        }
    }
}
