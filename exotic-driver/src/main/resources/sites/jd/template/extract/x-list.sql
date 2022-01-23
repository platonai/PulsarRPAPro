-- noinspection SqlDialectInspectionForFile
-- noinspection SqlResolveForFile
-- noinspection SqlNoDataSourceInspectionForFile

select
       dom_first_text(dom, '.p-name a i') as title,
       dom_first_text(dom, '.p-price') as priceText,
       dom_first_float(dom, '.p-price i', 0.0) as price,
       dom_first_text(dom, '.p-shop a') as shop,
       dom_first_href(dom, '.p-shop a') as shopUrl,
       dom_first_href(dom, 'div.p-name a[title]') as href
from
    load_and_select('{{url}}', '#J_goodsList li[data-sku]');
