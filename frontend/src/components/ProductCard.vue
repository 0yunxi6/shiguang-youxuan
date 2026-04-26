<template>
  <article class="product-card" tabindex="0" role="link" :aria-label="`查看${product.name}`" @click="handleClick" @keydown.enter.prevent="handleClick" @keydown.space.prevent="handleClick">
    <div class="card-img-wrap">
      <img :src="product.imageUrl || '/placeholder.svg'" :alt="product.name" loading="lazy" decoding="async" @error="handleImageError" />
      <div class="card-actions">
        <button class="act-btn" @click.stop="handleAddCart" :disabled="isSoldOut" :title="isSoldOut ? '已售罄' : '加入购物车'" :aria-label="isSoldOut ? '已售罄' : '加入购物车'">
          <el-icon :size="16"><ShoppingCart /></el-icon>
        </button>
        <button class="act-btn" @click.stop="handleToggleFav" :class="{ active: isFavorited }" title="收藏">
          <el-icon :size="16"><Star /></el-icon>
        </button>
      </div>
      <span v-if="product.stock <= 5 && product.stock > 0" class="tag tag-stock">仅剩{{ product.stock }}件</span>
      <span v-if="product.stock === 0" class="tag tag-out">已售罄</span>
      <span v-if="product.promotionActive" class="tag tag-promo">{{ product.promotionTag || '限时特惠' }}</span>
      <span v-if="product.sales && product.sales > 100" class="tag tag-hot">热销</span>
    </div>
    <div class="card-body">
      <h3 class="card-title">{{ product.name }}</h3>
      <p class="card-desc" v-if="product.description">{{ product.description }}</p>
      <div class="card-bottom">
        <div class="price-group">
          <span class="price-sym">¥</span>
          <span class="price-int">{{ priceInt }}</span>
          <span class="price-dec">.{{ priceDec }}</span>
          <span v-if="product.promotionActive" class="origin-price">¥{{ Number(product.price || 0).toFixed(2) }}</span>
        </div>
        <span class="sales-count" v-if="product.sales">已售{{ product.sales }}</span>
      </div>
    </div>
  </article>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ShoppingCart, Star } from '@element-plus/icons-vue'

const props = defineProps({ product: { type: Object, required: true } })
const emit = defineEmits(['click', 'add-cart', 'toggle-favorite'])
const isFavorited = ref(false)
const displayPrice = computed(() => props.product.promotionActive ? (props.product.effectivePrice || props.product.promotionPrice || props.product.price) : props.product.price)
const priceStr = computed(() => Number(displayPrice.value || 0).toFixed(2))
const priceInt = computed(() => priceStr.value.split('.')[0])
const priceDec = computed(() => priceStr.value.split('.')[1])
const isSoldOut = computed(() => Number(props.product.stock || 0) <= 0)
const handleClick = () => emit('click', props.product)
const handleAddCart = () => {
  if (isSoldOut.value) return
  emit('add-cart', props.product)
}
const handleToggleFav = () => { isFavorited.value = !isFavorited.value; emit('toggle-favorite', { ...props.product, _favorited: isFavorited.value }) }
const handleImageError = (event) => {
  event.target.src = '/placeholder.svg'
}
</script>

<style scoped>
.product-card {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: box-shadow 0.2s;
  border: 1px solid #f0eeea;
  height: 100%;
  display: flex;
  flex-direction: column;
}
.product-card:hover {
  box-shadow: 0 4px 16px rgba(0,0,0,0.06);
  border-color: #e0ddd6;
}
.card-img-wrap {
  position: relative;
  overflow: hidden;
  aspect-ratio: 1 / 1;
  background: #f7f7f5;
}
.card-img-wrap img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}
.product-card:hover .card-img-wrap img {
  transform: scale(1.03);
}
.card-actions {
  position: absolute;
  bottom: 10px;
  right: 10px;
  display: flex;
  gap: 6px;
  opacity: 0;
  transform: translateY(4px);
  transition: all 0.2s;
}
.product-card:hover .card-actions {
  opacity: 1;
  transform: translateY(0);
}
.act-btn {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: #fff;
  color: #666;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  transition: all 0.15s;
}
.act-btn:hover { background: #1a1a1a; color: #fff; }
.act-btn.active { background: #fef3e6; color: #e6a23c; }
.act-btn:disabled { opacity: 0.45; cursor: not-allowed; }
.tag {
  position: absolute;
  top: 8px;
  padding: 2px 8px;
  font-size: 11px;
  font-weight: 500;
  border-radius: 3px;
}
.tag-stock { left: 8px; background: #f5a623; color: #fff; }
.tag-out { left: 8px; background: #999; color: #fff; }
.tag-hot { right: 8px; background: var(--color-primary, #c45c3e); color: #fff; }
.tag-promo { left: 8px; bottom: 8px; top: auto; background: #f56c6c; color: #fff; }
.card-body {
  padding: 12px 14px;
  flex: 1;
  display: flex;
  flex-direction: column;
}
.card-title {
  font-size: 14px;
  font-weight: 500;
  color: #1a1a1a;
  margin: 0 0 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  line-height: 1.4;
}
.card-desc {
  font-size: 12px;
  color: #999;
  margin: 0 0 10px;
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  flex: 1;
}
.card-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.price-group {
  display: flex;
  align-items: baseline;
  color: var(--color-price, #c45c3e);
}
.price-sym { font-size: 12px; font-weight: 600; }
.price-int { font-size: 20px; font-weight: 700; line-height: 1; }
.price-dec { font-size: 12px; font-weight: 600; }
.origin-price {
  margin-left: 6px;
  font-size: 11px;
  color: #bbb;
  text-decoration: line-through;
  font-weight: 500;
}
.sales-count {
  font-size: 11px;
  color: #bbb;
}
</style>
