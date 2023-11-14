package ai.platon.exotic.examples.ml.supervised

import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.Frequency
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.dom.Documents
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.dom.nodes.node.ext.*
import ai.platon.pulsar.dom.select.collectIf
import ai.platon.scent.common.message.ScentMiscMessageWriter
import ai.platon.scent.common.mlLabels
import ai.platon.scent.common.normUrl
import ai.platon.scent.context.ScentContexts
import ai.platon.scent.ml.BasicNGramNodeEncoder
import ai.platon.scent.ml.NodeDataFrame
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.absolutePathString
import kotlin.io.path.isRegularFile
import kotlin.streams.toList

class DOMClassifier {
    private val logger = getLogger(this)

    private val session = ScentContexts.createSession()
    private val annotationTasks = mutableSetOf<String>()

    private var numChunks = 2
    private val labelSet = Frequency<String>()

    private val args = "" +
            " -nScreens 1" +
            " -trustSamples" +
//                " -polysemous" +
            " -diagnose" +
            " -nVerbose 1" +
            " -showTip" +
            " -showImage" +
//                " -cellType PLAIN_TEXT"
            ""

    private val options = session.options(args)

    private val messageWriter = session.context.getBean<ScentMiscMessageWriter>()

    private val exportPath = AppPaths.get("/tmp/dataset-dom-20230108.csv")
    private val modelPath = AppPaths.get("/tmp/dom_decision_tree.pmml")
    private val predictResultPath = AppPaths.get("/tmp/dom-predict-result.csv")

    /**
     * Node filter for biding industry
     * */
    private val biddingNodeFilter: (Node) -> Boolean = { node ->
        node is Element &&
                node.left in 0..500 &&
                node.width >= 200 &&
                node.top in 100..2000 &&
                node.bottom > 100 &&
                node.numChars > 5
    }

    fun encode() {
        labelSet.clear()

        var i = 0
        IntRange(0, numChunks - 1).forEach { ident ->
            prepareAnnotationTasks(ident)

            annotationTasks.take(50000)
                .asSequence()
                .map { Documents.parse(Paths.get(it), "UTF-8") }
                .mapNotNull { it.normUrl }
                .mapNotNull { loadTrainableDocumentOrNull(it, args) }
                .forEach { document ->
                    val encoder = BasicNGramNodeEncoder(nGram = options.nGram)
                    val nodes = document.document.collectIf(biddingNodeFilter)
                    val points = encoder.encode(nodes)
                    if (points.isNotEmpty()) {
                        val df = NodeDataFrame(points, encoder.schema, path = exportPath)
                        df.export()
                    }

                    if (++i % 1000 == 0) {
                        System.gc()
                    }
                }
        }

        logger.info("Feature file is exported. | {}", exportPath)

        logger.info("All done! ✨ \uD83C\uDF1F ✨")
    }

    fun train() {
        TODO("Use python to train the model")
    }

    fun predict() {
        Files.deleteIfExists(predictResultPath)
        Files.writeString(predictResultPath, "Url,Title,Brief,Content\n")

        val model = DOMEvaluator(modelPath, predictResultPath)

        val predictFailures = mutableMapOf<Int, Int>()
        var i = 0
        IntRange(0, 5).forEach { ident ->
            prepareAnnotationTasks(ident)

            annotationTasks.take(2000000)
                .asSequence()
                .map { Documents.parse(Paths.get(it), "UTF-8") }
                .mapNotNull { it.normUrl }
                .forEach { url ->
                    val result = model.predict(url)

                    val p = result["probability(1)"] ?: -1.0
                    if (p != 1.0) {
                        val failureCount = 1 + predictFailures.computeIfAbsent(ident) { 0 }
                        predictFailures[ident] = failureCount
                        messageWriter.write(url, "predict-failed-urls.txt")
                        messageWriter.write(result.toString(), "predict-failed-urls.txt")
                    }

                    if (++i % 1000 == 0) {
                        System.gc()
                    }
                }
        }

        logger.info("Predict failures in each directory: $predictFailures")

        logger.info("All done! ✨ \uD83C\uDF1F ✨")
    }

    private fun loadTrainableDocumentOrNull(url: String, args: String): FeaturedDocument? {
        val page = session.load(url, args)
        val document = session.parse(page, noCache = true)

        val mlLabels = page.mlLabels ?: return null

        document.forEach { node ->
            var labeled = false

            mlLabels.forEach { (vi, label) ->
                if (label != null && node.attr("vi") == vi) {
                    node.addMlLabel(label)
                    labelSet.add(label)
                    labeled = true
                }
            }

            if (!labeled) {
                labelSet.add("None")
            }
        }

        return document
    }

    private fun prepareAnnotationTasks(ident: Int) {
        BrowserSettings.privacy(2).maxTabs(8).withSPA()

        val baseDir = ExportPaths.BASE_DIR.resolve("annotated")
        val exportPath = baseDir.resolve("$ident")
        val mlAnnotationDir = exportPath.resolveSibling("ml.$ident")
        Files.createDirectories(mlAnnotationDir)
        val paths = Files.list(mlAnnotationDir).filter { it.isRegularFile() }.map { it.absolutePathString() }.toList()

        annotationTasks.clear()
        annotationTasks.addAll(paths)
    }
}

fun main() {
    val classifier = DOMClassifier()
     classifier.encode()
//    classifier.predict()
}
