package ai.platon.exotic.examples.ml.unsupervised.topEc.english.amazon

import ai.platon.exotic.crawl.common.LinkAnalyzer
import ai.platon.exotic.crawl.common.VerboseHarvester

fun main() {
    val portalUrl = "https://www.amazon.com/Best-Sellers/zgbs"
    
    val analyzer = LinkAnalyzer()
    val anchorGroups = analyzer.arrangeLinks(portalUrl)
    analyzer.printAnchorGroups(anchorGroups, true)
}
