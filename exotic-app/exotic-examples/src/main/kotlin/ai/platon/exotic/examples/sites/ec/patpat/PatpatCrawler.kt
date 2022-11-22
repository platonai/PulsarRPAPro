package ai.platon.exotic.examples.sites.ec.patpat

import ai.platon.pulsar.context.PulsarContexts

fun main() {
    val portalUrl = "https://us.patpat.com/category/Baby.html"
    val args = """-i 1s -ii 30d -outLink a[href~=product] -ignoreFailure"""
    PulsarContexts.createSession().loadOutPages(portalUrl, args)
}
