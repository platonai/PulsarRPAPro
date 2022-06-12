select
  dom_location(dom) as `Url`,
  dom_first_text(dom, '#productTitle') as `Title`,
  dom_first_text(dom, '#wayfinding-breadcrumbs_container') as `Breadcrumbs`,
  dom_all_slim_htmls(dom, '#wayfinding-breadcrumbs_container ul li a') as `Breadcrumbs anchors`,
  dom_all_hrefs(dom, '#wayfinding-breadcrumbs_container ul li a') as `Breadcrumbs links`,
  dom_first_text(dom, 'a#bylineInfo') as `Brand`,
  dom_first_href(dom, 'a#bylineInfo') as `Brand link`,
  dom_all_slim_htmls(dom, '#imageBlock img') as `Gallery`,
  dom_first_slim_html(dom, '#imageBlock img:expr(width > 400)') as `Main image`,
  dom_first_text(dom, '#price tr td:contains(List Price) ~ td') as `List price`,
  dom_first_text(dom, '#price tr td:matches(^Price) ~ td') as `Price`,
  dom_first_text(dom, '#price tr td:contains(You Save) ~ td') as `You save`,
  dom_sib(dom_select_first(dom, '#variation_color_name ul li')) as `Variation number`,
  dom_all_attrs(dom, '#variation_color_name ul li', 'data-defaultasin') as `Variation asins`,
  dom_all_attrs(dom, '#variation_color_name ul li', 'abs:data-dp-url') as `Variation links`,
  dom_first_text(dom, '#acBadge_feature_div') as `Product badges`,
  dom_first_text(dom, '#acBadge_feature_div i:contains(Best Seller)') as `Badge Best seller`,
  dom_first_text(dom, '#acBadge_feature_div span:contains(Amazon)') as `Badge Amazon's choice`,

  -- Seller info
  dom_first_text(dom, '#buybox div:contains(Sold by), #usedbuyBox div:contains(Sold by)') as `Sold by`,
  dom_first_slim_html(dom, '#buybox div:contains(Sold by) a, #usedbuyBox div:contains(Sold by) a') as `Sold by anchor`,
  dom_first_href(dom, '#buybox div:contains(Sold by) a, #usedbuyBox div:contains(Sold by) a') as `Sold by link`,

  dom_first_text(dom, '#desktop_buybox #merchant-info') as `Ship from`,
  dom_first_text(dom, '#availability') as `Availability in stock`,
  dom_first_slim_html(dom, '#availability a') as `Availability anchor`,
  dom_all_hrefs(dom, '#availability a') as `Available sellers links`,
  dom_first_text(dom, '#desktop_buybox #contextualIngressPtLabel_deliveryShortLine') as `Delivery`,

  -- New/Renewed/New & Used/Other sellers (跟卖商品: 新品/升级/新品 & 二手/其他卖家)
  dom_first_text(dom, '#olp-upd-new-used a') as `New & used sellers`,
  dom_first_slim_html(dom, '#olp-upd-new-used a') as `New & used sellers anchor`,
  dom_first_href(dom, '#olp-upd-new-used a') as `New & used sellers link`,
  str_substring_between(dom_first_text(dom, '#olp-upd-new-used a'), '(', ')') as `New & used sellers number`,
  str_substring_after(dom_first_text(dom, '#olp-upd-new-used a'), ' from ') as `New & used lowest price`,

  dom_first_text(dom, '#olp-upd-new a') as `New sellers`,
  dom_first_slim_html(dom, '#olp-upd-new a') as `New sellers anchor`,
  dom_first_href(dom, '#olp-upd-new a') as `New sellers link`,
  str_substring_between(dom_first_text(dom, '#olp-upd-new a'), '(', ')') as `New sellers number`,
  str_substring_after(dom_first_text(dom, '#olp-upd-new a'), ' from ') as `New sellers lowest price`,

  -- "This product is available as Renewed.", "该商品以续订形式提供。"
  dom_first_text(dom, '#certified-refurbished-version') as `Renewed`,
  dom_first_text(dom, '#certified-refurbished-version .a-color-price') as `Renewed price`,
  dom_first_href(dom, '#certified-refurbished-version a[href~=/dp/]') as `Renewed link`,
  dom_first_text(dom, '#newer-version') as `Available as newer version`,
  dom_first_text(dom, '#newer-version .a-color-price') as `Newer version price`,
  dom_first_href(dom, '#newer-version a[href~=/dp/]') as `Newer version link`,

  dom_first_text(dom, '#moreBuyingChoices_feature_div') as `More sellers`,
  dom_first_text(dom, '#mbc-sold-by-1') as `More seller 1`,
  dom_first_text(dom, '#mbc-price-1') as `More seller 1 price`,
  dom_first_text(dom, '#mbc-sold-by-2') as `More seller 2`,
  dom_first_text(dom, '#mbc-price-2') as `More seller 2 price`,

  -- Buy button
  dom_first_text(dom, '#addToCart_feature_div span:contains(Add to Cart)') as `Add to cart`,
  dom_first_text(dom, '#buyNow span:contains(Buy now)') as `Buy now`,

  -- Product details
  dom_first_slim_html(dom, '#feature-bullets ul') as `Feature bullets`,
  dom_first_text(dom, '#productDescription') as `Product description`,
  dom_all_texts(dom, '#prodDetails table tr th:eq(0), #prodDetails table tr td:eq(0)') as `Product attribute names`,
  dom_all_slim_htmls(dom, '#prodDetails table tr') as `Product attributes`,
  dom_all_slim_htmls(dom, '#prodDetails h1:contains(Feedback) ~ div a') as `Feedback links`,
  dom_first_text(dom, '#prodDetails table tr th:contains(ASIN) ~ td') as `ASIN`,
  dom_first_text(dom, '#prodDetails table tr th:contains(Manufacturer) ~ td') as `Manufacturer`,
  dom_first_text(dom, '#prodDetails table tr th:contains(Product Dimensions) ~ td') as `Product dimensions`,
  dom_first_text(dom, '#prodDetails table tr th:contains(Item Weight) ~ td') as `Item weight`,
  dom_first_text(dom, '#prodDetails table tr th:contains(Shipping Weight) ~ td') as `Shipping weight`,
  dom_first_text(dom, '#prodDetails table tr th:contains(Customer Reviews) ~ td') as `Customer reviews`,
  dom_first_text(dom, '#prodDetails table tr th:contains(Best Sellers Rank) ~ td') as `Best sellers rank`,
  dom_first_text(dom, '#prodDetails table tr th:contains(Date First) ~ td') as `Date on sale`,
  dom_first_text(dom, '#productDescription') as `Product description`,
  dom_all_imgs(dom, 'img:expr(left >= 10 && top >= 500 && width >= 400 && height >= 200)') as `Big images`,

  -- Answered questions
  dom_first_text(dom, 'a#askATFLink span') as `Answered questions`,
  dom_first_href(dom, 'a#askATFLink') as `Answered questions link`,
  str_first_integer(dom_first_text(dom, 'a#askATFLink span'), 0) as `Answered questions number`,

  -- Customer reviews
  dom_first_text(dom, '#reviewsMedley .AverageCustomerReviews span:contains(out of)') as `Customer reviews - average reviews text`,
  str_first_float(dom_first_text(dom, '#reviewsMedley .AverageCustomerReviews span:contains(out of)'), 0.0) as `Customer reviews - average reviews`,
  dom_first_text(dom, '#reviewsMedley div span:contains(customer ratings)') as `Customer reviews - ratings`,
  str_substring_before(dom_first_text(dom, '#reviewsMedley div span:contains(customer ratings)'), ' customer ratings') as `Customer reviews - number of ratings`,
  dom_all_slim_htmls(dom, 'table#histogramTable:expr(width > 100) tr') as `Customer reviews histogram`,
  dom_first_text(dom, 'table#histogramTable:expr(width > 100) td:contains(5 star) ~ td:contains(%)') as `Customer reviews 5 star`,
  dom_first_text(dom, 'table#histogramTable:expr(width > 100) td:contains(4 star) ~ td:contains(%)') as `Customer reviews 4 star`,
  dom_first_text(dom, 'table#histogramTable:expr(width > 100) td:contains(3 star) ~ td:contains(%)') as `Customer reviews 3 star`,
  dom_first_text(dom, 'table#histogramTable:expr(width > 100) td:contains(2 star) ~ td:contains(%)') as `Customer reviews 2 star`,
  dom_first_text(dom, 'table#histogramTable:expr(width > 100) td:contains(1 star) ~ td:contains(%)') as `Customer reviews 1 star`,

  dom_first_slim_html(dom, '#reviews-medley-footer a') as `Customer reviews all reviews`,
  dom_first_href(dom, '#reviews-medley-footer a') as `Customer reviews all reviews link`
from load_and_select('{{url}}', ':root');