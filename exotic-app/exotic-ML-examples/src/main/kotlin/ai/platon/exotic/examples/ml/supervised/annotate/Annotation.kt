package ai.platon.exotic.examples.ml.supervised.annotate
/**
 * Annotation
 * */
data class Annotation(
    val vi: String,
    val label: String,
)

/**
 * Annotate request
 * */
data class AnnotateRequest(
    val authToken: String,
    var url: String,
    val args: String? = null,
    val annotations: List<Annotation>,
)
