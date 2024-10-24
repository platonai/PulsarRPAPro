package ai.platon.exotic.examples.sites.topEc.english.amazon

import ai.platon.pulsar.skeleton.context.PulsarContexts
import ai.platon.scent.ql.h2.context.ScentSQLContexts

fun main() {
    val portalUrl = "https://www.amazon.com/Best-Sellers/zgbs"
    val args = "-i 1d -ii 700d -ol a[href~=/dp/] -tl 1000 -requireSize 1000000 -ignoreFailure"

    val session = ScentSQLContexts.createSession()
//    val fields = session.scrapeOutPages(portalUrl, args, ":root", listOf("title"))
//    println(fields.joinToString("\n"))
    session.submitForOutPages(portalUrl, args)
    
    PulsarContexts.await()
}
