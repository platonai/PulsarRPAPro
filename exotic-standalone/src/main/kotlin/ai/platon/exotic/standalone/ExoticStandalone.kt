package ai.platon.exotic.standalone

fun main(args: Array<String>) {
    val executor = ExoticExecutor(args)
    executor.parseCmdLine()
    executor.execute()
}
