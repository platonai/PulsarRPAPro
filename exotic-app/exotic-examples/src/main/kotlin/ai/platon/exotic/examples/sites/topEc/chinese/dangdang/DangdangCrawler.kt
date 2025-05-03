package ai.platon.exotic.examples.sites.topEc.chinese.dangdang

import ai.platon.pulsar.skeleton.context.PulsarContexts

fun main() {
    val portalUrl = "https://category.dangdang.com/cid4003844.html"
    val args = "-i 1s -ii 5d -ol a[href~=product] -ignoreFailure"

    PulsarContexts.createSession().submitForOutPages(portalUrl, args)

    PulsarContexts.await()
}
