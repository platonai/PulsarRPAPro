package ai.platon.exotic.examples

import ai.platon.pulsar.skeleton.PulsarSettings
import ai.platon.pulsar.skeleton.context.PulsarContexts

fun main() {
    PulsarSettings().disableProxy().maxBrowsers(1).withSPA().withPrototypeBrowser()
    val session = PulsarContexts.createSession()
    session.load("https://www.amazon.com/", "-refresh")
    readlnOrNull()
}
