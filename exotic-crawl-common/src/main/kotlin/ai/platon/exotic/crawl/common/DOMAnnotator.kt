package ai.platon.exotic.crawl.common

import org.springframework.web.client.RestTemplate

class DOMAnnotator {
    private val restTemplate = RestTemplate()
    
    fun annotate() {
    
    }
}

fun main() {
    val annotator = DOMAnnotator()
    annotator.annotate()
}
