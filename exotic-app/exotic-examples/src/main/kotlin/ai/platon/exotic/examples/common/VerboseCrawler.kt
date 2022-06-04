package ai.platon.exotic.examples.common

import ai.platon.pulsar.common.NetUtil
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.common.urls.UrlUtils
import ai.platon.pulsar.context.PulsarContext
import ai.platon.pulsar.context.PulsarContexts
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.session.PulsarSession
import org.slf4j.LoggerFactory
import java.net.URL

open class VerboseCrawler(
    val context: PulsarContext
) {
    private val logger = LoggerFactory.getLogger(VerboseCrawler::class.java)

    open val session: PulsarSession = PulsarContexts.createSession()

    fun load(url: String, args: String) {
        val options = session.options(args)
        load(url, options)
    }

    fun load(url: String, options: LoadOptions) {
        val page = session.load(url, options)

        val doc = session.parse(page)
        doc.absoluteLinks()
        doc.stripScripts()

        doc.select(options.outLinkSelector) { it.attr("abs:href") }.asSequence()
            .filter { UrlUtils.isValidUrl(it) }
            .mapTo(HashSet()) { it.substringBefore(".com") }
            .asSequence()
            .filter { it.isNotBlank() }
            .mapTo(HashSet()) { "$it.com" }
            .filter { NetUtil.testHttpNetwork(URL(it)) }
            .take(10)
            .joinToString("\n") { it }
            .also { println(it) }

        val path = session.export(doc)
        logger.info("Export to: file://{}", path)
    }

    fun loadOutPages(portalUrl: String, args: String): Collection<WebPage> {
        return loadOutPages(portalUrl, LoadOptions.parse(args, session.sessionConfig))
    }

    fun loadOutPages(portalUrl: String, options: LoadOptions): Collection<WebPage> {
        val page = session.load(portalUrl, options)

//        val page = session.load(portalUrl, options)
        if (!page.protocolStatus.isSuccess) {
            logger.warn("Failed to load page | {}", portalUrl)
        }

        val document = session.parse(page)
        document.absoluteLinks()
        document.stripScripts()
        val path = session.export(document)
        logger.info("Portal page is exported to: file://$path")

        val links = document.select(options.outLinkSelector) { it.attr("abs:href") }
            .mapTo(mutableSetOf()) { session.normalize(it, options) }
            .take(options.topLinks).map { it.spec }
        logger.info("Total {} items to load", links.size)

        val itemOptions = options.createItemOptions().apply { parse = true }

        return session.loadAll(links, itemOptions)
    }
}
