/**
 * 全局配置（API 地址 / 资源域名 / 微信 AppID / 订单状态枚举）
 *
 * 所有变量均可通过项目根目录的 .env / .env.production 覆盖，例如：
 *   VITE_APP_API_URL=https://your-domain/app-api
 *   VITE_APP_RESOURCES_URL=https://your-cdn/static
 *   VITE_WX_APP_ID=wxYourAppId
 *   VITE_USE_MOCK=true     # 无需后端即可浏览验收
 */
const ENV = import.meta.env || {}

const isProd = ENV.PROD === true

// 后端 API 基地址（含 /app-api）
export const VUE_APP_API_URL =
  ENV.VITE_APP_API_URL ||
  (isProd ? 'https://apidc.yixiang.co/app-api' : 'http://localhost:48081/app-api')

// 静态资源域名（商品/分类等图片前缀，由后端返回的图片 URL 拼接使用）
export const VUE_APP_RESOURCES_URL = ENV.VITE_APP_RESOURCES_URL || 'https://h5.yixiang.co/static'

// 文件上传地址
export const VUE_APP_UPLOAD_URL = VUE_APP_API_URL + '/infra/file/upload'

// 微信小程序 / 公众号 AppID（请替换为你自己的账号）
export const APP_ID = ENV.VITE_WX_APP_ID || 'wxdbdbc123c8c30b45'

// 是否启用本地 Mock（无需后端即可浏览验收）：.env 中设置 VITE_USE_MOCK=true
export const USE_MOCK = ENV.VITE_USE_MOCK === 'true' || ENV.VITE_USE_MOCK === true

// 饮品点单（自取 / 外卖）订单状态枚举
// 0:待支付 1:待接单 2:制作中 3:待取餐/配送 4:配送中 5:已完成 6:退款中 7:已退款 8:已取消
export const orderStatus = {
  0: '待支付',
  1: '待接单',
  2: '制作中',
  3: '待取餐',
  4: '配送中',
  5: '已完成',
  6: '退款中',
  7: '已退款',
  8: '已取消',
}

// 订单详情中的反向状态文案（与后端返回的状态码保持一致）
export const orderReStatus = {
  0: '等待买家付款',
  1: '商家已接单',
  2: '饮品制作中',
  3: '等待买家取餐 / 收货',
  4: '订单配送中',
  5: '订单已完成',
  6: '订单退款中',
  7: '已退款',
  8: '已取消',
}
