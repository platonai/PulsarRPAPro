package ai.platon.exotic.examples.sites.fashion.tommyjohn

import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.DateTimes
import ai.platon.pulsar.common.sql.SQLTemplate
import ai.platon.pulsar.ql.context.SQLContexts
import ai.platon.pulsar.test.ProductExtractor

fun main() {
    // BrowserSettings.withGUI()

    val indexSQL = """
        select
           dom_attr(dom, 'data-name') as `title`,
           dom_attr(dom, 'data-variant-price') as `list_Price`,
           dom_attr(dom, 'data-price') as `price`,
           dom_first_attr(dom, 'div.product-item__swatch-list', 'data-swatch-count') as `variant_Count`,
           dom_first_attr(dom, 'a[data-product-link] div[title~=star]', 'title') as `rating_Text`,
           str_substring_before(dom_first_attr(dom, 'a[data-product-link] div[title~=star]', 'title'), ' star') as `rating`,
           str_substring_between(dom_first_attr(dom, 'a[data-product-link] div[title~=star]', 'title'), 'with ', ' reviews') as `reviews`,
           dom_first_href(dom, 'a[data-product-link]') as `product_Link`,
           dom_base_uri(dom) as `base_Uri`
        from
           load_and_select(
                'https://tommyjohn.com/collections/loungewear-mens?sort-by=relevance&sort-order=descending
                    -i 1d -requireSize 500000 -netCond worse',
                'div[data-collection-entry] > div[data-product-id]'
           )
        """

    val itemsSQLTemplate = """
        select
           dom_first_text(dom, 'section.product-main h1.product-info__title') as `name`,
           dom_first_text(dom, 'section.product-main span.product-info__price') as `price`,
           dom_first_text(dom, 'section.product-main .yotpo-stars span:contains(star rating)') as `rating`,
           str_substring_between(dom_first_text(dom, 'section.product-main .yotpo-stars ~ a'), '(', ')') as `reviews`,

           dom_all_attrs(dom, 'div[data-color-section=Color] ul li input', 'value') as `color_Variants`,
           dom_all_texts(dom, 'div[data-option-name=Size] ul li') as `size_Variants`,

           dom_first_text(dom, 'div.product-details-container section[data-product-details-description]') as `product_Details`,
           dom_first_text(dom, 'div.product-page__options .product-description') as `description`,

           dom_first_attr(dom, 'div.product-gallery div[data-zoom-img]', 'data-zoom-img') as `big_Images`,
           dom_base_uri(dom) as `baseUri`
        from
           load_out_pages(
                '{{url}}
                    -i 1d -requireSize 500000 -itemRequireSize 250000 -ignoreFailure -netCond worst',
                'div.product-item a.product-meta:has(span.product-meta__title)'
           )
        """

    val reviewsSQLTemplate = """
        select
           dom_first_text(dom, 'p.reviewer-user-name') as `user_Name`,
           dom_first_text(dom, 'p.yotpo-review-buyer-data:contains(Date)') as `date`,
           dom_first_text(dom, 'p.yotpo-review-buyer-data:contains(Fit)') as `fit`,
           dom_first_text(dom, 'p.reviewer-user-type') as `user_Type`,

           dom_first_text(dom, 'span.yotpo-review-stars span:contains(rating)') as `rating`,
           dom_first_text(dom, 'p.yotpo-review-title') as `title`,
           dom_first_text(dom, 'div.yotpo-review-content') as `content`,
           dom_base_uri(dom) as `baseUri`
        from
           load_and_select(
                '{{url}} -i 1d -netCond worse',
                'ul.review-list li.review-item'
           )
        """

    val context = SQLContexts.create()

    val now = DateTimes.formatNow("HH")
    val path = AppPaths.getTmp("rs").resolve(now).resolve("tommy")
    val executor = ProductExtractor(path, context)
    val itemUrls = arrayOf(
        "https://tommyjohn.com/collections/loungewear-mens?sort-by=relevance&sort-order=descending",
        "https://tommyjohn.com/collections/mens-socks?sort-by=relevance&sort-order=descending",
        "https://tommyjohn.com/collections/mens-undershirts?sort-by=relevance&sort-order=descending",
        "https://tommyjohn.com/collections/mens-underwear-all-styles",
    )
    itemUrls.take(1).forEach { url ->
        val itemsSQL = SQLTemplate(itemsSQLTemplate).createInstance(url).sql
        executor.extract(itemsSQL, reviewsSQLTemplate)
    }
}
