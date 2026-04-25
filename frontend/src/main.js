import { createApp } from 'vue'
import 'element-plus/es/components/message/style/css'
import 'element-plus/es/components/message-box/style/css'
import App from './App.vue'
import router from './router'
import pinia from './store'
import './styles/index.scss'

const app = createApp(App)

app.config.errorHandler = (error, instance, info) => {
  console.error('[app:error]', info, error)
}

app.use(router)
app.use(pinia)
app.mount('#app')
