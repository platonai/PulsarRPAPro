package ai.platon.exotic.examples.ml.supervised

import ai.platon.exotic.examples.ml.supervised.annotate.AnnotateRequest
import ai.platon.exotic.examples.ml.supervised.annotate.Annotation
import ai.platon.scent.common.Auth
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.getForEntity
import org.springframework.web.client.postForObject

class DOMAnnotator {
    @Value("\${server.port}")
    val serverPort: Int = 0

    @Value("\${server.servlet.context-path}")
    val contextPath: String = ""
    
    val hostname = "127.0.0.1"
    
    val baseUri get() = String.format("http://%s:%d%s", hostname, serverPort, contextPath)
    
    private val username = Auth.randomUsername()
    private val authToken = Auth.gen(username)
    
    private val restTemplate = RestTemplate()
    
    private val annotationTasks = mutableSetOf<String>()
    
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
