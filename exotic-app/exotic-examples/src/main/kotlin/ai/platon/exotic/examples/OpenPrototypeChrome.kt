package ai.platon.exotic.examples

import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.context.PulsarContexts

fun main() {
    BrowserSettings.disableProxy().privacy(1).withSPA().withPrototypeBrowser()
    val session = PulsarContexts.createSession()
    session.load("https://www.amazon.com/", "-refresh")
    readlnOrNull()
}
