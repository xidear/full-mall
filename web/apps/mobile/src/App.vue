<script lang="ts">
/**
 * onLaunch 早于任何页面：页面栈为空、拿不到参数，失败也没有页面能提示，所以这里不放业务请求。
 * console.log 只用于区分「应用没起来」和「页面白屏」。
 */
export default {
  onLaunch() {
    console.log('Full Mall mobile launched')
  }
}
</script>

<style>
/*
 * 各页面 scoped 样式命中不到 uni-app 的页面根节点，背景/字体只能在这一层给一次；
 * 删掉后 H5 与小程序首帧都退回白底。
 */
page {
  /* 页面根节点会注入主题变量；这里的 fallback 只用于首帧，避免白色闪屏。 */
  background: var(--bg, #f6f5f2);
  color: var(--text, #1f2421);
  font-family: "PingFang SC", "Microsoft YaHei", sans-serif;
}
</style>