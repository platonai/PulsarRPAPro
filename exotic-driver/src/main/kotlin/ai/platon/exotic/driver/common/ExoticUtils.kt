package ai.platon.exotic.driver.common

import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.ProcessLauncher
import ai.platon.pulsar.common.browser.Browsers

object ExoticUtils {

    fun openBrowser(url: String) {
        val chromeBinary = Browsers.searchChromeBinary()
        val dataDir = AppPaths.getTmp("exotic-chrome")
        val args = listOf(url,
            "--user-data-dir=$dataDir",
            "--no-first-run",
            "--no-default-browser-check"
        )
        ProcessLauncher.launch("$chromeBinary", args)
    }
}
