package ai.platon.exotic.standalone.starter

import ai.platon.exotic.driver.common.ExoticUtils

fun main(argv: Array<String>) {
    ExoticUtils.prepareDatabase()

    val executor = ExoticExecutor(argv)
    executor.parseCmdLine()
    executor.execute()
}
