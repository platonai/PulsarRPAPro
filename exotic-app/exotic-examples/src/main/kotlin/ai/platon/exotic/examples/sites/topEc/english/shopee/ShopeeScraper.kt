package ai.platon.exotic.examples.sites.topEc.english.shopee

import ai.platon.pulsar.context.PulsarContexts

fun main() {
    val portalUrl = "https://shopee.sg/Computers-Peripherals-cat.11013247"
    val args = "-i 1s -ii 100d -ol a[href~=sp_atk] -tl 100 -ignoreFailure"
    val session = PulsarContexts.createSession()

//    val fieldSelectors = listOf("._2Csw3W", "._3uBhVI", "._3b2Btx", "._1kpF5Y")
    val fieldSelectors = mapOf(
        "title" to "._2Csw3W",
        "price" to "._3uBhVI",
        "star" to "._3b2Btx",
        "ratings" to "._1kpF5Y"
    )
    // the result changes frequently, here are the history records:
    // 2022-04-27
    // [{title=$19.90, price=4.9, star=8k, ratings=65% off}, {title=$1.99 - $5.28, price=4.9, star=5.3k, ratings=89% off}, {title=$12.38 - $15.00, price=4.9, star=1.2k, ratings=41% off}, {title=$19.90 - $39.90, price=4.9, star=29k, ratings=62% off}, {title=$10.65 - $18.99, price=4.9, star=11.1k, ratings=58% off}, {title=$5.02 - $10.00, price=4.9, star=7.5k, ratings=70% off}, {title=$6.70 - $14.10, price=4.8, star=3.6k, ratings=53% off}, {title=$23.99 - $29.99, price=4.9, star=3.9k, ratings=58% off}, {title=$3.06, price=5.0, star=153, ratings=3% off}, {title=$24.99 - $39.99, price=4.9, star=15.6k, ratings=72% off}, {title=$8.50, price=5.0, star=3.1k, ratings=31% off}, {title=null, price=4.7, star=4.1k, ratings=null}, {title=$3.99 - $11.99, price=4.8, star=20.4k, ratings=52% off}, {title=$1.89, price=5.0, star=31.4k, ratings=84% off}, {title=null, price=null, star=null, ratings=null}, {title=$3.79 - $9.90, price=4.8, star=10.7k, ratings=50% off}, {title=null, price=null, star=null, ratings=null}, {title=null, price=4.9, star=2.4k, ratings=null}, {title=$4.50 - $7.50, price=4.9, star=3.9k, ratings=49% off}, {title=null, price=4.8, star=2.6k, ratings=null}]
    //
    val fields = session.scrapeOutPages(portalUrl, args, fieldSelectors)
    println(fields)
}
