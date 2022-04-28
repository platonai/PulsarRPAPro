package ai.platon.exotic.examples.sites.video

import ai.platon.pulsar.test.VerboseSQLExecutor

fun main() {
    val portalUrl = "https://www.bilibili.com/movie/?spm_id_from=333.1007.0.0"
    val args = "-i 1s -ii 1d -ignoreFailure"

    val sql = """
select
    dom_first_text(dom, '.media-title') as title,
    dom_first_text(dom, '.pub-info') as addedTime,
    dom_first_text(dom, 'h4.score') as score,
    dom_first_text(dom, '.media-rating p') as ratings
from
    load_out_pages('$portalUrl $args', 'a[href~=play]', 1, 20)
    """.trimIndent()

    val executor = VerboseSQLExecutor()
    executor.executeQuery(sql)
}
