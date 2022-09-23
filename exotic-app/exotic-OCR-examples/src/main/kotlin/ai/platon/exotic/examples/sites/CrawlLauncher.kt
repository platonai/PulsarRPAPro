package ai.platon.exotic.examples.sites

import ai.platon.exotic.examples.sites.food.dianping.DianpingCrawler
import ai.platon.exotic.examples.sites.jd.JdCrawler
import ai.platon.exotic.examples.sites.walmart.WalmartCrawler
import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.common.config.CapabilityTypes

/**
java -Xmx10g -Xms2G -cp exotic-OCR-examples*.jar \
-D"loader.main=ai.platon.exotic.examples.sites.CrawlLauncherKt" \
-D"loader.args=\"-site jd\"" \
org.springframework.boot.loader.PropertiesLauncher
 * */
fun main(args: Array<String>) {
    var maxPrivacyContextCount = 0
    var maxActiveTabCount = 0
    var headless = false
    var supervised = false
    var site = ""

    var i = 0
    while (i < args.size) {
        if (args[i] == "-pc") maxPrivacyContextCount = args[++i].toInt()
        if (args[i] == "-tab") maxActiveTabCount = args[++i].toInt()
        if (args[i] == "-supervised") supervised = true
        if (args[i] == "-headless") headless = true
        if (args[i] == "-site") site = args[++i]

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

    when (site) {
        "jd" -> JdCrawler().runDefault()
        "walmart" -> WalmartCrawler().runDefault()
        "dianping" -> DianpingCrawler().runDefault()
        else -> println("No site chose")
    }

    println("All done.")
}
