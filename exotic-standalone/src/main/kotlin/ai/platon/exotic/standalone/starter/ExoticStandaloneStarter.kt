package ai.platon.exotic.standalone.starter

fun main(argv: Array<String>) {
    val executor = ExoticExecutor(argv)
    executor.parseCmdLine()
    executor.execute()
}
