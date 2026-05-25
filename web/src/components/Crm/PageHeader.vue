<template>
  <div class="crm-page-header">
    <div class="crm-page-header__bar">
      <div class="crm-page-header__main">
        <h2 class="crm-page-header__title">{{ title }}</h2>
        <p v-if="tagline" class="crm-page-header__tagline">{{ tagline }}</p>
      </div>
      <div class="crm-page-header__actions">
        <el-button
          v-if="introItems.length"
          class="crm-page-header__intro-btn"
          link
          type="primary"
          @click="introVisible = true"
        >
          <el-icon class="mr-1"><InfoFilled /></el-icon>
          功能介绍
        </el-button>
        <slot name="extra" />
      </div>
    </div>

    <el-drawer
      v-model="introVisible"
      :title="introTitle"
      direction="rtl"
      size="380px"
      class="crm-intro-drawer"
    >
      <p v-if="tagline" class="crm-intro-drawer__lead">{{ tagline }}</p>
      <ul class="crm-intro-drawer__list">
        <li v-for="(item, index) in introItems" :key="index">
          <el-icon class="crm-intro-drawer__icon"><CircleCheckFilled /></el-icon>
          <span>{{ item }}</span>
        </li>
      </ul>
      <p v-if="tips" class="crm-intro-drawer__tips">{{ tips }}</p>
    </el-drawer>
  </div>
</template>

<script setup>
import { CircleCheckFilled, InfoFilled } from '@element-plus/icons-vue'
import { computed, ref } from 'vue'

const props = defineProps({
  title: { type: String, required: true },
  /** 标题下一句简短说明（可选） */
  tagline: { type: String, default: '' },
  /** 功能介绍列表（推荐） */
  features: { type: Array, default: () => [] },
  /** 兼容旧版长 description，自动拆成列表并收入抽屉 */
  description: { type: String, default: '' },
  tips: { type: String, default: '' },
  introTitle: { type: String, default: '功能介绍' }
})

const introVisible = ref(false)

const introItems = computed(() => {
  if (props.features?.length) {
    return props.features
  }
  if (!props.description) {
    return []
  }
  return props.description
    .split(/[；;。]/)
    .map(s => s.trim())
    .filter(Boolean)
})
</script>

<style scoped>
.crm-page-header {
  margin-bottom: 16px;
  overflow: hidden;
  border-radius: 12px;
  border: 1px solid var(--el-border-color-lighter);
  background: #fff;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.06);
}
.crm-page-header__bar {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px 16px;
  padding: 14px 18px;
  border-top: 3px solid var(--el-color-primary);
}
.crm-page-header__main {
  min-width: 0;
  flex: 1;
}
.crm-page-header__title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  line-height: 1.35;
  color: #1e293b;
}
.crm-page-header__tagline {
  margin: 6px 0 0;
  font-size: 13px;
  line-height: 1.5;
  color: #64748b;
}
.crm-page-header__actions {
  display: flex;
  flex-shrink: 0;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}
.crm-page-header__intro-btn {
  font-size: 13px;
}
.crm-intro-drawer__lead {
  margin: 0 0 16px;
  padding: 10px 12px;
  font-size: 14px;
  line-height: 1.5;
  color: #334155;
  background: #f8fafc;
  border-radius: 8px;
}
.crm-intro-drawer__list {
  margin: 0;
  padding: 0;
  list-style: none;
}
.crm-intro-drawer__list li {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 0;
  font-size: 14px;
  line-height: 1.55;
  color: #475569;
  border-bottom: 1px solid #f1f5f9;
}
.crm-intro-drawer__list li:last-child {
  border-bottom: none;
}
.crm-intro-drawer__icon {
  margin-top: 3px;
  flex-shrink: 0;
  font-size: 16px;
  color: var(--el-color-success);
}
.crm-intro-drawer__tips {
  margin: 20px 0 0;
  font-size: 12px;
  line-height: 1.6;
  color: #94a3b8;
}
</style>
