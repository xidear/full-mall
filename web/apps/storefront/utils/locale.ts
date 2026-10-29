/**
 * 当前界面语言（SSR 与客户端共用）。格式化金额/日期必须跟随语言，
 * 不能固定写死 zh-CN，否则切到 English 后日期仍是中文格式。
 */
export function currentLocale(): string {
  return useState<string>('locale', () => 'zh-CN').value
}
