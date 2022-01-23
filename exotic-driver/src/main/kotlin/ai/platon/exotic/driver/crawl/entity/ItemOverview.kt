package ai.platon.exotic.driver.crawl.entity

class ItemOverview(
    var href: String
) {
    override fun toString() = """{href: '$href'}"""

    companion object {
        fun create(fields: Map<String, Any>): ItemOverview {
            return ItemOverview((fields["href"] ?: "").toString())
        }
    }
}
