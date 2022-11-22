package ai.platon.exotic.examples.sites

import ai.platon.exotic.examples.sites.jd.JdCrawler
import ai.platon.exotic.examples.sites.walmart.WalmartCrawler
import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.options.LoadOptions

/**
 * java -jar exotic-OCR-examples*.jar -pc 8 -tab 10 -supervised -site walmart
 *
 * Or
 *
java -Xmx10g -Xms2G -cp exotic-OCR-examples*.jar \
-D"loader.main=ai.platon.exotic.examples.sites.CrawlLauncherKt" \
-D"loader.args=\"-site jd\"" \
org.springframework.boot.loader.PropertiesLauncher
 * */
fun main(argv: Array<String>) {
    if (argv.isEmpty()) {
        val usage = """
usage: java -jar exotic-OCR*.jar [-pc 5] [-tab 10] [-supervised|-headless] -site [jd|walmart|dianping]
        """.trimIndent()
        println(usage)
        return
    }

    var maxPrivacyContextCount = 0
    var maxActiveTabCount = 0
    var headless = false
    var supervised = false
    var site = ""

    var i = 0
    while (i < argv.size) {
        if (argv[i] == "-pc") maxPrivacyContextCount = argv[++i].toInt()
        if (argv[i] == "-tab") maxActiveTabCount = argv[++i].toInt()
        if (argv[i] == "-supervised") supervised = true
        if (argv[i] == "-headless") headless = true
        if (argv[i] == "-site") site = argv[++i]

        ++i
    }

    if (maxPrivacyContextCount > 0) {
        System.setProperty(CapabilityTypes.PRIVACY_CONTEXT_NUMBER, maxPrivacyContextCount.toString())
    }
    if (maxActiveTabCount > 0) {
        System.setProperty(CapabilityTypes.BROWSER_MAX_ACTIVE_TABS, maxActiveTabCount.toString())
    }

    System.setProperty(CapabilityTypes.METRICS_ENABLED, "true")

    if (supervised) {
        BrowserSettings.supervised()
    } else if (headless) {
        BrowserSettings.headless()
    }

    val args = LoadOptions.normalize(argv.joinToString())
    when (site) {
        "jd" -> JdCrawler().runDefault(args)
        "walmart" -> WalmartCrawler().runDefault(args)
        else -> println("No site chose")
    }
}
