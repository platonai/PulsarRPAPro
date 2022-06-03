package ai.platon.exotic.services.api.entity.api

import ai.platon.exotic.services.common.jackson.prettyScentObjectWritter
import ai.platon.pulsar.driver.CompactedScrapeResponse
import org.apache.commons.lang.StringUtils
import org.bson.types.ObjectId
import java.time.Instant

class ExpandedScrapeResponse(
    val response: CompactedScrapeResponse
) {
    val id = response.id
    val timestamp = if (id != null) ObjectId(id).timestamp.toLong() else 0L
    val objectTime = if (timestamp > 0) Instant.ofEpochSecond(timestamp) else Instant.EPOCH
    val abbreviatedUrl = StringUtils.abbreviateMiddle(response.url, "...", 50)
    val resultSetAsJson get() = prettyScentObjectWritter().writeValueAsString(response.resultSet)
}
