package ai.platon.exotic.driver.crawl.scraper

import ai.platon.exotic.driver.common.DEV_MAX_OUT_PAGES
import ai.platon.exotic.driver.common.PRODUCT_MAX_OUT_PAGES
import ai.platon.exotic.driver.common.IS_DEVELOPMENT
import ai.platon.exotic.driver.crawl.entity.CrawlRule
import ai.platon.exotic.driver.crawl.entity.PortalTask
import ai.platon.pulsar.common.urls.UrlUtils
import ai.platon.pulsar.driver.DriverSettings
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration

open class OutPageScraper(
    val driverSettings: DriverSettings
): AutoCloseable {
    var logger: Logger = LoggerFactory.getLogger(OutPageScraper::class.java)

    val httpTimeout: Duration = Duration.ofMinutes(3)

    val taskSubmitter: TaskSubmitter = TaskSubmitter(driverSettings)

    fun scrape(sql: String) {
        val a = """
            select
                dom_uri(dom) as uri,
                dom_first_text(dom, '.itemInfo-wrap .sku-name') as product_title
            from
                load_out_pages(
                    'https://list.jd.com/list.html?cat=670,671,672&page=1 -taskId r2 -refresh',
                    '#J_goodsList li[data-sku] a[href~=item]'
                )
        """.trimIndent()

        val (prefix, part2) = sql.split("load_out_pages")
        val (portalUrl, outLinkSelector) = part2
            .substringAfterLast("(")
            .substringBeforeLast(")")
            .split(",")

        val portalSQLTemplate = """
            select
                dom_all_hrefs(dom, '$outLinkSelector') as hrefs
            from
                load_and_select('{{url}}', 'body');
        """.trimIndent()
        val itemSQLTemplate = "$prefix load_and_select('{{url}}', 'body')"
    }

    fun scrape(task: ListenableScrapeTask) {
        taskSubmitter.scrape(task)
    }

    fun scrape(listenablePortalTask: ListenablePortalTask) {
        val task = listenablePortalTask.task
        val rule = task.rule
        if (rule == null) {
            logger.info("No rule for task {}", task.id)
            return
        }

        val outLinkSelector = rule.outLinkSelector
        if (outLinkSelector == null) {
            logger.info("No out link selector for task {}", task.id)
            return
        }

        val args = buildPortalArgs(rule, listenablePortalTask.refresh)
        val priority = task.priority

        val portalSQLTemplate = """
            select
                dom_all_hrefs(dom, '$outLinkSelector') as hrefs
            from
                load_and_select('{{url}}', 'body');
        """.trimIndent()
        val scrapeTask = ScrapeTask(task.url, args, priority, portalSQLTemplate)
        scrapeTask.companionPortalTask = task

        val listenableScrapeTask = ListenableScrapeTask(scrapeTask).also {
            it.task.companionPortalTask = task
            it.onSubmitted = { listenablePortalTask.onSubmitted(it.task) }
            it.onRetry = { listenablePortalTask.onRetry(it.task) }
            it.onSuccess = {
                listenablePortalTask.onSuccess(it.task)
                createChildTasks(listenablePortalTask, it)
            }
            it.onFailed = { listenablePortalTask.onFailed(it.task) }
            it.onFinished = { listenablePortalTask.onFinished(it.task) }
            it.onTimeout = { listenablePortalTask.onTimeout(it.task) }
        }

        taskSubmitter.scrape(listenableScrapeTask)
    }

    override fun close() {
        taskSubmitter.close()
    }

    private fun createChildTasks(
        listenablePortalTask: ListenablePortalTask,
        scrapeTask: ListenableScrapeTask
    ) {
        val portalTask = scrapeTask.task.companionPortalTask ?: return
        val rule = portalTask.rule ?: return

        val sqlTemplate = rule.sqlTemplate?.trim()
        if (sqlTemplate.isNullOrBlank()) {
            logger.warn("No SQL template in rule {}", rule.id)
            return
        }

        val urls = createOutLinks(portalTask, scrapeTask)
        val args = buildItemArgs(rule, portalTask.args.contains("-refresh"))
        val tasks = createChildTasks(listenablePortalTask, urls, sqlTemplate, args)

        taskSubmitter.scrapeAll(tasks)
    }

    private fun createOutLinks(portalTask: PortalTask, scrapeTask: ListenableScrapeTask): List<String> {
        val resultSet = scrapeTask.task.response.resultSet
        if (resultSet == null || resultSet.isEmpty()) {
            logger.info("No result set | {}", scrapeTask.task.configuredUrl)
            return listOf()
        }

        val outLinkSelector = portalTask.rule?.outLinkSelector
        var hrefs = resultSet[0]["hrefs"]?.toString()
        if (hrefs.isNullOrBlank()) {
            logger.info("No hrefs in task #{} | {}", portalTask.id, scrapeTask.task.configuredUrl)
            return listOf()
        }

        val maxOutPages = if (IS_DEVELOPMENT) DEV_MAX_OUT_PAGES else PRODUCT_MAX_OUT_PAGES
        hrefs = hrefs.removePrefix("(").removeSuffix(")")

        // TODO: normalization
        val urls = hrefs.split(",").asSequence()
            .filter { UrlUtils.isValidUrl(it) }
            .map { it.substringBeforeLast("#") }
            .map { it.trim() }
            .take(maxOutPages)
            .toList()

        if (urls.isEmpty()) {
            logger.info("No out links in task #{} | <{}> | {}",
                portalTask.id, outLinkSelector, scrapeTask.task.configuredUrl)
        }

        return urls
    }

    private fun createChildTasks(
        listenablePortalTask: ListenablePortalTask,
        urls: List<String>,
        sqlTemplate: String,
        args: String
    ): List<ListenableScrapeTask> {
        val priority = listenablePortalTask.task.priority
        val tasks = urls.map { ScrapeTask(it, args, priority, sqlTemplate) }
            .map { ListenableScrapeTask(it) }
            .onEach {
                it.onSubmitted = { listenablePortalTask.onItemSubmitted(it.task) }
                it.onRetry = { listenablePortalTask.onItemRetry(it.task) }
                it.onSuccess = { listenablePortalTask.onItemSuccess(it.task) }
                it.onFailed = { listenablePortalTask.onItemFailed(it.task) }
                it.onFinished = { listenablePortalTask.onItemFinished(it.task) }
                it.onTimeout = { listenablePortalTask.onItemTimeout(it.task) }
            }

        return tasks
    }

    private fun buildPortalArgs(rule: CrawlRule, refresh: Boolean): String {
        var args = rule.buildArgs()
        args += if (refresh) " -refresh" else ""
        args += " -authToken " + driverSettings.authToken
        return args
    }

    private fun buildItemArgs(rule: CrawlRule, portalRefresh: Boolean): String {
        var args = rule.buildArgs() + " -scrollCount 20"
        args += if (portalRefresh) " -expires 2h" else " -expires 3600d"
        args += " -authToken " + driverSettings.authToken
        return args
    }
}
