import { defineStore } from 'pinia'
import { ref, computed, watch } from 'vue'
import { storage } from '../utils/storage'

export const useCartStore = defineStore('cart', () => {
  const cartItems = ref(storage.getJSON('cartItems', []))
  const synced = ref(false)

  const cartCount = computed(() => {
    return cartItems.value.reduce((total, item) => total + (item.quantity || 0), 0)
  })

  const cartTotal = computed(() => {
    return cartItems.value.reduce((total, item) => total + (item.price || 0) * (item.quantity || 0), 0)
  })

  // 自动持久化
  watch(cartItems, (val) => {
    storage.setJSON('cartItems', val)
  }, { deep: true })

  const setCartItems = (items) => {
    cartItems.value = items
    synced.value = true
  }

  const addCartItem = (item) => {
    const existItem = cartItems.value.find(i => i.productId === item.productId)
    if (existItem) {
      existItem.quantity += item.quantity
    } else {
      cartItems.value.push(item)
    }
  }

  const updateCartItem = (productId, quantity) => {
    const item = cartItems.value.find(i => i.productId === productId)
    if (item) {
      item.quantity = quantity
    }
  }

  const removeCartItem = (productId) => {
    const index = cartItems.value.findIndex(i => i.productId === productId)
    if (index > -1) {
      cartItems.value.splice(index, 1)
    }
  }

  const clearCart = () => {
    cartItems.value = []
    synced.value = false
  }

  return {
    cartItems,
    cartCount,
    cartTotal,
    synced,
    setCartItems,
    addCartItem,
    updateCartItem,
    removeCartItem,
    clearCart
  }
})
