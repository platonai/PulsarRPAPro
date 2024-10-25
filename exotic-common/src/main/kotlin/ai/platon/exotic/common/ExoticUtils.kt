package ai.platon.exotic.common

import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.ProcessLauncher
import ai.platon.pulsar.common.ResourceLoader
import ai.platon.pulsar.common.browser.Browsers
import org.apache.commons.lang3.SystemUtils
import java.nio.file.Files
import java.nio.file.Paths
import java.text.DecimalFormat

object ExoticUtils {
    
    private val FEATURE_FORMATTER = DecimalFormat("#.####")
    
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

    fun prepareDatabaseOrFail() {
        kotlin.runCatching { prepareDatabase() }.onFailure { System.err.println(it.message) }
    }

    fun prepareDatabase() {
        // use hsql now
//        val dbPath = AppPaths.SYS_USER_HOME.resolve("exotic-h2.mv.db")
//        if (!Files.exists(dbPath)) {
//            val inStream = ResourceLoader.getResourceAsStream("db/exotic-h2.mv.db.data")
//            if (inStream != null) {
//                Files.copy(inStream, dbPath)
//            }
//        }
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
    
    /**
     * Encode to libsvm record.
     *
     * @param features the feature vector
     * @param label    the label, negative value means not specified
     */
    fun encodeToLibSVMRecord(features: DoubleArray, label: Int): StringBuilder {
        val space = ' '
        val record = StringBuilder()
        record.append(label).append(space)
        for (i in features.indices) {
            val value = features[i]
            if (value != 0.0) {
                record.append(i).append(':').append(FEATURE_FORMATTER.format(value))
                record.append(space)
            }
        }
        val i = record.length - 1
        if (record[i] == space) {
            record.deleteCharAt(i)
        }
        return record
    }
}
