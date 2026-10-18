export const messages = {
  'zh-CN': {
    common: {
      save: '保存',
      cancel: '取消',
      search: '搜索',
      create: '新建',
      edit: '编辑',
      empty: '暂无数据',
      loading: '加载中'
    },
    product: {
      title: '商品',
      variants: '规格',
      shelfLife: '批次/效期',
      pickingStrategy: '拣货策略',
      minShelfLife: '最低剩余保质期（天）',
      nearExpiryDays: '临期阈值（天）'
    }
  },
  'zh-TW': {
    common: {
      save: '儲存',
      cancel: '取消',
      search: '搜尋',
      create: '新增',
      edit: '編輯',
      empty: '暫無資料',
      loading: '載入中'
    },
    product: {
      title: '商品',
      variants: '規格',
      shelfLife: '批次/效期',
      pickingStrategy: '揀貨策略',
      minShelfLife: '最低剩餘保存期限（天）',
      nearExpiryDays: '臨期閾值（天）'
    }
  }
} as const

export type Locale = keyof typeof messages
export type MessageKey = keyof (typeof messages)['zh-CN']