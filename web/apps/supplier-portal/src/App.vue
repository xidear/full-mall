<script setup lang="ts">
const endpoints = [
  { method: 'POST', path: '/open/v1/products/batch-upsert', desc: '商品/规格批量 upsert，外部编码映射' },
  { method: 'POST', path: '/open/v1/inventory/feed', desc: '库存全量/增量同步，支持安全库存' },
  { method: 'GET', path: '/supplier/v1/orders', desc: '拉取待接单/待发货订单' },
  { method: 'POST', path: '/open/v1/orders/{id}/accept', desc: '接单，返回承诺发货时间' },
  { method: 'POST', path: '/open/v1/orders/{id}/ship', desc: '回传承运商与运单号' },
  { method: 'POST', path: '/open/v1/orders/{id}/cancel', desc: '缺货/拒单并说明原因' }
]
</script>

<template>
  <div class="supplier-shell">
    <header class="supplier-header">
      <div>
        <span class="supplier-mark">FM</span>
        <div>
          <strong>供应商工作台</strong>
          <small>API 推送 · 库存同步 · 代发履约</small>
        </div>
      </div>
      <span class="supplier-mode">Phase 4 接口骨架</span>
    </header>

    <main class="supplier-main">
      <section class="hero">
        <p class="eyebrow">Supplier API v1</p>
        <h1>把商品、库存和代发订单接进商城</h1>
        <p>
          支持 API 推送和 CSV/Excel 上传。所有开放接口使用 HMAC 签名、时间戳、nonce、幂等键和限流；
          平台向供应商推送订单和状态变更时使用签名 Webhook。
        </p>
      </section>

      <section class="panel">
        <h2>开放接口</h2>
        <table class="endpoint-table">
          <thead>
            <tr>
              <th>方法</th>
              <th>路径</th>
              <th>用途</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="endpoint in endpoints" :key="endpoint.path">
              <td><span class="method" :class="endpoint.method.toLowerCase()">{{ endpoint.method }}</span></td>
              <td class="path">{{ endpoint.path }}</td>
              <td>{{ endpoint.desc }}</td>
            </tr>
          </tbody>
        </table>
      </section>

      <section class="grid">
        <article class="panel">
          <h2>接入步骤</h2>
          <ol>
            <li>在供应商后台生成 API Key，保存 Secret。</li>
            <li>用商品接口上传 SPU/SKU，并建立供应商 SKU 映射。</li>
            <li>定时推送库存与价格，设置安全库存。</li>
            <li>接单、发货、回传运单号；超时订单自动升级。</li>
            <li>每日对账：库存差异、拒单、轨迹延迟。</li>
          </ol>
        </article>
        <article class="panel">
          <h2>安全与幂等</h2>
          <ul>
            <li>HMAC-SHA256 签名，签名覆盖 method + path + timestamp + nonce + body。</li>
            <li>幂等键至少保留 24 小时；重复请求返回同一结果。</li>
            <li>库存 feed 支持全量与增量，冲突按更新时间优先。</li>
            <li>Webhook 失败指数退避重试，保留投递日志和人工重放。</li>
          </ul>
        </article>
      </section>
    </main>
  </div>
</template>