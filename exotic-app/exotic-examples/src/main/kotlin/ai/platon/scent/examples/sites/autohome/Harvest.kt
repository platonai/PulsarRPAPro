package ai.platon.scent.examples.sites.autohome

import ai.platon.scent.examples.common.WebHarvester
import ai.platon.scent.ql.h2.context.withSQLContext

fun main() = withSQLContext {
    WebHarvester(it).harvest(
        "https://mall.autohome.com.cn/list/0-0-33-0-0-0-0-0-0-1.html",
        "-i 1d -ii 1d -ol a[href~=detail]"
    )
}
