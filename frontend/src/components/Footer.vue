<template>
  <footer class="site-footer">
    <div class="footer-inner">
      <div class="footer-top">
        <div class="footer-brand">
          <div class="footer-logo">
            <span class="logo-icon">拾</span>
            <span class="logo-text">拾光优选</span>
          </div>
          <p class="brand-desc">拾取美好，优选生活</p>
        </div>
        <div class="footer-links-group">
          <div class="footer-col">
            <h4>购物指南</h4>
            <a href="#">购物流程</a>
            <a href="#">会员介绍</a>
            <a href="#">常见问题</a>
          </div>
          <div class="footer-col">
            <h4>配送服务</h4>
            <a href="#">配送方式</a>
            <a href="#">配送范围</a>
            <a href="#">配送时效</a>
          </div>
          <div class="footer-col">
            <h4>售后服务</h4>
            <a href="#">退换货政策</a>
            <a href="#">价格保护</a>
            <a href="#">投诉建议</a>
          </div>
          <div class="footer-col">
            <h4>关于我们</h4>
            <a href="#">品牌故事</a>
            <a href="#">加入我们</a>
            <a href="#">联系客服</a>
          </div>
        </div>
      </div>
      <div class="footer-bottom">
        <p>&copy; 2026 拾光优选. All rights reserved.</p>
        <div class="footer-badges">
          <span class="badge">安全支付</span>
          <span class="sep">·</span>
          <span class="badge">正品保障</span>
          <span class="sep">·</span>
          <span class="badge">极速发货</span>
        </div>
      </div>
    </div>
  </footer>
</template>

<style scoped>
.site-footer { background: #1a1a1a; color: #999; }
.footer-inner { max-width: 1200px; margin: 0 auto; padding: 0 20px; }
.footer-top { display: flex; justify-content: space-between; gap: 60px; padding: 48px 0 40px; }
.footer-brand { max-width: 240px; }
.footer-logo { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; }
.footer-logo .logo-icon { width: 30px; height: 30px; border-radius: 6px; background: #fff; color: #1a1a1a; display: flex; align-items: center; justify-content: center; font-size: 14px; font-weight: 800; }
.footer-logo .logo-text { font-size: 16px; font-weight: 700; color: #fff; }
.brand-desc { font-size: 13px; color: #555; margin: 0; line-height: 1.6; }
.footer-links-group { display: flex; gap: 48px; }
.footer-col { display: flex; flex-direction: column; gap: 10px; }
.footer-col h4 { font-size: 13px; font-weight: 600; color: #ccc; margin: 0 0 4px; }
.footer-col a { font-size: 13px; color: #666; text-decoration: none; transition: color 0.2s; }
.footer-col a:hover { color: #aaa; }
.footer-bottom { border-top: 1px solid rgba(255,255,255,0.06); padding: 20px 0; display: flex; align-items: center; justify-content: space-between; }
.footer-bottom p { font-size: 12px; color: #444; margin: 0; }
.footer-badges { display: flex; align-items: center; gap: 8px; }
.badge { font-size: 12px; color: #555; }
.sep { color: #333; }

@media (max-width: 768px) {
  .footer-top { flex-direction: column; gap: 32px; padding: 36px 0 28px; }
  .footer-links-group { display: grid; grid-template-columns: 1fr 1fr; gap: 24px; }
  .footer-bottom { flex-direction: column; gap: 10px; text-align: center; }
}
</style>
