/**
 * 后台展示用的枚举文案。
 * 后台可以显示内部编码，但列表/详情的主信息不应把 PENDING、MAIN 这类机器值直接抛给运营。
 */
export const aiJobStatusLabels: Record<string, string> = {
  PENDING: '排队中',
  QUEUED: '排队中',
  RUNNING: '生成中',
  SUCCEEDED: '已完成',
  FAILED: '失败',
  CANCELED: '已取消',
  CANCELLED: '已取消'
}

export const mediaRoleLabels: Record<string, string> = {
  MAIN: '主图',
  GALLERY: '图集',
  DETAIL: '详情图',
  VIDEO: '视频',
  SPEC: '规格图'
}

export const documentTypeLabels: Record<string, string> = {
  PDF: 'PDF 文档',
  DOC: 'Word 文档',
  DOCX: 'Word 文档',
  TXT: '纯文本',
  MD: 'Markdown',
  CSV: 'CSV 表格',
  HTML: '网页',
  HTM: '网页',
  XLS: 'Excel 表格',
  XLSX: 'Excel 表格'
}

export function labelOf(map: Record<string, string>, value?: string | null): string {
  if (!value) return '-'
  return map[value] ?? value
}
