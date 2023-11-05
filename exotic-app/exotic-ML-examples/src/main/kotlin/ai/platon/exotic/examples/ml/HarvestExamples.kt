package ai.platon.exotic.examples.ml

import ai.platon.exotic.common.ExoticUtils
import ai.platon.exotic.crawl.common.VerboseHarvester
import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.common.AppPaths
import ai.platon.scent.ScentContext
import ai.platon.scent.ql.h2.context.ScentSQLContexts
import org.slf4j.LoggerFactory

class HarvestExamples(
    context: ScentContext = ScentSQLContexts.create()
): VerboseHarvester(context) {

    private val logger = LoggerFactory.getLogger(HarvestExamples::class.java)

    val testedSeeds = listOf(
        /////////////////////////////////////////////////////////
        // The sites below are well tested

        "https://www.hua.com/gifts/chocolates/",
        "http://category.dangdang.com/cid4002590.html",
        "https://list.jd.com/list.html?cat=6728,6742,13246",
        "https://list.gome.com.cn/cat10000055-00-0-48-1-0-0-0-1-2h8q-0-0-10-0-0-0-0-0.html?intcmp=bx-1000078331-1",
        "https://www.amazon.com/Best-Sellers-Automotive/zgbs/automotive/ref=zg_bs_nav_0",
        "https://category.vip.com/search-1-0-1.html?q=3|49738||&rp=26600|48483&ff=|0|2|1&adidx=2&f=ad&adp=130610&adid=632686",

        "https://mall.ccmn.cn/mallstocks/",
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
        "https://www.cityflower.net/attribute/37.html",
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
        "https://list.gome.com.cn/cat10000070-00-0-48-1-0-0-0-1-0-0-1-0-0-0-0-0-0.html?intcmp=phone-163",
        "http://category.dangdang.com/cid4002590.html -tp 140 -i 1h -scrollCount 20 -ii 1d -ol a[href~=product]",
        "https://search.jd.com/Search?keyword=basketball&enc=utf-8&wq=basketball&pvid=27d8a05385cd49298b5caff778e14b97",
        "https://www.amazon.com/Best-Sellers-Automotive/zgbs/automotive/ref=zg_bs_nav_0",
        "https://shopee.sg/Computers-Peripherals-cat.11013247 -ol a[href~=sp_atk] -tl 20 -ignoreFailure -component .page-product__breadcrumb  -component .product-briefing",
    ).filter { it.isNotBlank() }

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
    BrowserSettings.headless()
    HarvestExamples().harvestAll()

    val baseDir = AppPaths.REPORT_DIR.resolve("harvest/corpus/")
    ExoticUtils.openBrowser("$baseDir")
}
