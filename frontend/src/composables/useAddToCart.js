import { useRouter } from 'vue-router'
import { addToCart as addToCartApi } from '../api'
import { useCartStore } from '../store/cart'
import { ElMessage } from 'element-plus'

export function useAddToCart() {
  const router = useRouter()
  const cartStore = useCartStore()

  const handleAddCart = async (product) => {
    try {
      await addToCartApi({ productId: product.id, quantity: 1 })
      cartStore.addCartItem({
        productId: product.id,
        productName: product.name,
        imageUrl: product.imageUrl,
        stock: product.stock,
        quantity: 1,
        price: product.price
      })
      ElMessage.success('已加入购物车')
    } catch (error) {
      if (error.response?.status === 401) {
        router.push({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } })
      }
    }
  }

  return { handleAddCart }
}
