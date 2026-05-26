<template>
  <div class="login relative flex min-h-screen overflow-hidden bg-gradient-to-br from-slate-50 via-sky-50 to-indigo-50/90">
    <!-- 亮色背景装饰 -->
    <div class="pointer-events-none absolute inset-0 overflow-hidden">
      <div class="absolute -left-24 -top-24 h-[22rem] w-[22rem] rounded-full bg-sky-200/70 blur-3xl" />
      <div class="absolute right-[-6rem] top-1/4 h-80 w-80 rounded-full bg-brand-200/60 blur-3xl" />
      <div class="absolute bottom-[-4rem] left-1/3 h-96 w-96 rounded-full bg-indigo-100/80 blur-3xl" />
      <div
        class="absolute inset-0 opacity-[0.35]"
        style="background-image: radial-gradient(circle at 1px 1px, rgb(148 163 184 / 0.15) 1px, transparent 0); background-size: 28px 28px;"
      />
    </div>

    <div class="relative z-10 flex w-full flex-col lg:flex-row">
      <!-- 左侧品牌区 -->
      <div class="hidden flex-1 flex-col justify-center px-12 py-16 lg:flex xl:px-20">
        <div class="mb-6 inline-flex w-fit items-center gap-2 rounded-full border border-slate-200/80 bg-white/70 px-4 py-1.5 text-xs font-medium text-brand-700 shadow-sm backdrop-blur-sm">
          <span class="h-1.5 w-1.5 rounded-full bg-emerald-500" />
          {{ title }}
        </div>
        <h1 class="max-w-lg text-4xl font-bold tracking-tight text-slate-800 xl:text-5xl">
          {{ title }}
        </h1>
        <p class="mt-4 max-w-md text-base leading-relaxed text-slate-600">
          客户、商机、合同、工作台一体化；支持工作流审批与数据监控大屏。
        </p>
        <ul class="mt-10 space-y-3 text-sm text-slate-600">
          <li class="flex items-center gap-3">
            <span class="flex h-8 w-8 items-center justify-center rounded-lg bg-brand-100 text-brand-600">✓</span>
            可拖拽个性化工作台
          </li>
          <li class="flex items-center gap-3">
            <span class="flex h-8 w-8 items-center justify-center rounded-lg bg-brand-100 text-brand-600">✓</span>
            CRM 全链路业务管理
          </li>
          <li class="flex items-center gap-3">
            <span class="flex h-8 w-8 items-center justify-center rounded-lg bg-brand-100 text-brand-600">✓</span>
            实时数据监控与审计
          </li>
        </ul>
      </div>

      <!-- 右侧登录卡片 -->
      <div class="flex flex-1 items-center justify-center px-6 py-12 sm:px-10">
        <div class="login-card w-full max-w-md p-8 sm:p-10">
          <div class="mb-2 flex justify-center lg:hidden">
            <span class="rounded-full border border-brand-200 bg-brand-50 px-3 py-1 text-xs font-medium text-brand-700">
              {{ title }}
            </span>
          </div>
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
                  class="h-10 w-[30%] cursor-pointer rounded-xl border border-slate-200 bg-white object-cover transition hover:border-brand-400 hover:shadow-sm"
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

    <div class="absolute bottom-0 z-10 w-full py-4 text-center text-xs tracking-wide text-slate-400">
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

.login-card {
  border-radius: 1rem;
  border: 1px solid rgb(226 232 240 / 0.9);
  background: rgb(255 255 255 / 0.92);
  box-shadow:
    0 4px 6px -1px rgb(15 23 42 / 0.04),
    0 20px 50px -12px rgb(37 99 235 / 0.12);
  backdrop-filter: blur(12px);
}

html.dark .login {
  background: linear-gradient(to bottom right, rgb(15 23 42), rgb(30 41 59), rgb(15 23 42));
}
html.dark .login .login-card {
  border-color: rgb(51 65 85 / 0.6);
  background-color: rgb(255 255 255 / 0.06);
  box-shadow: 0 25px 50px -12px rgb(0 0 0 / 0.35);
}
html.dark .login h1,
html.dark .login h3 {
  color: #f1f5f9;
}
html.dark .login .text-slate-800 {
  color: #f1f5f9;
}
html.dark .login .text-slate-600,
html.dark .login .text-slate-500,
html.dark .login .text-slate-400 {
  color: #94a3b8;
}
</style>
