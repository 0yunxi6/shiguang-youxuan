import request from '../utils/request'

const postJson = (url, data) => request.post(url, data)

// ========== Auth ==========
export const login = (data) => postJson('/auth/login', data)
export const register = (data) => postJson('/auth/register', data)
export const forgotPasswordVerify = (data) => postJson('/auth/forgot-password/verify', data)
export const forgotPasswordReset = (data) => postJson('/auth/forgot-password/reset', data)

// ========== Products ==========
export const getProductList = (params) => request.get('/products', { params })
export const getProductDetail = (id) => request.get(`/products/${id}`)
export const searchProducts = (params) => request.get('/products/search', { params })
export const getProductReviews = (productId, params, config = {}) => request.get(`/products/${productId}/reviews`, { params, ...config })
export const getProductReviewSummary = (productId, config = {}) => request.get(`/products/${productId}/reviews/summary`, config)
export const createProductReview = (data) => request.post('/reviews', data)
export const getFavorites = () => request.get('/favorites')
export const addFavorite = (productId) => request.post(`/favorites/${productId}`)
export const removeFavorite = (productId) => request.delete(`/favorites/${productId}`)

// ========== Categories ==========
export const getCategories = () => request.get('/categories')
export const getCategoryDetail = (id) => request.get(`/categories/${id}`)

// ========== Cart ==========
export const addToCart = (data) => request.post('/cart/add', data)
export const getCartList = () => request.get('/cart')
export const updateCartItem = (data) => request.put('/cart/update', data)
export const deleteCartItem = (id) => request.delete(`/cart/${id}`)
export const clearCart = () => request.delete('/cart/clear')

// ========== Orders ==========
export const createOrder = (data) => request.post('/orders', data)
export const getOrderList = (params) => request.get('/orders', { params })
export const getOrderDetail = (id) => request.get(`/orders/${id}`)
export const cancelOrder = (id) => request.put(`/orders/${id}/cancel`)
export const confirmOrder = (id) => request.put(`/orders/${id}/confirm`)
export const getCoupons = (params) => request.get('/coupons', { params })

// ========== User ==========
export const getUserProfile = () => request.get('/user/profile')
export const updateProfile = (data) => request.put('/user/profile', data)
export const updatePassword = (data) => request.put('/user/password', data)

// ========== File Upload ==========
export const uploadFile = (formData) => request.post('/upload/image', formData, { headers: { 'Content-Type': 'multipart/form-data' } })

// ========== Admin APIs ==========
export const adminGetStats = () => request.get('/admin/dashboard/stats')
export const adminGetProducts = (params) => request.get('/admin/products', { params })
export const adminCreateProduct = (data) => request.post('/admin/products', data)
export const adminUpdateProduct = (id, data) => request.put(`/admin/products/${id}`, data)
export const adminDeleteProduct = (id) => request.delete(`/admin/products/${id}`)
export const adminGetOrders = (params) => request.get('/admin/orders', { params })
export const adminUpdateOrderStatus = (id, status) => request.put(`/admin/orders/${id}/status`, null, { params: { status } })
export const adminGetUsers = (params) => request.get('/admin/users', { params })
export const adminToggleUserStatus = (id, status) => request.put(`/admin/users/${id}/status`, null, { params: { status } })
export const adminDeleteUser = (id) => request.delete(`/admin/users/${id}`)
export const adminCreateCategory = (data) => request.post('/admin/categories', data)
export const adminUpdateCategory = (id, data) => request.put(`/admin/categories/${id}`, data)
export const adminDeleteCategory = (id) => request.delete(`/admin/categories/${id}`)
export const adminGetReviews = (params, config = {}) => request.get('/admin/reviews', { params, ...config })
export const adminGetReviewStats = (config = {}) => request.get('/admin/reviews/stats', config)
export const adminUpdateReviewStatus = (id, status) => request.put(`/admin/reviews/${id}/status`, null, { params: { status } })
export const adminDeleteReview = (id) => request.delete(`/admin/reviews/${id}`)
