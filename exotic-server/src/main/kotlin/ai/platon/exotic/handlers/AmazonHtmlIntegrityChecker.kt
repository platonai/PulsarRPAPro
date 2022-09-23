package ai.platon.exotic.handlers

import ai.platon.pulsar.common.HtmlIntegrity
import ai.platon.pulsar.common.HtmlUtils
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.urls.sites.amazon.AmazonUrls
import ai.platon.pulsar.persist.PageDatum
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.metadata.OpenPageCategory
import ai.platon.pulsar.persist.metadata.PageCategory
import ai.platon.pulsar.protocol.browser.emulator.util.HtmlIntegrityChecker
import ai.platon.scent.common.AMAZON_ENABLE_DISTRICT_CHECK

class AmazonHtmlIntegrityChecker(
    immutableConfig: ImmutableConfig
): HtmlIntegrityChecker {
    companion object {
        private const val SMALL_CONTENT_LIMIT = 1_000_000 / 2 // 500KiB
    }

    private val logger = getLogger(this)

    private val enableAmazonDistrictCheck = immutableConfig.getBoolean(AMAZON_ENABLE_DISTRICT_CHECK, false);

    override fun isRelevant(url: String) = url.contains("amazon.com")

    fun isRelevant(page: WebPage) = isRelevant(page.url)

    val categories = mapOf(
        "/zgbs/" to OpenPageCategory(PageCategory.INDEX),
        "/most-wished-for/" to OpenPageCategory("INDEX", "IMWF"),
        "/new-releases/" to OpenPageCategory("INDEX", "INR"),
        "/movers-and-shakers/" to OpenPageCategory("INDEX", "IMAS")
    )

    override fun invoke(pageSource: String, pageDatum: PageDatum): HtmlIntegrity {
        if (!isRelevant(pageDatum.url)) {
            return HtmlIntegrity.OK
        }

        return checkHtmlIntegrity(pageSource, pageDatum)
    }

    /**
     * Check if the html is integral without field extraction, a further html integrity checking can be
     * applied after field extraction.
     * */
    private fun checkHtmlIntegrity(pageSource: String, pageDatum: PageDatum): HtmlIntegrity {
        if (!isRelevant(pageDatum.url)) {
            return HtmlIntegrity.OK
        }

        if (!AmazonUrls.isAmazon(pageDatum.url)) {
            return HtmlIntegrity.OK
        }

        val length = pageSource.length.toLong()
        var integrity = HtmlIntegrity.OK

        if (integrity.isOK && length < SMALL_CONTENT_LIMIT) {
            integrity = when {
                length == 0L -> HtmlIntegrity.EMPTY_0B
                length == 39L -> HtmlIntegrity.EMPTY_39B
                // There is nothing in <body> tag
                // Blank body can be caused by anti-spider
                HtmlUtils.isBlankBody(pageSource) -> HtmlIntegrity.BLANK_BODY
                // example: https://www.amazon.com/dp/B0BBBBB
                // the page size is 2k
                isNotFound(pageSource, pageDatum) -> HtmlIntegrity.NOT_FOUND
                // robot check
                isRobotCheck(pageSource, pageDatum) -> HtmlIntegrity.ROBOT_CHECK
                // too small
                isTooSmall(pageSource, pageDatum) -> HtmlIntegrity.TOO_SMALL
                else -> integrity
            }
        }

        if (integrity.isOK && isWrongDistrict(pageSource, pageDatum)) {
            integrity = HtmlIntegrity.WRONG_DISTRICT
        }

        return integrity
    }

    private fun isAmazonReviewPage(page: WebPage): Boolean {
        return AmazonUrls.isAmazon(page.url) && page.url.contains("/product-reviews/")
    }

    private fun isTooSmall(pageSource: String, page: PageDatum): Boolean {
        val length = pageSource.length
        return if (AmazonUrls.isAmazonItemPage(page.url)) {
            length < SMALL_CONTENT_LIMIT / 2
        } else {
            length < 1000
        }
    }

    private fun isWrongDistrict(pageSource: String, page: PageDatum): Boolean {
        if (!enableAmazonDistrictCheck) {
            return false
        }

        if (!AmazonUrls.isAmazonItemPage(page.url) && !AmazonUrls.isAmazonIndexPage(page.url)) {
            return false
        }

        var pos = pageSource.indexOf("glow-ingress-block")
        if (pos != -1) {
            pos = pageSource.indexOf("Deliver to", pos)
            if (pos != -1) {
                pos = pageSource.indexOf("New York", pos)
                if (pos == -1) {
                    // when the deliver destination is not New York, the district is wrong
                    return true
                }
            }
        }

        return false
    }

    private fun isRobotCheck(pageSource: String, page: PageDatum): Boolean {
        return pageSource.length < 150_000 && pageSource.contains("Type the characters you see in this image")
    }

    private fun isNotFound(pageSource: String, page: PageDatum): Boolean {
        return pageSource.length < 150_000 && pageSource.contains("Sorry! We couldn't find that page")
    }
}
