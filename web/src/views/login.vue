<template>
  <div class="login relative flex min-h-screen overflow-hidden bg-slate-950">
    <!-- 背景装饰 -->
    <div class="pointer-events-none absolute inset-0">
      <div class="absolute -left-32 -top-32 h-96 w-96 rounded-full bg-brand-600/30 blur-3xl" />
      <div class="absolute bottom-0 right-0 h-[28rem] w-[28rem] rounded-full bg-indigo-500/20 blur-3xl" />
      <div
        class="absolute inset-0 opacity-30"
        style="background-image: url('../assets/images/login-background.jpg'); background-size: cover; background-position: center;"
      />
      <div class="absolute inset-0 bg-slate-950/70 backdrop-blur-[2px]" />
    </div>

    <div class="relative z-10 flex w-full flex-col lg:flex-row">
      <!-- 左侧品牌区 -->
      <div class="hidden flex-1 flex-col justify-center px-12 py-16 lg:flex xl:px-20">
        <div class="mb-6 inline-flex w-fit items-center gap-2 rounded-full border border-white/10 bg-white/5 px-4 py-1.5 text-xs font-medium text-brand-200 backdrop-blur">
          <span class="h-1.5 w-1.5 rounded-full bg-emerald-400" />
          2026 技术比武 · CRM 平台
        </div>
        <h1 class="max-w-lg text-4xl font-bold tracking-tight text-white xl:text-5xl">
          智能客户关系
          <span class="bg-gradient-to-r from-brand-300 to-cyan-300 bg-clip-text text-transparent">管理平台</span>
        </h1>
        <p class="mt-4 max-w-md text-base leading-relaxed text-slate-400">
          客户、商机、合同、工作台一体化；支持工作流审批与数据监控大屏。
        </p>
        <ul class="mt-10 space-y-3 text-sm text-slate-400">
          <li class="flex items-center gap-3">
            <span class="flex h-8 w-8 items-center justify-center rounded-lg bg-brand-500/20 text-brand-300">✓</span>
            可拖拽个性化工作台
          </li>
          <li class="flex items-center gap-3">
            <span class="flex h-8 w-8 items-center justify-center rounded-lg bg-brand-500/20 text-brand-300">✓</span>
            CRM 全链路业务管理
          </li>
          <li class="flex items-center gap-3">
            <span class="flex h-8 w-8 items-center justify-center rounded-lg bg-brand-500/20 text-brand-300">✓</span>
            实时数据监控与审计
          </li>
        </ul>
      </div>

      <!-- 右侧登录卡片 -->
      <div class="flex flex-1 items-center justify-center px-6 py-12 sm:px-10">
        <div class="tw-card w-full max-w-md p-8 sm:p-10">
          <div class="mb-8 text-center lg:text-left">
            <h3 class="text-2xl font-bold text-slate-800">{{ title }}</h3>
            <p class="mt-2 text-sm text-slate-500">欢迎回来，请登录您的账号</p>
          </div>

          <el-form ref="loginRef" :model="loginForm" :rules="loginRules" class="tw-input-shell space-y-1">
            <el-form-item prop="username">
              <el-input
                v-model="loginForm.username"
                type="text"
                size="large"
                auto-complete="off"
                placeholder="账号"
              >
                <template #prefix><svg-icon icon-class="user" class="el-input__icon input-icon text-slate-400" /></template>
              </el-input>
            </el-form-item>
            <el-form-item prop="password">
              <el-input
                v-model="loginForm.password"
                type="password"
                size="large"
                auto-complete="off"
                placeholder="密码"
                show-password
                @keyup.enter="handleLogin"
              >
                <template #prefix><svg-icon icon-class="password" class="el-input__icon input-icon text-slate-400" /></template>
              </el-input>
            </el-form-item>
            <el-form-item v-if="captchaEnabled" prop="code">
              <div class="flex gap-3">
                <el-input
                  v-model="loginForm.code"
                  size="large"
                  auto-complete="off"
                  placeholder="验证码"
                  class="flex-1"
                  @keyup.enter="handleLogin"
                >
                  <template #prefix><svg-icon icon-class="validCode" class="el-input__icon input-icon text-slate-400" /></template>
                </el-input>
                <img
                  :src="codeUrl"
                  class="h-10 w-[30%] cursor-pointer rounded-xl border border-slate-200 object-cover transition hover:border-brand-400"
                  alt="验证码"
                  @click="getCode"
                />
              </div>
            </el-form-item>

            <div class="flex items-center justify-between py-1">
              <el-checkbox v-model="loginForm.rememberMe" class="!text-slate-600">记住密码</el-checkbox>
              <router-link
                v-if="register"
                class="text-sm font-medium text-brand-600 transition hover:text-brand-700"
                :to="'/register'"
              >立即注册</router-link>
            </div>

            <el-form-item class="!mb-0 mt-2">
              <button
                type="button"
                class="tw-btn-primary flex items-center justify-center gap-2 disabled:opacity-60"
                :disabled="loading"
                @click.prevent="handleLogin"
              >
                <span v-if="!loading">登 录</span>
                <span v-else class="inline-flex items-center gap-2">
                  <span class="h-4 w-4 animate-spin rounded-full border-2 border-white/30 border-t-white" />
                  登录中...
                </span>
              </button>
            </el-form-item>
          </el-form>
        </div>
      </div>
    </div>

    <div class="absolute bottom-0 z-10 w-full py-4 text-center text-xs tracking-wide text-slate-500">
      <span>{{ footerContent }}</span>
    </div>
  </div>
</template>

<script setup>
import { getCodeImg } from "@/api/login"
import Cookies from "js-cookie"
import { encrypt, decrypt } from "@/utils/jsencrypt"
import useUserStore from '@/store/modules/user'
import defaultSettings from '@/settings'

const title = import.meta.env.VITE_APP_TITLE
const footerContent = defaultSettings.footerContent
const userStore = useUserStore()
const route = useRoute()
const router = useRouter()
const { proxy } = getCurrentInstance()

const loginForm = ref({
  username: "admin",
  password: "admin123",
  rememberMe: false,
  code: "",
  uuid: ""
})

const loginRules = {
  username: [{ required: true, trigger: "blur", message: "请输入您的账号" }],
  password: [{ required: true, trigger: "blur", message: "请输入您的密码" }],
  code: [{ required: true, trigger: "change", message: "请输入验证码" }]
}

const codeUrl = ref("")
const loading = ref(false)
const captchaEnabled = ref(true)
const register = ref(false)
const redirect = ref(undefined)

watch(route, (newRoute) => {
    redirect.value = newRoute.query && newRoute.query.redirect
}, { immediate: true })

function handleLogin() {
  proxy.$refs.loginRef.validate(valid => {
    if (valid) {
      loading.value = true
      if (loginForm.value.rememberMe) {
        Cookies.set("username", loginForm.value.username, { expires: 30 })
        Cookies.set("password", encrypt(loginForm.value.password), { expires: 30 })
        Cookies.set("rememberMe", loginForm.value.rememberMe, { expires: 30 })
      } else {
        Cookies.remove("username")
        Cookies.remove("password")
        Cookies.remove("rememberMe")
      }
      userStore.login(loginForm.value).then(() => {
        const query = route.query
        const otherQueryParams = Object.keys(query).reduce((acc, cur) => {
          if (cur !== "redirect") {
            acc[cur] = query[cur]
          }
          return acc
        }, {})
        router.push({ path: redirect.value || "/", query: otherQueryParams })
      }).catch(() => {
        loading.value = false
        if (captchaEnabled.value) {
          getCode()
        }
      })
    }
  })
}

function getCode() {
  getCodeImg().then(res => {
    captchaEnabled.value = res.captchaEnabled === undefined ? true : res.captchaEnabled
    if (captchaEnabled.value) {
      codeUrl.value = "data:image/gif;base64," + res.img
      loginForm.value.uuid = res.uuid
    }
  })
}

function getCookie() {
  const username = Cookies.get("username")
  const password = Cookies.get("password")
  const rememberMe = Cookies.get("rememberMe")
  loginForm.value = {
    username: username === undefined ? loginForm.value.username : username,
    password: password === undefined ? loginForm.value.password : decrypt(password),
    rememberMe: rememberMe === undefined ? false : Boolean(rememberMe)
  }
}

getCode()
getCookie()
</script>

<style scoped>
.input-icon {
  height: 1rem;
  width: 1rem;
}
:deep(.el-form-item) {
  margin-bottom: 18px;
}
html.dark .login .tw-card {
  border-color: rgb(51 65 85 / 0.5);
  background-color: rgb(15 23 42 / 0.9);
}
html.dark .login h3 {
  color: #f1f5f9;
}
html.dark .login p {
  color: #94a3b8;
}
</style>
