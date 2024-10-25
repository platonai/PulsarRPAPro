package ai.platon.exotic.standalone.api

import ai.platon.exotic.common.ExoticUtils
import ai.platon.exotic.standalone.common.UberJars
import ai.platon.pulsar.common.DateTimes.MILLIS_PER_DAY
import ai.platon.pulsar.common.DateTimes.MILLIS_PER_SECOND
import ai.platon.scent.common.runCatchingWarnUnexpected
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@EnableScheduling
class StandaloneScheduler {
    
    @Value("\${server.port}")
    private val serverPort: Int = 0

    @Value("\${server.servlet.context-path}")
    private val serverContextPath: String = "/exotic"

    @Scheduled(initialDelay = 10 * MILLIS_PER_SECOND, fixedDelay = 1000 * MILLIS_PER_DAY)
    fun openWebConsole() {
        val contextPath = serverContextPath.removePrefix("/")
        val url = "http://localhost:$serverPort/$contextPath/crawl/rules/"

        ExoticUtils.openBrowser(url)
    }
    
    @Scheduled(initialDelay = 10 * MILLIS_PER_SECOND, fixedDelay = 10 * MILLIS_PER_SECOND)
    fun monitorAgents() {
        runCatchingWarnUnexpected { UberJars.monitor() }
    }
}
