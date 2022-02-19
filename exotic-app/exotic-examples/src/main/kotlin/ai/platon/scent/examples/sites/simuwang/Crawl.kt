package ai.platon.scent.examples.sites.simuwang

import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.crawl.AbstractJsEventHandler
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.persist.WebPage
import ai.platon.scent.examples.common.VerboseCrawler
import ai.platon.scent.ql.h2.context.withSQLContext

class LoginJsEventHandler: AbstractJsEventHandler() {
    override var verbose = true

    override suspend fun onAfterComputeFeature(page: WebPage, driver: WebDriver): Any? {
        val expressions = """
            let message = "Start choose district";
            document.querySelector(".comp-login input.comp-login-input").value = '18621538660';
            document.querySelector(".comp-login input#GLUXZipUpdateInput").value = 'Nichang2';
            document.querySelector(".comp-login button#comp-login-btn").click();
        """.trimIndent()

        return evaluate(driver, expressions.split(";"))
    }
}

fun main() {
    val seed = "https://ly.simuwang.com/"
    val args = "-i 1s -ii 10d -ol a[href~=roadshow] -tl 100"

    withSQLContext {
        System.setProperty(CapabilityTypes.BROWSER_LAUNCH_NO_SANDBOX, "false")
        val crawler = VerboseCrawler(it)
        crawler.load(seed, args, LoginJsEventHandler())
        crawler.loadOutPages(seed, args)
    }
}
