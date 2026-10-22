# yshop-drink-uniapp-vue3

饮品点单（自取 / 外卖）uni-app 前端，基于 **Vue3 + uni-app（Vite 编译器）+ Pinia + uv-ui**。

> 源码位于 `src/`（适配 uni-app Vite 编译器默认源码根 `src/`）。
> 本工程以命令行（Vite CLI）方式运行；若需 HBuilderX 直接打开，请将 `src/` 内容移回根目录并相应调整 `index.html` 入口。

## 一、运行方式

```bash
# 1. 安装依赖（已配置项目级 .npmrc 覆盖全局 omit=dev，普通 npm install 即可装齐开发依赖）
npm install

# 2. 启动 H5 本地预览
npm run dev:h5

# 3. 构建 H5
npm run build:h5

# 4. 微信小程序（构建后用微信开发者工具导入 dist/build/mp-weixin）
npm run dev:mp-weixin
npm run build:mp-weixin
```

可用脚本：`dev:h5` / `build:h5` / `dev:mp-weixin` / `build:mp-weixin` / `dev:app` / `build:app`。

## 二、后端联调步骤（生产 / 真实数据）

业务数据来自后端 `yshop-drink-boot3`（Spring Boot）。**不启动后端时，所有页面为空或报错。**

1. 启动后端，使其监听 `http://localhost:48081`，并准备好数据库（见后端仓库说明）。
2. 确认 `src/config/index.js` 中 `VUE_APP_API_URL` 指向你的后端（默认 `http://localhost:48081/app-api`）。
3. 微信小程序 / 公众号：
   - 在 `src/config/index.js` 或 `.env` 中将 `VITE_WX_APP_ID` 替换为你自己的微信 AppID；
   - 公众号支付还需在微信公众平台配置 JS 安全域名、网页授权域名；支付参数由后端 `/member/wx-mp/create-jsapi-signature` 与 `/order/pay` 返回。
4. 静态资源（商品 / 分类图片）：默认资源域名为 `https://h5.yixiang.co/static`，由后端返回的图片 URL 拼接；自托管时改 `VITE_APP_RESOURCES_URL`。

## 三、本地 Mock（无需后端即可浏览验收）

设置 `.env`：

```ini
VITE_USE_MOCK=true
```

重新 `npm run dev:h5` 即可。Mock 会拦截全部 API 请求并返回与后端一致的 `{ code:0, data }`，
覆盖门店、商品、登录、订单、卡券、积分商城、地址、账单等，便于 UI / 流程验收。

> 说明：Mock 仅用于演示，下单 / 支付会直接走“Mock 支付成功”分支，不会真正调用第三方支付。

## 四、可配置项（.env）

复制 `.env.example` 为 `.env`（或 `.env.production`），支持：

| 变量 | 说明 | 默认 |
| --- | --- | --- |
| `VITE_APP_API_URL` | 后端 API 基地址（含 `/app-api`） | `http://localhost:48081/app-api` |
| `VITE_APP_RESOURCES_URL` | 静态资源（图片）域名前缀 | `https://h5.yixiang.co/static` |
| `VITE_WX_APP_ID` | 微信小程序 / 公众号 AppID | 示例 AppID，需自换 |
| `VITE_USE_MOCK` | 本地 Mock 开关 `true/false` | `false` |

## 五、已知限制 / 待办

- H5 微信公众号支付（JSAPI）已在 `pay.vue` 实现，依赖后端返回签名与支付参数，需在微信内实测。
- App 端微信支付（`trade_type=APP`）暂未实现（工程当前主要面向 H5 / 小程序）。
- `orderStatus` 枚举已按饮品点单场景校正（待支付 / 待接单 / 制作中 / 待取餐 / 配送中 / 已完成 / 退款中 / 已退款 / 已取消），若后端状态码不一致需同步调整 `src/config/index.js`。
