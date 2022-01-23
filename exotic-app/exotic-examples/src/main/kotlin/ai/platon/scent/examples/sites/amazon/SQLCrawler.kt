package ai.platon.scent.examples.sites.amazon

import ai.platon.scent.examples.common.VerboseSqlExtractor
import ai.platon.scent.ql.h2.context.withSQLContext

fun main() = withSQLContext { cx ->
    val url = "https://www.amazon.com/s?rh=n:3396311&rd=1&fs=true"
    val sql = "select\n" +
            "    dom_base_uri(dom) as `url`,\n" +
            "    dom_first_text(dom, 'div span:containsOwn(results for) , div span:containsOwn(results)') as `results`,\n" +
            "    array_join_to_string(dom_all_texts(dom, 'ul.a-pagination > li, div#pagn > span'), '|') as `pagination`\n" +
            "from load_and_select('$url -i 1s', 'body');"

    val extractor = VerboseSqlExtractor(cx)
    extractor.execute(sql)

    readLine()
}
