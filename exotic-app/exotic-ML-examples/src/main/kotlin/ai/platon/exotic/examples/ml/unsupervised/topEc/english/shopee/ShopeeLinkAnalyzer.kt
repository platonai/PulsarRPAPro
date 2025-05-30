package ai.platon.exotic.examples.ml.unsupervised.topEc.english.shopee

import ai.platon.exotic.crawl.common.LinkAnalyzer

fun main() {
    val portalUrl = "https://www.amazon.com/b?node=1292115011"

    val analyzer = LinkAnalyzer()
    val anchorGroups = analyzer.arrangeLinks(portalUrl)
    analyzer.printAnchorGroups(anchorGroups, true)

    anchorGroups.forEach {
        println("======")
        println(it.path)
        println(it.dictionary)
        println(it.folds)
        it.anchorSpecs.take(5).forEach { spec ->
            println(spec.url)
        }
    }
}
