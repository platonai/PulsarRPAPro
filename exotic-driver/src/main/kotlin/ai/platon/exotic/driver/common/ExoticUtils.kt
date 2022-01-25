package ai.platon.exotic.driver.common

import ai.platon.pulsar.common.ProcessLauncher
import ai.platon.pulsar.common.browser.Browsers

object ExoticUtils {
    fun openBrowser(url: String) {
        val chromeBinary = Browsers.searchChromeBinary()
        ProcessLauncher.launch("$chromeBinary", listOf(url))
    }
}
