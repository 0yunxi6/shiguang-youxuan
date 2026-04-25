export const storage = {
  get(key, fallback = null) {
    try {
      const value = localStorage.getItem(key)
      return value === null ? fallback : value
    } catch (error) {
      return fallback
    }
  },

  set(key, value) {
    try {
      localStorage.setItem(key, value)
    } catch (error) {
      // ????????????????????
    }
  },

  getJSON(key, fallback = null) {
    const value = this.get(key)
    if (value === null) return fallback
    try {
      return JSON.parse(value)
    } catch (error) {
      this.remove(key)
      return fallback
    }
  },

  setJSON(key, value) {
    this.set(key, JSON.stringify(value))
  },

  remove(key) {
    try {
      localStorage.removeItem(key)
    } catch (error) {
      // ignore
    }
  }
}
