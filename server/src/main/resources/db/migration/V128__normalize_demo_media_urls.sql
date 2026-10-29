-- 演示数据里残留的本地绝对图片地址（http://localhost:8080/media/images/...）统一改为站内相对路径。
-- 只处理 localhost/127.0.0.1，生产环境的绝对 CDN 地址不受影响。
UPDATE sku
   SET image_url = regexp_replace(image_url, '^https?://(localhost|127\.0\.0\.1)(:[0-9]+)?/media(?=/)', '')
 WHERE image_url ~ '^https?://(localhost|127\.0\.0\.1)(:[0-9]+)?/media/';
