package ai.platon.exotic.standalone.starter

import ai.platon.exotic.common.ExoticUtils

fun main(argv: Array<String>) {
    ExoticUtils.prepareDatabaseOrFail()

    val executor = ExoticExecutor(argv)
    executor.parseCmdLine()
    executor.harvest = true
    executor.configuredUrl = "https://www.hua.com/flower/"
    executor.execute()
}
