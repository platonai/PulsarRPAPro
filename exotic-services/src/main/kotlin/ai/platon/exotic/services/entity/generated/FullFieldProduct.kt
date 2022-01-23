package ai.platon.exotic.services.entity.generated

import java.time.Instant
import javax.persistence.*

@Table(name = "full_field_products")
@Entity
class FullFieldProduct {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    var id: Long = 0

    @Column(name = "uri", length = 1024)
    var uri: String? = null

    @Column(name = "site", length = 16)
    var site: String? = null

    @Column(name = "product_title", length = 1024)
    var productTitle: String? = null

    @Column(name = "product_name")
    var productName: String? = null

    @Column(name = "big_img_url", length = 1024)
    var bigImgUrl: String? = null

    @Lob
    @Column(name = "img_urls")
    var imgUrls: String? = null

    @Column(name = "category_path")
    var categoryPath: String? = null

    @Column(name = "brand", length = 64)
    var brand: String? = null

    @Column(name = "model", length = 64)
    var model: String? = null

    @Column(name = "model_raw_text", length = 1024)
    var modelRawText: String? = null

    @Column(name = "specification")
    var specification: String? = null

    @Column(name = "material")
    var material: String? = null

    @Column(name = "price", length = 64)
    var price: String? = null

    @Column(name = "price_raw_text")
    var priceRawText: String? = null

    @Column(name = "min_amount_to_buy", length = 8)
    var minAmountToBuy: String? = null

    @Column(name = "max_amount_to_buy", length = 8)
    var maxAmountToBuy: String? = null

    @Column(name = "sales_volume", length = 8)
    var salesVolume: String? = null

    @Column(name = "stock_prompt", length = 8)
    var stockPrompt: String? = null

    @Column(name = "inventory_amount", length = 8)
    var inventoryAmount: String? = null

    @Column(name = "favorable_rate", length = 8)
    var favorableRate: String? = null

    @Column(name = "good_reviews_raw_text", length = 16)
    var goodReviewsRawText: String? = null

    @Column(name = "good_reviews", length = 16)
    var goodReviews: String? = null

    @Column(name = "normal_reviews_raw_text", length = 16)
    var normalReviewsRawText: String? = null

    @Column(name = "normal_reviews", length = 16)
    var normalReviews: String? = null

    @Column(name = "bad_reviews_raw_text", length = 16)
    var badReviewsRawText: String? = null

    @Column(name = "bad_reviews", length = 16)
    var badReviews: String? = null

    @Column(name = "shop_name", length = 64)
    var shopName: String? = null

    @Column(name = "shop_url", length = 1024)
    var shopUrl: String? = null

    @Column(name = "shop_location")
    var shopLocation: String? = null

    @Column(name = "shop_tel", length = 8)
    var shopTel: String? = null

    @Lob
    @Column(name = "shop_scores")
    var shopScores: String? = null

    @Column(name = "shop_stars", length = 32)
    var shopStars: String? = null

    @Column(name = "delivery_from", length = 8)
    var deliveryFrom: String? = null

    @Column(name = "express_fee", length = 8)
    var expressFee: String? = null

    @Column(name = "express_fee_raw_text", length = 32)
    var expressFeeRawText: String? = null

    @Column(name = "comment_count", length = 8)
    var commentCount: String? = null

    @Column(name = "comment_count_raw_text", length = 32)
    var commentCountRawText: String? = null

    @Column(name = "coupon", length = 64)
    var coupon: String? = null

    @Column(name = "coupon_comment", length = 64)
    var couponComment: String? = null

    @Lob
    @Column(name = "promotion")
    var promotion: String? = null

    @Column(name = "delivery_by", length = 128)
    var deliveryBy: String? = null

    @Lob
    @Column(name = "summary_service")
    var summaryService: String? = null

    @Lob
    @Column(name = "services")
    var services: String? = null

    @Lob
    @Column(name = "variants")
    var variants: String? = null

    @Lob
    @Column(name = "base_uri")
    var baseUri: String? = null

    @Column(name = "create_time")
    var createTime: Instant? = null
}
