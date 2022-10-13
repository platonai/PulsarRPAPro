package ai.platon.exotic.examples.sites.topEc.english.walmart

import ai.platon.pulsar.common.HtmlIntegrity
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.context.PulsarContexts
import ai.platon.pulsar.persist.PageDatum
import ai.platon.pulsar.protocol.browser.emulator.BrowserResponseEvents
import ai.platon.pulsar.protocol.browser.emulator.BrowserResponseHandler
import ai.platon.pulsar.protocol.browser.emulator.util.HtmlIntegrityChecker
import kotlinx.coroutines.delay
import kotlin.random.Random

class WalmartHtmlChecker: HtmlIntegrityChecker {
    override fun isRelevant(url: String): Boolean {
        return true
    }

    // Since we need to check the html integrity of the page, we need active dom urls,
    // which is calculated in javascript.
    override fun invoke(pageSource: String, pageDatum: PageDatum): HtmlIntegrity {
        val url = pageDatum.activeDOMUrls?.location ?: pageDatum.url
        // Authorization verification
        return when {
            "verify" in url -> HtmlIntegrity.ROBOT_CHECK_3
            "403 Forbidden" in pageSource -> HtmlIntegrity.FORBIDDEN
            else -> HtmlIntegrity.OK
        }
    }
}

fun main() {
    System.setProperty(CapabilityTypes.PRIVACY_CONTEXT_NUMBER, "3")
    System.setProperty(CapabilityTypes.BROWSER_MAX_ACTIVE_TABS, "5")

//    BrowserSettings.headless()
//    BrowserSettings.withWorseNetwork()

    val portalUrls = """
https://www.walmart.com/browse/cell-phones/apple-iphone/1105910_7551331_1127173?povid=web_globalnav_cellphones_iphone
    """.trimIndent().split("\n")
    val args = "-i 1s -ii 1s -ol a[href~=/ip/] -ignoreFailure"

    val contextLocation = "classpath:pulsar-beans/app-context.xml"
    val session = PulsarContexts.create(contextLocation).createSession()
    val responseHandler = session.context.getBean(BrowserResponseHandler::class)
    responseHandler.emit(BrowserResponseEvents.initHTMLIntegrityChecker, WalmartHtmlChecker())

    val options = session.options(args)
    val seh = options.itemEvent.browseEvent
    seh.onWillFetch.addLast { page, driver ->
        // delay(1_000L + Random.nextInt(20_000))
    }
    seh.onWillCheckDocumentState.addLast { page, driver ->
        delay(10_000L + Random.nextInt(20_000))
    }

    portalUrls.forEach {
        session.loadOutPages(it, options)
    }
}
