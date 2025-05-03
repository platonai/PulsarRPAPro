package ai.platon.exotic.examples.ml

import ai.platon.exotic.crawl.common.VerboseHarvester
import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.proxy.ProxyPoolManager
import ai.platon.scent.skeleton.ScentContext
import ai.platon.scent.ql.h2.context.ScentSQLContexts

class HarvestExamples(context: ScentContext) : VerboseHarvester(context) {
    
    val testingSeeds = listOf(
        /////////////////////////////////////////////////////////
        // The sites below are well tested
        
        "https://www.hua.com/gifts/chocolates/",
        "http://category.dangdang.com/cid4002590.html -requireItemSize 500000",
        "https://list.jd.com/list.html?cat=6728,6742,13246",
        "https://list.gome.com.cn/cat10000055-00-0-48-1-0-0-0-1-2h8q-0-0-10-0-0-0-0-0.html?intcmp=bx-1000078331-1",
        "https://www.amazon.com/Best-Sellers-Automotive/zgbs/automotive/ref=zg_bs_nav_0",
        "https://category.vip.com/search-1-0-1.html?q=3|49738||&rp=26600|48483&ff=|0|2|1&adidx=2&f=ad&adp=130610&adid=632686",
        "https://www.amazon.com/b?node=1292115011 -ol a[href~=sp_atk] -ignoreFailure -component .page-product__breadcrumb -component .product-briefing",
        
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
        "https://movie.youku.com/",
        "https://auto.youku.com/",
        "http://list.youku.com/category/video",
        
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
        "http://shop.boqii.com/cat/list-576-0-0-0.html -ol a[href*=product]",
        "https://www.21cake.com/gallery-index---0---1.html -ol a[href*=goods]",
        "http://www.zgxhcs.com/flower_0_0_63_0_0_0_0_0_0.html -ol a[href*=/flower/]",
    ).filter { it.isNotBlank() }
    
    val testedSeeds = listOf(
        "https://www.amazon.com/Best-Sellers-Automotive/zgbs/automotive/ref=zg_bs_nav_0 -ol a[href*=/dp/]",
        "https://list.suning.com/0-20006-0-0-0-0-0-0-0-0-11635.html -component #gm-prd-main -component .breadcrumb",
        "https://list.gome.com.cn/cat10000062.html?intcmp=sy-1000051866-0",
        "https://www.hua.com/flower/",
        "https://mall.ccmn.cn/mallstocks/",
        "https://www.taoshouyou.com/game/wangzherongyao-2256-0-21",
        "http://shop.boqii.com/cat/list-576-0-0-0.html",
    )
    
    private val defaultArgs = """
    
    """.trimIndent()
    
    fun arrangeDocuments() {
        listOf(seeds, testedSeeds).flatten().toSet().filter { it.isNotBlank() }.forEach { url ->
            arrangeDocument(url, defaultArgs)
        }
    }
    
    fun harvest() {
        val url = seeds[0]
        harvest(url, defaultArgs)
    }
    
    fun harvestAll() {
        listOf(testedSeeds, seeds).flatten().toSet().filter { it.contains("hua") }.parallelStream().forEach {
            harvest(it, defaultArgs)
        }
    }
}

fun main() {
    ProxyPoolManager.enableDefaultProviders()
    BrowserSettings.maxBrowsers(4).maxOpenTabs(5)
//    HarvestExamples().arrangeDocuments()
    val context = ScentSQLContexts.create()
    val crawler = HarvestExamples(context)
    crawler.harvestAll()
    
    val baseDir = AppPaths.REPORT_DIR.resolve("harvest/corpus/")
    // crawler.openBrowser("$baseDir")
}
