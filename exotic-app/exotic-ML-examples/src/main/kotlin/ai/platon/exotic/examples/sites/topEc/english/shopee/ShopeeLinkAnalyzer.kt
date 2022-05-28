package ai.platon.exotic.examples.sites.topEc.english.shopee

import ai.platon.exotic.examples.common.VerboseHarvester

fun main() {
    val portalUrl = "https://shopee.sg/Computers-Peripherals-cat.11013247"

    val harvester = VerboseHarvester()
    val anchorGroups = VerboseHarvester().arrangeLinks(portalUrl)
    harvester.printAnchorGroups(anchorGroups, true)

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
