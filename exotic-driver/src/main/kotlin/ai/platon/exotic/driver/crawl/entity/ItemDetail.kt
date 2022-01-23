package ai.platon.exotic.driver.crawl.entity

class ItemDetail(
    var uri: String,
    var baseUri: String
) {
    var properties: Map<String, Any?> = mutableMapOf()
    var allowDuplicate = false

    companion object {
        fun create(uri: String, properties: Map<String, Any?>, allowDuplicate: Boolean = false): ItemDetail {
            val product = ItemDetail(uri, (properties["base_uri"] ?: "").toString())
            product.properties = properties
            product.allowDuplicate = allowDuplicate
            return product
        }
    }
}
