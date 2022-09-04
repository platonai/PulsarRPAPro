package ai.platon.exotic.examples.sites.food.dianping

object TaskDef {

    val commentSelectors = IntRange(1, 10)
        .map { i ->
            listOf(
                "comment-$i" to cs(i),
//                "comment-$i-user-name" to cs(i) + " .content .user-info p",
//                "comment-$i-avePrice" to cs(i) + " .content .shop-info .average",
                "comment-$i-desc" to cs(i) + " .content p.desc.J-desc",
                "comment-$i-desc-" to cs(i) + " .content p.desc",
//                "comment-$i-publishTime" to cs(i) + " .content .misc-info .time",
//                "comment-$i-praise" to cs(i) + " .content .misc-info .J-praise",
//                "comment-$i-response" to cs(i) + " .content .misc-info .J-response",
//                "comment-$i-favorite" to cs(i) + " .content .misc-info .J-favorite",
//                "comment-$i-report" to cs(i) + " .content .misc-info .J-report",
//                "comment-$i-shop" to cs(i) + " .content .misc-info .shop"
            ).map { it.first to it.second }
        }.flatten().associate { it.first to it.second }

    val fieldSelectors = mutableMapOf(
        "001basicInfo" to ".basic-info",
        "002shopName" to ".basic-info .shop-name",
        "003score" to ".basic-info .brief-info .mid-score",
//        "reviewCount" to "#reviewCount",
//        "avgPrice" to "#avgPriceTitle",
//        "commentScores" to "#comment_score",
//        "address" to "#address",
//        "tel" to ".tel",
    ).also { it.putAll(commentSelectors) }

    val homePage = "https://www.dianping.com/"

    val portalUrls = mutableListOf(
        "https://www.dianping.com/beijing/ch10/g104",
        "https://www.dianping.com/beijing/ch10/g105",
        "https://www.dianping.com/beijing/ch10/g106",
        "https://www.dianping.com/beijing/ch10/g107",
        "https://www.dianping.com/beijing/ch10/g109",
        "https://www.dianping.com/beijing/ch10/g110",
        "https://www.dianping.com/beijing/ch75/g34309",
        "https://www.dianping.com/beijing/ch25/g136",
        "https://www.dianping.com/beijing/ch25/g105",
        "https://www.dianping.com/beijing/ch25/g106",
        "https://www.dianping.com/beijing/ch25/g107",
        "https://www.dianping.com/beijing/ch30",
        "https://www.dianping.com/beijing/ch30/g141",
        "https://www.dianping.com/beijing/ch30/g135",
        "https://www.dianping.com/beijing/ch30/g144",
        "https://www.dianping.com/beijing/ch30/g134",
    )

    fun cs(i: Int) = buildCommentSelector(i)

    fun buildCommentSelector(i: Int): String {
        return "ul.comment-list li.comment-item:nth-child($i)"
    }

    fun isShop(url: String): Boolean {
        return "shop" in url
    }
}
