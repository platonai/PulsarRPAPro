package ai.platon.exotic.standalone

fun main(argv: Array<String>) {
    val executor = ExoticExecutor(argv)
    executor.parseCmdLine()
    executor.execute()
}
