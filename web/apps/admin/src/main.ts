import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'
import { initAuth } from './auth'
import './styles.css'

initAuth()

createApp(App)
  .use(ElementPlus)
  .use(router)
  .mount('#app')