package ai.platon.exotic.examples.sites.search

import ai.platon.pulsar.context.PulsarContexts

fun main() {
    val url = "https://www.baidu.com/"

    val session = PulsarContexts.createSession()
    session.open(url)

    readLine()
}