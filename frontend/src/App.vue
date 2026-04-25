<template>
  <div id="app">
    <template v-if="isAdminRoute">
      <router-view />
    </template>
    <template v-else>
      <Header />
      <main class="main-content">
        <router-view v-slot="{ Component }">
          <transition name="page" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
      <Footer />
    </template>
    <BackToTop />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import Header from './components/Header.vue'
import Footer from './components/Footer.vue'
import BackToTop from './components/BackToTop.vue'

const route = useRoute()
const isAdminRoute = computed(() => route.path.startsWith('/admin'))
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

#app {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  color: #1a1a1a;
  background: #f7f7f5;
}

.main-content {
  padding: 0;
  min-height: calc(100vh - 60px);
}

a {
  text-decoration: none;
  color: inherit;
}

::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

::-webkit-scrollbar-thumb {
  background: #d0cec8;
  border-radius: 3px;
}

::-webkit-scrollbar-thumb:hover {
  background: #b5b3ad;
}

::-webkit-scrollbar-track {
  background: transparent;
}

.page-enter-active {
  transition: opacity 0.2s ease;
}

.page-leave-active {
  transition: opacity 0.12s ease;
}

.page-enter-from {
  opacity: 0;
}

.page-leave-to {
  opacity: 0;
}

:root {
  --color-primary: #c45c3e;
  --color-primary-light: #d4785e;
  --color-primary-dark: #a84830;
  --color-accent: #2c2c2c;
  --color-bg: #f7f7f5;
  --color-bg-warm: #f2efe9;
  --color-text: #1a1a1a;
  --color-text-secondary: #666;
  --color-text-muted: #999;
  --color-border: #e8e6e1;
  --color-price: #c45c3e;
  --radius-sm: 4px;
  --radius-md: 8px;
  --radius-lg: 12px;

  --el-color-primary: #c45c3e;
  --el-color-primary-light-3: #d4785e;
  --el-color-primary-light-5: #dfa08a;
  --el-color-primary-light-7: #eac4b6;
  --el-color-primary-light-8: #f0d5ca;
  --el-color-primary-light-9: #f7e8e2;
  --el-color-primary-dark-2: #a84830;
  --el-border-radius-base: 6px;
  --el-border-radius-small: 4px;
  --el-font-size-base: 14px;
}

.el-button--primary {
  --el-button-bg-color: var(--color-primary);
  --el-button-border-color: var(--color-primary);
  --el-button-hover-bg-color: var(--color-primary-light);
  --el-button-hover-border-color: var(--color-primary-light);
  --el-button-active-bg-color: var(--color-primary-dark);
  --el-button-active-border-color: var(--color-primary-dark);
}

.el-message {
  border-radius: 8px !important;
}

.el-message-box {
  border-radius: 10px !important;
}

.el-dialog {
  border-radius: 10px !important;
}

.el-dialog__header {
  border-bottom: 1px solid var(--color-border);
  padding-bottom: 14px;
  margin-right: 0 !important;
}

.el-dialog__title {
  font-weight: 600;
  color: #303133;
}

.el-input__wrapper {
  border-radius: 6px !important;
}

.el-select .el-input__wrapper {
  border-radius: 6px !important;
}
</style>
