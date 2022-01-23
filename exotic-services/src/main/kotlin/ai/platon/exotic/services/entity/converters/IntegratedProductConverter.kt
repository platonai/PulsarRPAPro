package ai.platon.exotic.services.entity.converters

import ai.platon.exotic.services.entity.generated.IntegratedProduct
import java.time.Instant
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.isAccessible

data class ProductStatistics(
    var numNoName: Int = 0,
    var numNoPrice: Int = 0,
) {
    val isQualified get() = numNoName == 0 && numNoPrice == 0
}

class IntegratedProductConverter: ModelConverter() {
    companion object {
        val globalStatistics = ProductStatistics()
    }

    val statistics = ProductStatistics()

    fun convertReflect(props: Map<String, Any?>): Pair<IntegratedProduct, ProductStatistics> {
        val p = IntegratedProduct()
        val stat = ProductStatistics()

        IntegratedProduct::class.declaredMemberProperties
            .onEach { it.isAccessible = true }
            .map { it.call() }

        return p to stat
    }

    fun convert(props: Map<String, Any?>): Pair<IntegratedProduct, ProductStatistics> {
        val p = IntegratedProduct()
        val stat = ProductStatistics()

        p.id = l(props, "id")
        p.site = s(props, "site")
        p.bigImgUrl = s(props, "bigImgUrl")
        p.productName = s(props, "productName") ?: s(props, "productTitle")
        p.categoryPath = s(props, "categoryPath")?.split(" > ")?.take(3)?.joinToString(" > ")?:""
        p.brand = s(props, "brand")
        p.model = s(props, "model")
        p.specification = s(props, "specification")
        p.material = s(props, "material")
        p.price = d(props, "price")
        p.minAmountToBuy = i(props, "minAmountToBuy")
        p.maxAmountToBuy = i(props, "maxAmountToBuy")
        p.inventoryAmount = i(props, "inventoryAmount")
        p.stockPrompt = s(props, "stockPrompt")
        p.salesVolume = i(props, "salesVolume")
        p.favorableRate = s(props, "favorableRate")

        p.productReviews = s(props, "goodReviewsRawText") + "\n" +
                s(props, "normalReviewsRawText") + "\n" +
                s(props, "badReviewsRawText")
        p.shopName = s(props, "shopName")
        p.shopUrl = s(props, "shopUrl")
        p.shopTel = s(props, "shopTel")
        p.shopScores = s(props, "shopScores")?.removePrefix("评分详细 ")
        p.deliveryFrom = s(props, "deliveryFrom")
        p.expressFee = s(props, "expressFeeRawText")
        p.uri = s(props, "uri") ?: ""
        p.createTime = kotlin.runCatching { Instant.parse(s(props, "createTime")) }
            .getOrNull() ?: Instant.now()

        if (p.productName.isNullOrBlank()) {
            ++stat.numNoName
            ++statistics.numNoName
            ++globalStatistics.numNoName
        }
        if ((p.price ?: -1.0) < 0.0) {
            ++stat.numNoPrice
            ++statistics.numNoPrice
            ++globalStatistics.numNoPrice
        }

        return p to stat
    }
}
