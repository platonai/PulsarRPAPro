package ai.platon.exotic.services.api.entity.api

import ai.platon.exotic.services.common.jackson.prettyScentObjectWritter
import ai.platon.pulsar.common.ResourceStatus
import ai.platon.pulsar.driver.CompactedScrapeResponse
import org.apache.commons.lang3.StringUtils
import org.bson.types.ObjectId
import java.text.NumberFormat
import java.time.Instant

/**
 * TODO: rename to ScrapeTaskView
 * */
class ExpandedScrapeResponse(
    val response: CompactedScrapeResponse
) {
    val id = response.id
    val timestamp = if (id != null) ObjectId(id).timestamp.toLong() else 0L
    val objectTime = if (timestamp > 0) Instant.ofEpochSecond(timestamp) else Instant.EPOCH
    val abbreviatedUrl = StringUtils.abbreviateMiddle(response.url, "...", 45)
        .removePrefix("http://")
        .removePrefix("https://")
        .removePrefix("www.")
    val status: String get() {
        val code = when {
            response.pageStatusCode == ResourceStatus.SC_GONE -> ResourceStatus.SC_EXPECTATION_FAILED
            else -> response.statusCode
        }
        return ResourceStatus.getStatusText(code)
    }
    val pageStatus: String get() {
        return when {
            response.pageStatusCode == 1601 -> "Retrying"
            response.pageStatusCode > 1000 -> response.pageStatusCode.toString()
            else -> ResourceStatus.getStatusText(response.pageStatusCode)
        }
    }
    val contentLength: String get() {
        return NumberFormat.getIntegerInstance().format(response.pageContentBytes)
    }
    val resultSetAsJson get() = prettyScentObjectWritter().writeValueAsString(response.resultSet)
}
