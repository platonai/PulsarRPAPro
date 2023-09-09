package ai.platon.exotic.examples

import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.context.PulsarContexts

fun main() {
    BrowserSettings.withPrototypeBrowser()
    
    val session = PulsarContexts.createSession()
    session.load("https://www.tmall.com/", "-refresh")
    readLine()
}
