package ai.platon.scent.examples.sites.amazon

import ai.platon.pulsar.ql.h2.H2Db
import ai.platon.scent.ql.h2.context.withSQLContext

private const val asinBestUrl = "https://www.amazon.com/Best-Sellers-Automotive/zgbs/automotive/ref=zg_bs_nav_0"
private const val asinCategory = "https://www.amazon.com/b?node=3117954011"
private const val args = "-i 1d -ii 365d -irs 500000 -tl 40 -ol a[href~=/dp/] -diagnose -vj"

fun main() = withSQLContext { cx ->
    H2Db().getRandomConnection().use {  conn ->
        conn.createStatement().use {  stat ->
            stat.execute("select * from harvest('https://www.amazon.com/b?node=3117954011 $args')")
        }
    }
}
