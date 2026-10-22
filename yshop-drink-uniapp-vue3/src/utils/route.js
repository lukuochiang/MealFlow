/**
 * 路由兼容层
 *
 * 当前工程采用分包路径（pages-checkout / pages-order / pages-user / pages-score），
 * 而后端/运营配置可能仍下发旧版工程路径（如 /pages/components/pages/address/address）。
 * 跳转前先做一次「旧路径 → 已注册页面」解析，避免 navigateTo:fail page not found。
 */

// 与 pages.json 保持一致的已注册页面清单
const REGISTERED_PAGES = [
  '/pages/index/index',
  '/pages/menu/menu',
  '/pages/order/order',
  '/pages/mine/mine',
  '/pages-checkout/pay/pay',
  '/pages-checkout/remark/remark',
  '/pages-checkout/packages/index',
  '/pages-checkout/shop/shop',
  '/pages-checkout/cart/cart',
  '/pages-order/orders/orders',
  '/pages-order/orders/detail',
  '/pages-order/orders/refund',
  '/pages-user/login/login',
  '/pages-user/login/logout',
  '/pages-user/address/address',
  '/pages-user/address/add',
  '/pages-user/coupons/coupons',
  '/pages-user/mine/userinfo',
  '/pages-user/mine/content',
  '/pages-user/balance/bill',
  '/pages-score/scoreproduct/list',
  '/pages-score/scoreproduct/detail',
  '/pages-score/scoreproduct/order',
  '/pages-score/scoreproduct/orderDetail',
  '/pages-score/scoreproduct/confirm',
]

// 无法按后缀匹配的特殊旧路径，显式映射
const ROUTE_ALIASES = {
  // 服务详情页未单独注册，复用内容页（入参一致：id + name）
  '/pages-user/mine/service': '/pages-user/mine/content',
}

function withQuery(path, query) {
  return query ? `${path}?${query}` : path
}

/**
 * 将任意（可能是旧版 / 后端下发的）页面路径解析为当前工程已注册的路径
 */
export function resolveRoute(url) {
  if (!url || typeof url !== 'string') return url

  const [rawPath, query = ''] = url.split('?')
  const path = (rawPath[0] === '/' ? rawPath : '/' + rawPath).replace(/\/+$/, '')

  // 已是合法路径，原样返回
  if (REGISTERED_PAGES.includes(path)) return url

  // 显式别名
  if (ROUTE_ALIASES[path]) return withQuery(ROUTE_ALIASES[path], query)

  // 按末两级目录匹配旧路径（如 .../address/address → /pages-user/address/address）
  const tail = path.split('/').slice(-2).join('/')
  const hit = REGISTERED_PAGES.find((p) => p.endsWith('/' + tail))
  if (hit) return withQuery(hit, query)

  console.warn(`[route] 未注册的页面路径: ${url}`)
  return url
}

/**
 * 页面跳转（自动兼容旧版路径）
 */
export function navigateTo(url) {
  uni.navigateTo({
    url: resolveRoute(url),
    fail: (err) => {
      console.warn('[route] navigateTo fail:', url, err)
    },
  })
}
