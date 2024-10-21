package ai.platon.exotic.crawl.common

import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.RequiredDirectory
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/**
 * Paths for machine learning tasks.
 */
object ExoticMLPaths {
    
    val logger: Logger = LoggerFactory.getLogger(ExoticMLPaths::class.java)
    
    @RequiredDirectory
    val baseDir: Path = AppPaths.getProcTmp("ml")
    
    @RequiredDirectory
    val datasetDir: Path = baseDir.resolve("dataset")
    
    @RequiredDirectory
    val taskBaseDir: Path = baseDir.resolve("tasks")
    
    /**
     * The pattern of unsupervised task directory names.
     * A typical directory name: `p1723201624`.
     *  - p: project
     *  - 1723201624: seconds from epoch
     * */
    val projectBaseDirPattern = "p\\d+".toRegex()
    
    @RequiredDirectory
    val supervisedTaskDir: Path = taskBaseDir.resolve("supervised")
    
    /**
     * The directory to store unsupervised tasks.
     * A program may generate unsupervised tasks in this directory and another program may process the tasks.
     * */
    @RequiredDirectory
    val supervisedTaskProcessingDir = supervisedTaskDir.resolve("processing")
    /**
     * The directory to store unsupervised tasks that have been processed.
     * */
    @RequiredDirectory
    val supervisedTaskProcessedDir = supervisedTaskDir.resolve("processed")
    /**
     * The directory to store unsupervised tasks that have been generated.
     * */
    @RequiredDirectory
    val supervisedTaskResultBaseDir = supervisedTaskDir.resolve("result")
    
    ////////////////////////////////////////////////////////////////////////////
    // Unsupervised task directories
    
    /**
     * The directory to store unsupervised tasks.
     * A program may generate unsupervised tasks in this directory and another program may process the tasks.
     * */
    @RequiredDirectory
    val unsupervisedTaskDir: Path = taskBaseDir.resolve("unsupervised")
    
    /**
     * The directory to store unsupervised tasks.
     * A program may generate unsupervised tasks in this directory and another program may process the tasks.
     * */
    @RequiredDirectory
    val unsupervisedTaskProcessingDir = unsupervisedTaskDir.resolve("processing")
    /**
     * The directory to store unsupervised tasks that have been processed.
     * */
    @RequiredDirectory
    val unsupervisedTaskProcessedDir = unsupervisedTaskDir.resolve("processed")
    /**
     * The directory to store unsupervised tasks that have been generated.
     * */
    @RequiredDirectory
    val unsupervisedTaskResultBaseDir = unsupervisedTaskDir.resolve("result")
    
    ////////////////////////////////////////////////////////////////////////////
    // Prompt task directories

    /**
     * The directory to store prompts.
     * A program may generate prompts in this directory and another program may process the prompts.
     * */
    @RequiredDirectory
    val promptTaskBaseDir = taskBaseDir.resolve("prompts")
    /**
     * The directory to store prompts that are being processed.
     * */
    @RequiredDirectory
    val promptTaskProcessingDir = promptTaskBaseDir.resolve("processing")
    /**
     * The directory to store prompts that have been processed.
     * */
    @RequiredDirectory
    val promptTaskProcessedDir = promptTaskBaseDir.resolve("processed")
    /**
     * The directory to store prompts that have been generated.
     * */
    @RequiredDirectory
    val promptTaskResultBaseDir = promptTaskBaseDir.resolve("result")
    /**
     * The directory to store prompts that have been generated.
     * */
    @RequiredDirectory
//    val promptTaskResultCacheDir = promptTaskResultBaseDir.resolve("cache")
    val promptTaskResultCacheDir = AppPaths.CACHE_DIR.resolve("prompts")
    /**
     * The pattern of prompt file names.
     * A typical file name: `prompt.p1723201624.0.remarkable.txt`.
     *  - p1723201624: the project id
     *  - 0: the prediction in the clustering result
     *  - remarkable: a label for the prompt, it's optional
     *  - txt: the file extension
     *
     *  Can be parsed into a PromptFile object.
     *
     * ```
     * data class PromptFile(
     *     val clusteringProjectId: String,
     *     val prediction: String,
     *     val label: String,
     *     val extension: String
     * )
     * ```
     *
     * Explanation:
     *  - `prompt.`: Prefix indicating a prompt file.
     *  - `(p\\d+)`: Captures the clustering task ID, starting with 'p' followed by digits.
     *  - `(\\d+)`: Captures the cluster ID, consisting of one or more digits.
     *  - `(\.\w+)?`: A optional label.
     *  - `([a-zA-Z]+)`: Fixed file extension indicating the file format.
     * */
    val promptFileNamePattern = "prompt.(p\\d+)\\.(\\d+)\\.?(\\w+)?\\.([a-zA-Z]+)".toRegex()
    /**
     * The pattern of prompt answer file names.
     * The answer is generated by the LLM.
     * A typical file name: `prompt.p1723201624.0.excellent.answer.json`.
     *   - p1723201624: the project id
     *   - 0: the prediction in the clustering result
     *   - excellent: a label for the answer, it's optional
     *   - json: the file extension
     *
     * Can be parsed into a AnswerFile object.
     *
     * ```
     * data class AnswerFile(
     *     val clusteringProjectId: String,
     *     val prediction: String,
     *     val label: String,
     *     val extension: String
     * )
     * ```
     *
     * Explanation:
     *  - `prompt.`: Prefix indicating a prompt file.
     *  - `(p\\d+)`: Captures the clustering task ID, starting with 'p' followed by digits.
     *  - `(\\d+)`: Captures the cluster ID, consisting of one or more digits.
     *  - `(\.\w+)?`: A optional label.
     *  - `([a-zA-Z]+)`: Fixed file extension indicating the file format.
     * */
    val answerFileNamePattern = "prompt.(p\\d+)\\.(\\d+)\\.?(\\w+)?\\.answer\\.([a-zA-Z]+)".toRegex()
    
    /**
     * The directory to store reports.
     * A program may generate reports in this directory and another program may process the reports.
     * */
    @RequiredDirectory
    val reportBaseDir = taskBaseDir.resolve("report")
    /**
     * The directory to store reports that are being processed.
     * */
    @RequiredDirectory
    val reportProcessingDir = reportBaseDir.resolve("processing")
    /**
     * The directory to store reports that have been processed.
     * */
    @RequiredDirectory
    val reportProcessedDir = reportBaseDir.resolve("processed")
    /**
     * The directory to store the result of report processing.
     * */
    @RequiredDirectory
    val reportResultDir = reportBaseDir.resolve("result")
    /**
     * The pattern of report file names.
     *
     * The file name should have an extension.
     * */
    val reportFileNamePattern = ".+\\..+".toRegex()
    
    init {
        createDirectories()
    }
    
    /**
     * Create directories if they do not exist.
     */
    fun createDirectories() {
        ExoticMLPaths::class.java.declaredFields
            .filter { it.annotations.any { it is RequiredDirectory } }
            .mapNotNull { it.get(AppPaths) as? Path }
            .forEach { it.takeUnless { Files.exists(it) }?.let { Files.createDirectories(it) } }
    }

    fun copyToLearnUnsupervised(taskFile: Path) {
        val path = unsupervisedTaskDir.resolve(taskFile.fileName)
        Files.copy(taskFile, path, StandardCopyOption.REPLACE_EXISTING)
    }
    
    fun copyToPrompt(promptFile: Path) {
        val path = promptTaskBaseDir.resolve(promptFile.fileName)
        Files.copy(promptFile, path, StandardCopyOption.REPLACE_EXISTING)
    }
    
    fun copyToReport(reportFile: Path) {
        val path = reportBaseDir.resolve(reportFile.fileName)
        Files.copy(reportFile, path, StandardCopyOption.REPLACE_EXISTING)
    }
}
