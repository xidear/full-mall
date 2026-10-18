# Mobile (uni-app)

该目录是 H5 / 微信小程序 / APP 的多端骨架。

## 开发

```bash
npm install
npm run dev:h5
npm run dev:mp-weixin
```

微信小程序需要在微信开发者工具中导入 `dist/dev/mp-weixin`，并在 `src/manifest.json` 填入真实 appid。H5 可直连本地 API；小程序需要在微信后台配置 request 合法域名。

## 注意

- 依赖固定为 DCloud `vue3` 发行版本 `3.0.0-alpha-5030220261016001`，Vite 固定 5.2.8；升级前必须重新验证 H5 和微信小程序构建。
- APP 端先用 uni-app 跑通业务；若原生体验成为瓶颈，可迁移 Flutter，API 契约不变。
- 当前页面已接入四套主题预设（商城/机械/清新/极简）和商品列表；购物车、结算、自提和客服将在后续阶段接入。