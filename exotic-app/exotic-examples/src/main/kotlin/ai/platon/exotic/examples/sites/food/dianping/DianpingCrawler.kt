package ai.platon.exotic.examples.sites.food.dianping

import ai.platon.pulsar.context.PulsarContexts
import com.google.gson.GsonBuilder

fun main() {
    val portalUrl = "https://www.dianping.com/beijing/ch10/g110"
    val args = "-i 1s -ii 5d -ol \"#shop-all-list .tit a[href~=shop]\" -ignoreFailure"

    val session = PulsarContexts.createSession()
    session.loadOutPages(portalUrl, args)

    val extractors = mapOf(
        "title" to ".basic-info h2",
        "score" to ".basic-info .brief-info .mid-score",
        "reviewCount" to "#reviewCount",
        "avgPrice" to "#avgPriceTitle",
        "commentScores" to "#comment_score",
        "address" to "#address",
        "tel" to ".tel",
    )

    val fields = session.scrapeOutPages(portalUrl, args, extractors)
    println(GsonBuilder().setPrettyPrinting().create().toJson(fields))
}
