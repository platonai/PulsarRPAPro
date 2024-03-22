package ai.platon.exotic.examples.sites.social

import ai.platon.pulsar.context.PulsarContexts

class Twitter {
    private val session = PulsarContexts.createSession()
    private val home = "https://twitter.com/"
    
    fun visit() {
        session.open(home)
    }
}

fun main() {
    val twitter = Twitter()
    twitter.visit()
    
    readlnOrNull()
}
