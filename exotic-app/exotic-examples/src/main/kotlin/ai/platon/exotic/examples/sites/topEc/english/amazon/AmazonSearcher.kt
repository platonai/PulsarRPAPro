package ai.platon.exotic.examples.sites.topEc.english.amazon

import ai.platon.pulsar.context.PulsarContexts
import ai.platon.pulsar.crawl.AbstractWebPageWebDriverHandler
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.dom.Documents
import ai.platon.pulsar.persist.WebPage

class AmazonSearcherJsEventHandler: AbstractWebPageWebDriverHandler() {
    override var verbose: Boolean = true

    override suspend fun invokeDeferred(page: WebPage, driver: WebDriver): Any? {
        val selector = "input#twotabsearchtextbox"
        val expressions = "document.querySelector('$selector').value = 'cup';" +
                "document.querySelector('$selector').click();" +
                "document.querySelector('$selector').focus({preventScroll: true});" +
                "let a = 1+1;" +
                "var b = 1+2;" +
                "let c = 1+3;"

        evaluate(driver, expressions.split(";"))

        val expression = "document.querySelector('#suggestions').outerHTML;"
        val value = evaluate(driver, expression)

        if (value is String && value.contains("<div")) {
            val doc = Documents.parseBodyFragment(value)
            val suggestions = doc.select(".s-suggestion")
            suggestions.forEach {
                println("................................")
                println("alias: " + it.attr("data-alias"))
                println("keyword: " + it.attr("data-keyword"))
                println("isfb: " + it.attr("data-isfb"))
                println("crid: " + it.attr("data-crid"))
            }
        }

        return value
    }
}

fun main() {
    val portalUrl = "https://www.amazon.com/"

    val cx = PulsarContexts.create()
    val i = cx.createSession()
    val opts = i.options("-i 0s")
    opts.ensureEventHandler().simulateEventHandler.onAfterComputeFeature
        .addLast(AmazonSearcherJsEventHandler())
    i.load(portalUrl, opts)

    readLine()
}
