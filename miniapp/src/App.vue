<script setup lang="ts">
import { onLaunch } from '@dcloudio/uni-app'
import { useSessionStore } from '@/stores/session'

onLaunch(() => {
  useSessionStore().restore()
})
</script>

<style>
page {
  min-height: 100%;
  background: #f6f7fb;
  color: #172033;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
}

button::after {
  border: 0;
}

.ruankao-page {
  min-height: 100vh;
  box-sizing: border-box;
  padding: 32rpx;
}

.ruankao-card {
  border-radius: 28rpx;
  background: #fff;
  box-shadow: 0 12rpx 34rpx rgba(31, 45, 70, 0.06);
}
</style>
