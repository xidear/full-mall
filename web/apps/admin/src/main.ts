import { createApp } from 'vue'
import ElementPlus from 'element-plus'
// 组件库语言包：不配置时下拉占位/分页/确认框按钮全是英文，页面上会出现 "Select" 这种中英混排
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'
import { initAuth } from './auth'
import './styles.css'

initAuth()

createApp(App)
  .use(ElementPlus, { locale: zhCn })
  .use(router)
  .mount('#app')