package ai.platon.exotic.crawl.common

import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.ProcessLauncher
import ai.platon.pulsar.common.ResourceLoader
import ai.platon.pulsar.common.browser.Browsers
import java.nio.file.Files

object ExoticUtils {
    fun formatTime(s: String, time: Long): String {
        return if (time == 0L) "" else time.toString() + " " + s + if (time == 1L) "" else "s"
    }

    fun formatDuration(seconds: Long): String {
        var readableDuration = formatLongDuration(seconds)
        if (readableDuration.contains(",")) {
            readableDuration = formatShortDuration(seconds)
        }
        return readableDuration
    }

    fun formatLongDuration(seconds: Long): String {
        return if (seconds == 0L) "now" else listOf(
            formatTime("year", seconds / 31536000),
            formatTime("day", seconds / 86400 % 365),
            formatTime("hour", seconds / 3600 % 24),
            formatTime("min", seconds / 60 % 60),
            formatTime("second", seconds % 3600 % 60)
        ).filter { it !== "" }.joinToString().replace(", (?!.+,)".toRegex(), " and ")
    }

    fun formatShortDuration(seconds: Long): String {
        return if (seconds == 0L) "now" else listOf(
            formatTime("y", seconds / 31536000),
            formatTime("d", seconds / 86400 % 365),
            formatTime("h", seconds / 3600 % 24),
            formatTime("m", seconds / 60 % 60),
            formatTime("s", seconds % 3600 % 60)
        ).filter { e -> e !== "" }.joinToString().replace(", (?!.+,)".toRegex(), " and ")
    }

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
