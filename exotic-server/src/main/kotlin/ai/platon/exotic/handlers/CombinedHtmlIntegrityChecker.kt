package ai.platon.exotic.handlers

import ai.platon.pulsar.common.HtmlIntegrity
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.persist.PageDatum
import ai.platon.pulsar.protocol.browser.emulator.HtmlIntegrityChecker

class CombinedHtmlIntegrityChecker(
    immutableConfig: ImmutableConfig
): HtmlIntegrityChecker {
    private val amazonChecker = AmazonHtmlIntegrityChecker(immutableConfig)
    private val jdChecker = JdHtmlIntegrityChecker(immutableConfig)

    override fun invoke(pageSource: String, pageDatum: PageDatum): HtmlIntegrity {
        return when {
            amazonChecker.isRelevant(pageDatum.url) -> amazonChecker.invoke(pageSource, pageDatum)
            jdChecker.isRelevant(pageDatum.url) -> jdChecker.invoke(pageSource, pageDatum)
            else -> HtmlIntegrity.OK
        }
    }
}
