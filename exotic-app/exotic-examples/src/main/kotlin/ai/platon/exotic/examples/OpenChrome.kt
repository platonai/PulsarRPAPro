package ai.platon.exotic.examples

import ai.platon.scent.ql.h2.context.withSQLContext

fun main() = withSQLContext { cx ->
    val session = cx.createSession()
    session.load("https://www.tmall.com/", "-refresh")
}
