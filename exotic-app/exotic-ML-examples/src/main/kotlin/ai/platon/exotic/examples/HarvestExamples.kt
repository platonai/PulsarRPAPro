package ai.platon.exotic.examples

import ai.platon.exotic.driver.common.ExoticUtils
import ai.platon.exotic.examples.common.VerboseHarvester
import ai.platon.pulsar.common.AppPaths
import ai.platon.scent.ScentContext
import ai.platon.scent.context.withContext
import ai.platon.scent.dom.HNormUrl
import ai.platon.scent.dom.nodes.annotateNodes
import ai.platon.scent.ql.h2.context.ScentSQLContexts
import org.slf4j.LoggerFactory

class HarvestExamples(
    context: ScentContext = ScentSQLContexts.create()
): VerboseHarvester(context) {

    private val logger = LoggerFactory.getLogger(HarvestExamples::class.java)

    val testedSeeds = listOf(
        /////////////////////////////////////////////////////////
        // The sites below are well tested

        "http://mall.goumin.com/mall/list/219",
        "https://www.hua.com/gifts/chocolates/",
        "http://category.dangdang.com/cid4002590.html",
        "https://list.mogujie.com/book/magic/51894",
        "https://list.jd.com/list.html?cat=6728,6742,13246",
        "https://list.gome.com.cn/cat10000055-00-0-48-1-0-0-0-1-2h8q-0-0-10-0-0-0-0-0.html?intcmp=bx-1000078331-1",
        "https://search.yhd.com/c0-0/k电视/",
        "https://www.amazon.cn/b/ref=sa_menu_Accessories_l3_b888650051?ie=UTF8&node=888650051",
        "https://category.vip.com/search-1-0-1.html?q=3|49738||&rp=26600|48483&ff=|0|2|1&adidx=2&f=ad&adp=130610&adid=632686",

        "https://www.lagou.com/zhaopin/chanpinzongjian/?labelWords=label",
        "https://mall.ccmn.cn/mallstocks/",
        "https://sh.julive.com/project/s/i1",
        "https://www.meiju.net/Mlist//Mju13.html",
        "http://mall.molbase.cn/p/612",
        "https://www.haier.com/xjd/all.shtml",
        "https://bj.nuomi.com/540",
        "https://www.haozu.com/sh/fxxiezilou/",
        "http://www.dianping.com/",
        "http://www.dianping.com/wuhan/ch55/g163",

        "https://p4psearch.1688.com/p4p114/p4psearch/offer.htm?spm=a2609.11209760.it2i6j8a.680.3c312de1W6LoPE&keywords=不锈钢螺丝", // very long time
        // company
        "https://www.cyzone.cn/company/list-0-0-1/",
        "https://www.cyzone.cn/capital/list-0-1-4/",
        // flower
        "https://www.hua.com/flower/",
        "https://www.hua.com/gifts/chocolates/",
        "https://www.hua.com/yongshenghua/yongshenghua_large.html",
        "http://www.cityflower.net/goodslist/5/",
        "http://www.cityflower.net/goodslist/2/",
        "http://www.cityflower.net/goodslist/1/0/0-0-4-0.html",
        "http://www.cityflower.net/",
        "http://www.zgxhcs.com/",
        // laobao
        "https://www.zhaolaobao.com/productlist.html?classifyId=77",
        "https://www.zhaolaobao.com/productlist.html?classifyId=82",
        "https://www.zhaolaobao.com/",
        // snacks
        "http://www.lingshi.com/",
        "http://www.lingshi.com/list/f64_o1.htm",
        "http://www.lingshi.com/list/f39_o1.htm",
        // jobs
        "https://www.lagou.com/gongsi/",
        "https://www.lagou.com/zhaopin/chanpinzongjian/",
        // love
        "http://yuehui.163.com/",
        // movie
        "http://v.hao123.baidu.com/v/search?channel=movie&category=科幻",
        "https://youku.com/",
        "https://movie.youku.com/?spm=a2ha1.12675304.m_6913_c_14318.d_3&scm=20140719.manual.6913.url_in_blank_http%3A%2F%2Fmovie.youku.com",
        "https://auto.youku.com/?spm=a2ha1.12675304.m_6913_c_14318.d_16&scm=20140719.manual.6913.url_in_blank_http%3A%2F%2Fauto.youku.com",
        "http://list.youku.com/category/video?spm=a2h1n.8251847.0.0",

        // pets
        "http://shop.boqii.com/cat/list-576-0-0-0.html",
        "http://shop.boqii.com/small/",
        "http://shop.boqii.com/brand/",
        "http://longyu.cc/shop.php?mod=exchange",
        "http://longdian.com/",
        "http://longdian.com/et_special.php?id=75",
        // menu
        "http://life.hao123.com/menu",
        "http://www.chinacaipu.com/shicai/sjy/junzao/xianggu/",

        "http://sj.zol.com.cn/",
        "http://sj.zol.com.cn/series/list80_1_1.html",
        "http://sj.zol.com.cn/pad/android/",
        "https://www.taoshouyou.com/game/wangzherongyao-2256-0-21",
        // property
        "https://www.ausproperty.cn/building/melbourne/",
        "http://jp.loupan.com/xinfang/",
        "https://malaysia.fang.com/house/",
        ""
    ).filter { it.isNotBlank() }

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
            logger.info("Arranging links in page $url")
            val normUrl = HNormUrl.parse(url, session.sessionConfig.toVolatileConfig())
            val doc = session.load(url).let { session.parse(it) }
            session.arrangeLinks(normUrl, doc)
            doc.also { it.annotateNodes(normUrl.hOptions) }.also { session.export(it) }
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

fun main() {
    HarvestExamples().harvestAll()

    val baseDir = AppPaths.REPORT_DIR.resolve("harvest/corpus/")
    ExoticUtils.openBrowser("$baseDir")
}
