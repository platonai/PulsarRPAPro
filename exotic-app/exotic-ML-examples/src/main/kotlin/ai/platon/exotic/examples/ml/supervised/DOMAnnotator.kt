package ai.platon.exotic.examples.ml.supervised

import ai.platon.exotic.examples.ml.supervised.annotate.AnnotateRequest
import ai.platon.exotic.examples.ml.supervised.annotate.Annotation
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.getForEntity
import org.springframework.web.client.postForObject

class DOMAnnotator {
    private val hostname = "127.0.0.1"
    private val serverPort: Int = 8182
    private val contextPath: String = "/api"
    
    private val baseUri get() = String.format("http://%s:%d%s", hostname, serverPort, contextPath)
    private val authToken = "b06test42c13cb000f74539b20be9550b8a1a90b9"
    
    private val restTemplate = RestTemplate()
    
    private val annotationTasks = mutableSetOf<String>()
    
    /**
     * The tasks are in the following folder:
     *
     * AppPaths.EXPORT_DIR.resolve("annotated")
     * */
    fun annotate() {
        val listTaskUrl = "$baseUri/annotate-tasks"
        val response: ResponseEntity<Array<String>> = restTemplate.getForEntity(listTaskUrl)
        response.body?.toCollection(annotationTasks)
        
        println("Total " + annotationTasks.size + " annotation tasks")
        
        annotationTasks.forEachIndexed { i, u ->
            val a = i * 10
            val b = i % 3
            val top = 2 * a + b
            val width = 300 + b
            val request = AnnotateRequest(
                authToken, u,
                annotations = listOf(
                    Annotation("100, $top, $width, 100", "foo"),
                    Annotation("200, $top, $width, 200", "bar"),
                    Annotation("300, $top, $width, 300", "zoo"),
                )
            )
            val annotations: String? = restTemplate.postForObject("$baseUri/annotate", request, String::class)
            println("$annotations\t$u")
        }
    }
}

fun main() {
    val annotator = DOMAnnotator()
    annotator.annotate()
}
