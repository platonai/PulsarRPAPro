package ai.platon.exotic.examples.sites.topEc.english.amazon

import ai.platon.pulsar.ql.context.SQLContexts

fun main() {
    val portalUrl = "https://www.amazon.com/Best-Sellers/zgbs"
    val args = "-i 1d -ii 7d -ol a[href~=/dp/] -ignoreFailure"

    val fields = SQLContexts.createSession().scrapeOutPages(portalUrl, args, ":root", listOf("title"))
    println(fields.joinToString("\n"))
}
