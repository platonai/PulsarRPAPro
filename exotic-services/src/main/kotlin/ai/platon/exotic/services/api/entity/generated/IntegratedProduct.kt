package ai.platon.exotic.services.api.entity.generated

import java.time.Instant
import javax.persistence.*

@Table(name = "integrated_products")
@Entity
class IntegratedProduct {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    var id: Long = 0

    @Column(name = "site", length = 256)
    var site: String? = null

    @Column(name = "big_img_url", length = 1024)
    var bigImgUrl: String? = null

    @Column(name = "product_name", length = 1024)
    var productName: String? = null

    @Column(name = "category_path", length = 256)
    var categoryPath: String? = null

    @Column(name = "brand", length = 256)
    var brand: String? = null

    @Column(name = "model", length = 256)
    var model: String? = null

    @Column(name = "specification", length = 256)
    var specification: String? = null

    @Column(name = "material", length = 256)
    var material: String? = null

    @Column(name = "price")
    var price: Double? = null

    @Column(name = "min_amount_to_buy")
    var minAmountToBuy: Int? = null

    @Column(name = "max_amount_to_buy")
    var maxAmountToBuy: Int? = null

    @Column(name = "inventory_amount")
    var inventoryAmount: Int? = null

    @Column(name = "sales_volume")
    var salesVolume: Int? = null

    @Column(name = "favorable_rate", length = 32)
    var favorableRate: String? = null

    @Column(name = "product_reviews", length = 64)
    var productReviews: String? = null

    @Column(name = "shop_name", length = 256)
    var shopName: String? = null

    @Column(name = "shop_url", length = 1024)
    var shopUrl: String? = null

    @Column(name = "shop_tel", length = 256)
    var shopTel: String? = null

    @Column(name = "shop_scores", length = 256)
    var shopScores: String? = null

    @Column(name = "delivery_from", length = 256)
    var deliveryFrom: String? = null

    @Column(name = "express_fee", length = 256)
    var expressFee: String? = null

    @Column(name = "stock_prompt")
    var stockPrompt: String? = null

    @Column(name = "uri", length = 1024)
    var uri: String = ""

    @Column(name = "create_time")
    var createTime: Instant? = null
}
