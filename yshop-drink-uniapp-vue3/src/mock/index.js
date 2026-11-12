/**
 * 本地 Mock 数据层（仅当 VITE_USE_MOCK=true 时启用）
 *
 * 通过替换 flyio 的 adapter，拦截全部 API 请求并返回与后端一致的
 * { code: 0, data } 结构，使项目在无后端的情况下也能启动、浏览与验收 UI。
 *
 * 修改开关：根目录 .env 中 VITE_USE_MOCK=true
 */
import { APP_ID } from '@/config'

// 使用本地已存在的静态图，避免 Mock 下图片裂图
const IMG = '/static/images/new002.jpg'
const ICON = '/static/images/new003.jpg'
const STORE_IMG = '/static/images/index003.png'
const AVATAR = '/static/images/mine/default.png'
const BANNER_1 = '/static/images/banner1.png'
const BANNER_2 = '/static/images/banner2.png'
const BANNER_3 = '/static/images/banner3.png'

/* ---------------- 示例数据 ---------------- */

function makeProduct(id, name, price, desc) {
  const attrSpec = ['中杯', '大杯']
  const attrTemp = ['热', '冰', '去冰']
  // 构造与后端一致的 SKU 映射：productValue["规格,温度"] = { price, stock, image }
  const productValue = {}
  attrSpec.forEach((s) => {
    attrTemp.forEach((t) => {
      productValue[`${s},${t}`] = { price: String(price), stock: 99, image: IMG }
    })
  })
  return {
    id,
    storeName: name,
    storeInfo: desc,
    image: IMG,
    price,
    stock: 99,
    description: desc,
    sales: Math.floor(Math.random() * 500),
    cartNum: 0,
    productAttr: [
      { attrName: '规格', attrValueArr: attrSpec },
      { attrName: '温度', attrValueArr: attrTemp },
    ],
    productValue,
  }
}

const demoStore = {
  id: 1,
  name: '演示饮品门店',
  notice: '欢迎光临，全场饮品 8 折起',
  status: 1,
  startTime: '2024-01-01 09:00:00',
  endTime: '2024-01-01 22:00:00',
  image: STORE_IMG,
  deliveryPrice: 5,
  min_price: 20,
  distance: 2,
  dis: 2,
  far: 5,
  address: '示例市示例区示例路 1 号',
  phone: '400-000-0000',
}

const demoGoods = [
  {
    id: 1,
    name: '招牌茶饮',
    icon: ICON,
    goodsList: [
      makeProduct(101, '珍珠奶茶', 16, '经典珍珠奶茶，香浓顺滑'),
      makeProduct(102, '葡萄柚茶', 18, '清爽葡萄柚，微酸回甘'),
      makeProduct(103, '芝士莓莓', 22, '草莓 + 芝士奶盖'),
    ],
  },
  {
    id: 2,
    name: '咖啡',
    icon: ICON,
    goodsList: [
      makeProduct(201, '生椰拿铁', 20, '椰香浓郁'),
      makeProduct(202, '美式咖啡', 15, '清爽不苦'),
    ],
  },
  {
    id: 3,
    name: '果茶',
    icon: ICON,
    goodsList: [
      makeProduct(301, '满杯红柚', 19, '红柚 + 绿茶'),
      makeProduct(302, '柠檬绿茶', 14, '夏日解腻'),
    ],
  },
]

const demoMember = {
  id: 1,
  nickname: '演示用户',
  avatar: AVATAR,
  mobile: '13800000000',
  couponCount: 3,
  integral: 500,
  nowMoney: 100,
  sumMoney: 320,
  createTime: '2024-01-01 10:00:00',
}

const demoServices = [
  { id: 1, name: '我的卡券', type: 'pages', pages: '/pages-user/coupons/coupons', image: ICON },
  // 模拟后端下发的旧版工程路径，验证路由兼容层能正确解析到当前分包页面
  { id: 2, name: '收货地址', type: 'pages', pages: '/pages/components/pages/address/address', image: ICON },
  { id: 3, name: '历史订单', type: 'pages', pages: '/pages/components/pages/orders/orders', image: ICON },
  { id: 4, name: '在线客服', type: 'contact', image: ICON },
  { id: 5, name: '客服电话', type: 'call', phone: '400-123-4567', image: ICON },
  { id: 6, name: '关于我们', type: 'content', image: ICON },
  { id: 7, name: '积分说明', type: 'content', image: ICON },
]

const demoAds = {
  list: [
    { id: 1, image: BANNER_1, url: '' },
    { id: 2, image: BANNER_2, url: '' },
    { id: 3, image: BANNER_3, url: '' },
  ],
  isActive: true,
}

const demoBills = [
  { id: 1, title: '余额消费', mark: '订单支付', pm: 1, number: 32, createTime: '2024-01-01 12:00' },
  { id: 2, title: '账户充值', mark: '微信充值', pm: 0, number: 100, createTime: '2024-01-02 09:30' },
]

const demoCoupons = [
  { id: 1, name: '满20减5', amount: 5, least: 20, endTime: '2024-12-31' },
  { id: 2, name: '满50减12', amount: 12, least: 50, endTime: '2024-12-31' },
]

const demoScoreProducts = [
  { id: 1, name: '奈雪券·20元', image: IMG, score: 200, price: 20, description: '兑换 20 元饮品券', stock: 99, sales: 120 },
  { id:2, name: '周边帆布袋', image: IMG, score: 500, price: 0, description: '限量帆布袋', stock: 50, sales: 30 },
  { id:3, name: '会员月卡', image: IMG, score: 800, price: 0, description: '30 天会员权益', stock: 20, sales: 10 },
]

const demoOrders = [
  {
    orderId: 'O20240101001',
    status: 1,
    createTime: '2024-01-01 12:10',
    totalPrice: 32,
    payPrice: 32,
    type: 0,
    storeName: '演示饮品门店',
    productList: [{ name: '珍珠奶茶', image: IMG, number: 2, price: 16 }],
  },
  {
    orderId: 'O20240101002',
    status: 5,
    createTime: '2024-01-02 18:00',
    totalPrice: 20,
    payPrice: 20,
    type: 1,
    storeName: '演示饮品门店',
    productList: [{ name: '生椰拿铁', image: IMG, number: 1, price: 20 }],
  },
]

const demoAddresses = [
  { id: 1, realName: '张三', phone: '13800000000', address: '示例市示例区', detail: '示例路 1 号', isDefault: true },
]

const demoRechargeList = [
  { id: 1, price: 50, give: 5 },
  { id: 2, price: 100, give: 12 },
]

const demoScoreOrders = [
  {
    orderId: 'S20240101001',
    status: 5,
    createTime: '2024-01-01 12:10',
    totalScore: 200,
    productList: [{ name: '奈雪券·20元', image: IMG, number: 1 }],
  },
]

function buildOrderDetail(orderId, status) {
  return {
    orderId,
    status,
    createTime: '2024-01-01 12:10',
    totalPrice: 32,
    payPrice: 32,
    type: 0,
    storeName: '演示饮品门店',
    address: '示例市示例区示例路 1 号',
    phone: '13800000000',
    takeinTime: '立即用餐',
    payType: 'weixin',
    remark: '',
    productList: [{ name: '珍珠奶茶', image: IMG, number: 2, price: 16 }],
  }
}

/* ---------------- 路由 ---------------- */

function ok(data) {
  return Promise.resolve({
    data: { code: 0, msg: 'success', data },
    status: 200,
    statusText: 'OK',
    headers: {},
  })
}

function mockRoute(method, path, body) {
  const GET = (p) => method === 'GET' && path === p
  const POST = (p) => method === 'POST' && path === p

  // 商品 / 门店 / 广告
  if (GET('/product/products')) return ok(demoGoods)
  if (GET('/store/nearby')) return ok(demoStore)
  if (GET('/store/list')) return ok([demoStore])
  if (GET('/ad/list')) return ok(demoAds)

  // 订单
  if (GET('/order/list')) return ok(demoOrders)
  if (POST('/order/create')) return ok({ orderId: 'O' + Date.now() })
  if (GET('/order/detail')) {
    const m = path.match(/^\/order\/detail\/(\w+)/)
    return ok(buildOrderDetail(m ? m[1] : 'O20240101001', 1))
  }
  if (POST('/order/take')) return ok({})
  if (POST('/order/refund')) return ok({})
  // 支付：Mock 下直接返回成功标记，由 pay.vue 走 Mock 成功分支
  if (POST('/order/pay')) return ok({ trade_type: 'MOCK_SUCCESS', data: {} })

  // 登录 / 授权
  if (POST('/member/auth/send-sms-code')) return ok({})
  if (POST('/member/auth/sms-login')) {
    return ok({ userInfo: demoMember, accessToken: 'mock-token-' + Date.now() })
  }
  if (POST('/member/auth/auth-miniapp-login')) {
    return ok({ userInfo: demoMember, accessToken: 'mock-token-' + Date.now() })
  }
  if (POST('/member/auth/auth-session')) {
    return ok({ openId: 'mock-openid', userInfo: demoMember, accessToken: 'mock-token' })
  }
  if (GET('/member/auth/auth-wechat-login')) {
    return ok({ openId: 'mock-openid', userInfo: demoMember, accessToken: 'mock-token' })
  }
  if (GET('/member/wx-mp/create-jsapi-signature')) {
    return ok({ appId: APP_ID, timestamp: String(Date.now()), nonceStr: 'mock', signature: 'mock' })
  }

  // 用户
  if (GET('/member/user/get-info')) return ok(demoMember)
  if (POST('/member/user/update-nickname')) return ok({})
  if (GET('/member/user/getBill')) return ok(demoBills)
  if (GET('/recharge/getMoneyList')) return ok(demoRechargeList)
  if (POST('/member/user/recharge')) return ok({})

  // 服务 / 内容
  if (GET('/service/list')) return ok(demoServices)
  if (GET('/service/content')) return ok({ content: '<p>这是一段演示内容。</p>' })

  // 卡券
  if (GET('/coupon/my')) return ok(demoCoupons)
  if (GET('/coupon/not')) return ok(demoCoupons)
  if (GET('/coupon/count')) return ok(2)
  if (POST('/coupon/receive')) return ok({})

  // 地址
  if (GET('/address/list')) return ok(demoAddresses)
  if (POST('/address/addAndEdit')) return ok({})
  if (POST('/address/del')) return ok({})
  if (POST('/address/getDistanceFromLocation')) return ok({ distance: 1.2 })

  // 积分商城
  if (GET('/score-product/list')) return ok(demoScoreProducts)
  if (GET('/score-product/detail')) return ok(demoScoreProducts[0])
  if (POST('/score-order/submit')) return ok({ orderId: 'S' + Date.now() })
  if (GET('/score-order/list')) return ok(demoScoreOrders)
  if (GET('/score-order/detail')) return ok(demoScoreOrders[0])
  if (GET('/score-order/take')) return ok({})
  if (GET('/express/getLogistic')) return ok({})

  // 未匹配：返回空对象，避免页面崩溃
  return ok({})
}

/* ---------------- flyio adapter ---------------- */

export function mockAdapter(request) {
  const url = request.url || ''
  const method = (request.method || 'GET').toUpperCase()

  let path = url.replace(/^https?:\/\/[^/]+/, '')
  path = path.split('?')[0]
  path = path.replace(/^\/app-api/, '')
  if (!path) path = '/'

  let body = request.body
  if (typeof body === 'string') {
    try {
      body = JSON.parse(body)
    } catch (e) {
      body = {}
    }
  }
  body = body || {}

  return mockRoute(method, path, body)
}
