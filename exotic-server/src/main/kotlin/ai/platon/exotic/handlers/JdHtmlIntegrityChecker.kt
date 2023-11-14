package ai.platon.exotic.handlers

import ai.platon.pulsar.common.HtmlIntegrity
import ai.platon.pulsar.common.HtmlUtils
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.persist.PageDatum
import ai.platon.pulsar.persist.ProtocolStatus
import ai.platon.pulsar.persist.metadata.OpenPageCategory
import ai.platon.pulsar.persist.metadata.PageCategory
import ai.platon.pulsar.protocol.browser.emulator.util.HtmlIntegrityChecker
import java.util.*

class JdHtmlIntegrityChecker(
    immutableConfig: ImmutableConfig
): HtmlIntegrityChecker {
    companion object {
        private const val SMALL_CONTENT_LIMIT = 200_000 // 200KiB
    }

    val categories = mapOf(
        "/list.html" to OpenPageCategory(PageCategory.INDEX),
        "/item.jd" to OpenPageCategory(PageCategory.DETAIL)
    )

    override fun isRelevant(url: String) = url.contains("jd.com")

    override fun invoke(pageSource: String, pageDatum: PageDatum): HtmlIntegrity {
        return checkHtmlIntegrity(pageSource, pageDatum)
    }

    /**
     * Check if the html is integral without field extraction, a further html integrity checking can be
     * applied after field extraction.
     * */
    fun checkHtmlIntegrity(
        pageSource: String,
        pageDatum: PageDatum
    ): HtmlIntegrity {
        if (!isRelevant(pageDatum.url)) {
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
                // robot check
                isRobotCheck(pageSource, pageDatum) -> HtmlIntegrity.ROBOT_CHECK
                // the page size is 2k
                isNotFound(pageSource, pageDatum) -> HtmlIntegrity.NOT_FOUND
                else -> integrity
            }
        }

        return integrity
    }

    /**
     * Redirected to the home page
     * */
    private fun isNotFound(pageSource: String, pageDatum: PageDatum): Boolean {
        val url = pageDatum.url
        val location = pageDatum.location
        return url.contains("item.jd.com") && location.startsWith("https://www.jd.com/")
    }

    /**
     * Redirected to login page
     * */
    private fun isRobotCheck(pageSource: String, pageDatum: PageDatum): Boolean {
        val url = pageDatum.url
        val location = pageDatum.location
        val isItemPage = url.contains("item.jd.com")
        if (isItemPage) {
            val protocolStatus = pageDatum.protocolStatus
            return when {
                protocolStatus.isTimeout -> true
                protocolStatus.getArgOrElse(ProtocolStatus.ARG_REASON, "") == "ERR_TIMED_OUT" -> true
                pageSource.contains("<title>京东-欢迎登录</title>") -> true
                location.contains("login.aspx") -> true
                else -> false
            }
        }

        return false
    }
}
