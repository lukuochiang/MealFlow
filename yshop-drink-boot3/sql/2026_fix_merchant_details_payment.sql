-- ============================================================
-- 移动支付上线前数据修正（对应支付补全方案 P0 阻塞项）
-- 作用：修正 merchant_details 的异步回调地址、证书路径与演示密钥
-- 用法：在已导入 yixiang-drink-open.sql 的库上执行本文件
-- 重要：
--   1) 将下面所有 <DOMAIN> 替换为你的真实部署域名。该域名必须与小程序/
--      前端配置的服务器域名一致，且已在微信支付/支付宝后台配置为授权域名。
--   2) 回调路径必须是 /app-api/order/notify/payBack{details_id}.json
--      （AppOrderController 的路由为 /notify/payBack{detailsId}.json，
--       且 /app-api/** 已在安全白名单 permitAll，无需额外放行）。
--   3) wx_miniapp 的证书路径与 APIv3 密钥必须替换为真实商户配置
--      （cert_store_type=PATH 时需要真实 p12 服务器路径；也可改为 STR 内联证书）。
--   4) ali_* 行的 RSA 公私钥为示例值，上线前也需替换为真实商户密钥。
-- ============================================================

-- ① 全量修正异步回调地址（原数据多数域名错/缺 /notify/ 段/detailsId 不匹配）
UPDATE merchant_details SET notify_url = 'https://<DOMAIN>/app-api/order/notify/payBackali_app.json'    WHERE details_id = 'ali_app';
UPDATE merchant_details SET notify_url = 'https://<DOMAIN>/app-api/order/notify/payBackali_h5.json'    WHERE details_id = 'ali_h5';
UPDATE merchant_details SET notify_url = 'https://<DOMAIN>/app-api/order/notify/payBackwx_h5.json'     WHERE details_id = 'wx_h5';
UPDATE merchant_details SET notify_url = 'https://<DOMAIN>/app-api/order/notify/payBackwx_miniapp.json' WHERE details_id = 'wx_miniapp';
UPDATE merchant_details SET notify_url = 'https://<DOMAIN>/app-api/order/notify/payBackwx_wechat.json' WHERE details_id = 'wx_wechat';

-- ② 替换 wx_miniapp 本地证书路径与演示密钥（/Users/hupeng/cert/... 部署必失败）
UPDATE merchant_details
   SET key_private = '<WECHAT_API_V3_KEY>',
       key_cert   = '<SERVER_PATH_TO_APICLIENT_CERT_P12>'
 WHERE details_id = 'wx_miniapp';

-- ②-补充：微信 APP 支付所需商户配置（原库缺失 wx_app）。
--   appid 需替换为「微信开放平台-移动应用」的 appid；mch_id 与证书同主商户（与 wx_miniapp 一致）。
--   列顺序：details_id,pay_type,appid,mch_id,cert_store_type,key_private,key_public,key_cert,key_cert_pwd,
--           notify_url,return_url,sign_type,seller,sub_app_id,sub_mch_id,input_charset,is_test,updater,creator,create_time,update_time,deleted
INSERT INTO merchant_details (details_id, pay_type, appid, mch_id, cert_store_type, key_private, key_public, key_cert, key_cert_pwd, notify_url, return_url, sign_type, seller, sub_app_id, sub_mch_id, input_charset, is_test, updater, creator, create_time, update_time, deleted)
VALUES ('wx_app', 'wxPay', '<WX_APP_APPID>', '1493624002', 'PATH', '<WECHAT_API_V3_KEY>', NULL, '<SERVER_PATH_TO_APICLIENT_CERT_P12>', '1493624002', 'https://<DOMAIN>/app-api/order/notify/payBackwx_app.json', '', 'MD5', NULL, NULL, NULL, 'UTF-8', 0, '1', NULL, NULL, NULL, b'0');
