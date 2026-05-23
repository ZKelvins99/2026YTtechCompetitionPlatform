<template>
  <div class="crm-page-header">
    <div class="crm-page-header__accent" />
    <div class="crm-page-header__body">
      <div class="crm-page-header__text">
        <h2 class="crm-page-header__title">{{ title }}</h2>
        <p v-if="description" class="crm-page-header__desc">{{ description }}</p>
      </div>
      <div v-if="$slots.extra" class="crm-page-header__extra">
        <slot name="extra" />
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  title: { type: String, required: true },
  description: { type: String, default: '' }
})
</script>
