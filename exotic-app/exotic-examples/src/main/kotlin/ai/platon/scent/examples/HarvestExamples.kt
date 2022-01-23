package ai.platon.scent.examples

import ai.platon.scent.ScentContext
import ai.platon.scent.context.withContext
import ai.platon.scent.dom.HNormUrl
import ai.platon.scent.dom.nodes.annotateNodes
import ai.platon.scent.examples.common.WebHarvester

class HarvestExamples(context: ScentContext): WebHarvester(context) {

    val seeds = listOf(
            "https://list.suning.com/0-20006-0-0-0-0-0-0-0-0-11635.html -expires 1s -ol \".product-box a[href~=product]\"",
            "http://dzhcg.sinopr.org/channel/301",
            "https://list.gome.com.cn/cat10000070-00-0-48-1-0-0-0-1-0-0-1-0-0-0-0-0-0.html?intcmp=phone-163",
            "http://category.dangdang.com/cid4002590.html -tp 140 -i 1h -scrollCount 20 -ii 1d -ol a[href~=product]",
            "https://www.proya.com/product_query-xId-583.html -i 1d -tl 40 -ol \"a[href~=product_detail]\" -ii 7d -c \".productInfo .conn\"",
            "https://www.esteelauder.com.cn/products/14731/product-catalog -i 1s -ii 7d -ol a[href~=product]",
            "https://www.darphin.com/collections/essential-oil-elixir",
            "https://search.jd.com/Search?keyword=basketball&enc=utf-8&wq=basketball&pvid=27d8a05385cd49298b5caff778e14b97"
    ).filter { it.isNotBlank() }

    fun arrangeLinks() {
        listOf(seeds, testedSeeds).flatten().filter { it.isNotBlank() }.forEach { url ->
            log.info("Arranging links in page $url")
            val normUrl = HNormUrl.parse(url, i.sessionConfig.toVolatileConfig())
            val doc = i.load(url).let { i.parse(it) }
            i.arrangeLinks(normUrl, doc)
            doc.also { it.annotateNodes(normUrl.hOptions) }.also { i.export(it) }
        }
    }

    fun arrangeDocuments() {
        listOf(seeds).flatten().filter { it.isNotBlank() }.forEach { url ->
            arrangeDocument(url)
        }
    }

    fun harvest() {
        val url = seeds[0]
        harvest(url)
    }

    fun harvestAll() {
        seeds.forEach { harvest(it) }
    }
}

fun main() = withContext { HarvestExamples(it).harvestAll() }
