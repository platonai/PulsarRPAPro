-- noinspection SqlDialectInspectionForFile
-- noinspection SqlResolveForFile
-- noinspection SqlNoDataSourceInspectionForFile

select
    dom_uri(dom) as uri,
    '京东' as site,
    dom_first_text(dom, '.itemInfo-wrap .sku-name') as product_title,
    dom_first_text(dom, '#crumb-wrap .crumb') as category_path,
    dom_all_slim_htmls(dom, '#crumb-wrap .crumb a') as categories,
    str_substring_after(dom_first_text(dom, '.p-parameter .parameter2 li:contains(商品名称)'), '：') as product_name,
    dom_first_img(dom, '.product-intro .preview img#spec-img') as big_img_url,
    dom_all_imgs(dom, '.product-intro .preview .spec-list .spec-items') as img_urls,
    dom_first_text(dom, '.p-parameter #parameter-brand a') as brand,
    array_first_not_blank(make_array(
        str_substring_after(dom_first_text(dom, '.p-parameter .parameter2 li:contains(货号)'), '：'),
        dom_first_text(dom, '.detail .Ptable-item dt:contains(型号) ~ dd')
        )) as model,
    make_array(
        dom_first_text(dom, '.detail .Ptable-item dl:contains(型号)'),
        dom_first_text(dom, '.p-parameter .parameter2 li:contains(货号)')
        ) as model_raw_text,
    dom_first_text(dom, '#choose-attrs #choose-attr-1 div.item.selected a i') as specification,
    str_substring_after(dom_first_text(dom, '.p-parameter .parameter2 li:contains(材质)'), '：') as material,
    dom_first_float(dom, '.itemInfo-wrap .p-price .price', 0.0) as price,
    dom_first_text(dom, '.itemInfo-wrap .p-price .price') as price_raw_text,
    dom_first_attr(dom, '.choose-amount input', 'value') as min_amount_to_buy,
    dom_first_attr(dom, '.choose-amount input', 'data-max') as max_amount_to_buy,
    -1 as sales_volume,
    dom_first_text(dom, '.itemInfo-wrap .summary .summary-stock #store-prompt') as stock_prompt,
    -1 as inventory_amount,
    dom_first_text(dom, '.detail .comment-percent .percent-con') as favorable_rate,

    dom_first_text(dom, '.detail .comments-list li a:contains(好评)') as good_reviews_raw_text,
    str_first_float(dom_first_text(dom, '.detail .comments-list li a:contains(好评)'), 0) as good_reviews,

    dom_first_text(dom, '.detail .comments-list li a:contains(中评)') as normal_reviews_raw_text,
    str_first_float(dom_first_text(dom, '.detail .comments-list li a:contains(中评)'), 0) as normal_reviews,

    dom_first_text(dom, '.detail .comments-list li a:contains(差评)') as bad_reviews_raw_text,
    str_first_float(dom_first_text(dom, '.detail .comments-list li a:contains(差评)'), 0) as bad_reviews,

    dom_first_text(dom, '.crumb-wrap .contact .name a[href]') as shop_name,
    dom_first_href(dom, '.crumb-wrap .contact .name a') as shop_url,
    'unknown' as shop_location,
    'unknown' as shop_tel,
    dom_first_text(dom, '.pop-score-detail') as shop_scores,
    dom_first_attr(dom, '.crumb-wrap .contact .star .star-gray', 'title') as shop_stars,
    str_substring_between(dom_first_text(dom, '.itemInfo-wrap #summary-supply #summary-service'), '从 ', ' 发货, ') as delivery_from,
    str_substring_between(dom_first_text(dom, '.itemInfo-wrap .summary .summary-stock .dcashDesc'), '运费', '元') as express_fee,
    dom_first_text(dom, '.itemInfo-wrap .summary .summary-stock .dcashDesc') as express_fee_raw_text,

    dom_first_float(dom, '.itemInfo-wrap #comment-count a', 0.0) as comment_count,
    dom_first_text(dom, '.itemInfo-wrap #comment-count a') as comment_count_raw_text,
    dom_first_text(dom, '.itemInfo-wrap #summary-quan .quan-item') as coupon,
    dom_first_attr(dom, '.itemInfo-wrap #summary-quan .quan-item', 'title') as coupon_comment,
    dom_first_text(dom, '.itemInfo-wrap #summary-promotion') as promotion,
    dom_first_text(dom, '.itemInfo-wrap #summary-service a') as delivery_by,
    dom_first_text(dom, '.itemInfo-wrap #summary-supply #summary-service') as summary_service,
    dom_first_text(dom, '.itemInfo-wrap .services') as services,
    dom_all_texts(dom, '.itemInfo-wrap #choose-attrs #choose-attr-1 a') as variants,
    dom_base_uri(dom) as base_uri
from
    load_and_select('{{url}}', 'body');
