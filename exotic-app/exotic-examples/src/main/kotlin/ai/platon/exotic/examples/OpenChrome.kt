package ai.platon.exotic.examples

import ai.platon.pulsar.context.PulsarContexts

fun main() {
    val session = PulsarContexts.createSession()
    session.load("https://www.tmall.com/", "-refresh")
    readLine()
}
