package ai.platon.exotic.crawl.common

import ai.platon.pulsar.common.LinkExtractors
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.math.vectors.get
import ai.platon.pulsar.dom.nodes.node.ext.*
import ai.platon.scent.common.message.ScentMiscMessageWriter
import ai.platon.scent.context.ScentContexts
import ai.platon.scent.ml.EncodeOptions
import ai.platon.scent.ml.NodePoint
import ai.platon.scent.ml.jpmml.JPMMLEvaluator
import com.google.common.base.CharMatcher
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.StringUtils
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class DOMEvaluator(
    modelPath: Path,
    private val predictResultPath: Path? = null
) {
    companion object {
        const val FULL_WIDTH_COMMA = "，"
    }
    
    private val logger = getLogger(this)
    
    private val session = ScentContexts.createSession()
    
    private val evaluator = JPMMLEvaluator()
    
    private val messageWriter = session.context.getBean<ScentMiscMessageWriter>()
    
    private val failedUrlReportFile = "predict-failed-urls.txt"
    
    private val datasetPath = Files.createTempFile("dataset-", "csv")
    
    private val biddingNodeFilter: (Node) -> Boolean = { node ->
        node is Element &&
            node.left in 0..500 &&
            node.width >= 200 &&
            node.top in 100..2000 &&
            node.bottom > 100 &&
            node.numChars in 5..100
    }
    
    init {
        evaluator.model = modelPath.toFile()
        
        if (!Files.exists(evaluator.model.toPath())) {
            System.err.println("Model file not found | $modelPath")
        }
        
        if (predictResultPath != null) {
            Files.deleteIfExists(predictResultPath)
            messageWriter.write("URL,Title,Brief,Content", predictResultPath)
        }
    }
    
    fun predict(record: Map<String, *>): Map<String, *> {
        return evaluator.evaluate(record)
    }
    
    fun predict(url: String): Map<String, *> {
        return try {
            predict0(url)
        } catch (t: Throwable) {
            t.printStackTrace()
            mapOf<String, Any>()
        }
    }
    
    private fun predict0(url: String): Map<String, *> {
        val options = session.options()
        
        val document = session.loadDocument(url, options)
        val hasIframe = document.selectFirstOrNull("iframe") != null
        if (hasIframe) {
            return mapOf("Failure" to "IFrame")
        }
        
//        val df = session.encodeNodes(listOf(document), options, biddingNodeFilter)
        
        val encodeOptions = EncodeOptions(datasetPath = datasetPath)
        val df = session.encodeNodes(document, encodeOptions, filter = biddingNodeFilter)
        
        val columns = df.schema.columns
        
        val titleLabel = "1"
        val result: MutableMap<String, String> = mutableMapOf()
        val sb = StringBuilder()
//        for (x in df.points) {
//            val x1 = IntRange(0, columns.size - 1).associate { i -> columns[i].name to x[i] }
//            val r = predict(x1)
//
//            val p = r["probability($titleLabel)"]
//            if (p == 1.0) {
//                r.entries.associateTo(result) { it.key to it.value.toString() }
//                extractFields(url, x, result)
//                break
//            }
//        }
        
        if (predictResultPath != null && result.isNotEmpty()) {
            csvFormat(sb, result)
            messageWriter.write(sb.toString(), predictResultPath)
        }
        sb.clear()
        
        if (result.isEmpty()) {
            messageWriter.write(url, failedUrlReportFile)
        }
        
        return result
    }
    
    private fun extractFields(url: String, x: NodePoint, result: MutableMap<String, String>) {
        val ele = x.node.bestElement
        val title = ele.text()
        
        result["Url"] = url
        result["Title"] = title
        
        val contentNode = ele.parents().firstOrNull {
            it.numChars > 200 && it.numTextNodes > 20 && it.numAnchors < 10
        }
        
        if (contentNode != null) {
            val content = contentNode.text()
            val brief = StringUtils.abbreviateMiddle(content, "...", 100)
            
            result["Brief"] = brief
            result["Content"] = content
        }
    }
    
    private fun csvFormat(sb: StringBuilder, result: MutableMap<String, String>) {
        val url = result["Url"] ?: return
        val title = result["Title"] ?: return
        val brief = result["Brief"]
        val content = result["Content"]
        
        sb.append(cleanCsvCell(url)).append(",")
        sb.append(cleanCsvCell(title)).append(",")
        if (brief != null) sb.append(cleanCsvCell(brief)).append(",")
        if (content != null) sb.append(cleanCsvCell(content))
    }
    
    private fun cleanCsvCell(text: String): String {
        var text0 = text
        
        text0 = text0.replace("\\p{Cntrl}".toRegex(), "\t")
        text0 = CharMatcher.javaIsoControl().removeFrom(text0)
        text0 = text0.replace(",", FULL_WIDTH_COMMA)
        
        return text0
    }
}

fun main() {
    val modelPath = Paths.get("/tmp/dom_decision_tree.pmml")
    if (!Files.exists(modelPath)) {
        val modelURL = URL("http://platonai.cn/s/model/dom_decision_tree_bidding.0.0.1.pmml")
        FileUtils.copyURLToFile(modelURL, modelPath.toFile())
    }
    
    val predictResultPath = Paths.get("/tmp/dom_decision_tree_predict.csv")
    val model = DOMEvaluator(modelPath, predictResultPath)
    
    val urls = LinkExtractors.fromResource("seeds/bidding-detail.txt")
        .filterNot { it.contains("index") }
        .shuffled()
        .take(10000)
    
    var predictedCount = 0
    var failureCount = 0
    var iframeCount = 0
    urls.forEach { url ->
        val result = model.predict(url)
        if (result.isEmpty()) {
            ++failureCount
            println("Failed to predict, [Unknown Reason] | $url")
        } else if (result.containsKey("Failure")) {
            ++iframeCount
            println("Failed to predict, [" + result["Failure"] + "] | $url")
        } else {
            ++predictedCount
            println(result)
            println("Title:\t" + result["Title"] + " | $url")
        }
    }
    
    println("\n=======================")
    val totalCount = urls.size - iframeCount
    val predicatedRate = predictedCount / totalCount.toFloat()
    println("Predicated: $predictedCount Total: $totalCount Predicated Rate: $predicatedRate")
    println("Full extracted result are exported to $predictResultPath")
    println("=======================\n")
}
