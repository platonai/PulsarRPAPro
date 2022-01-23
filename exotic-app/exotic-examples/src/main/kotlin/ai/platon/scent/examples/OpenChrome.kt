package ai.platon.scent.examples.sites

import ai.platon.scent.ql.h2.context.withSQLContext

fun main() = withSQLContext { cx ->
    val session = cx.createSession()
    session.load("https://www.tmall.com/", "-refresh")
    readLine()
}
