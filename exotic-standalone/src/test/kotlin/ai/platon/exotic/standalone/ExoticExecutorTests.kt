package ai.platon.exotic.standalone

import ai.platon.pulsar.common.urls.UrlUtils
import ai.platon.scent.context.ScentContexts
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ExoticExecutorTests {

    val session = ScentContexts.createSession()

    @Test
    fun testComponentOptions() {
        val args1 = "-componentSelectors #abc -componentSelectors #def"
        val args2 = "-component #abc -component #def"
        val options = session.options(args2)
//        println(options)
        assertEquals(args1, options.toString())
    }

    @Test
    fun testParseHarvestCmdLine() {
        val cmdLine = "-headless harvest https://www.amazon.com/Best-Sellers/zgbs"
        val executor = ExoticExecutor(cmdLine)
        executor.parseCmdLine()
        assertTrue(executor.harvest)
        assertTrue(executor.headless)
        assertFalse(executor.scrape)
        assertTrue(executor.scrapeFields.isEmpty())
    }

    @Test
    fun testParseHarvestCmdLineWithComponentOptions() {
        val args = "-component #centerCol -component #buybox"
        val cmdLine = "-headless harvest https://www.amazon.com/Best-Sellers/zgbs $args"
        val executor = ExoticExecutor(cmdLine)
        executor.parseCmdLine()
        assertTrue(executor.harvest)
        val (_, args1) = UrlUtils.splitUrlArgs(executor.configuredUrl)
        assertEquals(args, args1)
        val options = session.options(args1)
        assertTrue(options.componentSelectors.contains("#centerCol"))
    }

    @Test
    fun testParseScrapeCmdLine() {
        val cmdLine = "-headless scrape https://www.amazon.com/Best-Sellers/zgbs -outLink a[href~=/dp/] -field h2 -field .price"
        val executor = ExoticExecutor(cmdLine)
        executor.parseCmdLine()
        assertFalse(executor.harvest)
        assertTrue(executor.headless)
        assertTrue(executor.scrape)
        assertTrue(executor.scrapeFields.isNotEmpty())
        assertTrue(executor.scrapeFields.contains("h2"))
        assertTrue(executor.scrapeFields.contains(".price"))
    }

    @Test
    fun testScraping() {
        val cmdLine = "scrape https://www.amazon.com/dp/B09V3KXJPB -field #productTitle" +
                " -field #acrPopover -field #acrCustomerReviewText -field #askATFLink"
        val executor = ExoticExecutor(cmdLine)
        executor.parseCmdLine()
        val result = executor.scrape()
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun testScrapeOutPages() {
        val cmdLine = "scrape https://www.amazon.com/dp/B09V3KXJPB " +
                " -outLink a[href~=/dp/]" +
                " -field #productTitle" +
                " -field #acrPopover -field #acrCustomerReviewText -field #askATFLink"
        val executor = ExoticExecutor(cmdLine)
        executor.mute()
        executor.parseCmdLine()
        val result = executor.scrape()
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun testScrapeHelp() {
        val executor = ExoticExecutor("--help scrape")
        executor.mute()
        executor.execute()
        assertTrue(executor.lastHelpMessage?.contains("-itemExpires") == true)
    }

    @Test
    fun testHarvestHelp() {
        val executor = ExoticExecutor("--help harvest -verbose")
//        executor.mute()
        executor.execute()
        assertTrue(executor.lastHelpMessage?.contains("-trustSamples") == true)
        assertTrue(executor.lastHelpMessage?.contains("-componentSelectors") == true)
    }

    @Test
    fun testSQLHelp() {
        val executor = ExoticExecutor("--help SQL")
        executor.mute()
        executor.execute()
        assertTrue(executor.lastHelpMessage?.contains("DOM_SLIM_HTML") == true)
    }

    @Test
    fun testSQLHelpVerbose() {
        val executor = ExoticExecutor("--help SQL -verbose")
        executor.mute()
        executor.execute()
        assertTrue(executor.lastHelpMessage?.contains("DOM_SLIM_HTML") == true)
        assertTrue(executor.lastHelpMessage?.contains("DomFunctions") == true)
    }
}
