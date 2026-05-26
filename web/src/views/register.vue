<template>
  <div class="register relative flex min-h-screen items-center justify-center overflow-hidden bg-slate-950 px-6 py-12">
    <div class="pointer-events-none absolute inset-0">
      <div class="absolute -right-20 top-20 h-80 w-80 rounded-full bg-violet-500/25 blur-3xl" />
      <div class="absolute bottom-10 left-10 h-72 w-72 rounded-full bg-brand-600/25 blur-3xl" />
      <div
        class="absolute inset-0 opacity-25"
        style="background-image: url('../assets/images/login-background.jpg'); background-size: cover;"
      />
      <div class="absolute inset-0 bg-slate-950/75" />
    </div>

    <div class="tw-card relative z-10 w-full max-w-md p-8 sm:p-10">
      <div class="mb-8 text-center">
        <h3 class="text-2xl font-bold text-slate-800">{{ title }}</h3>
        <p class="mt-2 text-sm text-slate-500">创建新账号，加入智能客户关系管理平台</p>
      </div>

      <el-form ref="registerRef" :model="registerForm" :rules="registerRules" class="tw-input-shell">
        <el-form-item prop="username">
          <el-input v-model="registerForm.username" type="text" size="large" auto-complete="off" placeholder="账号">
            <template #prefix><svg-icon icon-class="user" class="input-icon text-slate-400" /></template>
          </el-input>
        </el-form-item>
        <el-form-item prop="password" :rules="registerPwdValidator">
          <el-input v-model="registerForm.password" type="password" size="large" auto-complete="off" placeholder="密码" show-password @keyup.enter="handleRegister">
            <template #prefix><svg-icon icon-class="password" class="input-icon text-slate-400" /></template>
          </el-input>
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input v-model="registerForm.confirmPassword" type="password" size="large" auto-complete="off" placeholder="确认密码" show-password @keyup.enter="handleRegister">
            <template #prefix><svg-icon icon-class="password" class="input-icon text-slate-400" /></template>
          </el-input>
        </el-form-item>
        <el-form-item v-if="captchaEnabled" prop="code">
          <div class="flex gap-3">
            <el-input v-model="registerForm.code" size="large" auto-complete="off" placeholder="验证码" class="flex-1" @keyup.enter="handleRegister">
              <template #prefix><svg-icon icon-class="validCode" class="input-icon text-slate-400" /></template>
            </el-input>
            <img :src="codeUrl" class="h-10 w-[30%] cursor-pointer rounded-xl border border-slate-200 object-cover hover:border-brand-400" alt="验证码" @click="getCode" />
          </div>
        </el-form-item>

        <el-form-item class="!mb-2">
          <button type="button" class="tw-btn-primary disabled:opacity-60" :disabled="loading" @click.prevent="handleRegister">
            <span v-if="!loading">注 册</span>
            <span v-else>注册中...</span>
          </button>
        </el-form-item>
        <div class="text-center text-sm">
          <router-link class="font-medium text-brand-600 hover:text-brand-700" to="/login">使用已有账户登录</router-link>
        </div>
      </el-form>
    </div>

    <div class="absolute bottom-0 z-10 w-full py-4 text-center text-xs tracking-wide text-slate-500">
      <span>{{ footerContent }}</span>
    </div>
  </div>
</template>

<script setup>
import { ElMessageBox } from "element-plus"
import { getCodeImg, register } from "@/api/login"
import defaultSettings from '@/settings'
import { usePasswordRule } from "@/utils/passwordRule"

const title = import.meta.env.VITE_APP_TITLE
const footerContent = defaultSettings.footerContent
const router = useRouter()
const { proxy } = getCurrentInstance()
const { registerPwdValidator } = usePasswordRule()

const registerForm = ref({
  username: "",
  password: "",
  confirmPassword: "",
  code: "",
  uuid: ""
})

const equalToPassword = (rule, value, callback) => {
  if (registerForm.value.password !== value) {
    callback(new Error("两次输入的密码不一致"))
  } else {
    callback()
  }
}

const registerRules = {
  username: [
    { required: true, trigger: "blur", message: "请输入您的账号" },
    { min: 2, max: 20, message: "用户账号长度必须介于 2 和 20 之间", trigger: "blur" }
  ],
  confirmPassword: [
    { required: true, trigger: "blur", message: "请再次输入您的密码" },
    { required: true, validator: equalToPassword, trigger: "blur" }
  ],
  code: [{ required: true, trigger: "change", message: "请输入验证码" }]
}

const codeUrl = ref("")
const loading = ref(false)
const captchaEnabled = ref(true)

function handleRegister() {
  proxy.$refs.registerRef.validate(valid => {
    if (valid) {
      loading.value = true
      register(registerForm.value).then(res => {
        const username = registerForm.value.username
        ElMessageBox.alert("<font color='red'>恭喜你，您的账号 " + username + " 注册成功！</font>", "系统提示", {
          dangerouslyUseHTMLString: true,
          type: "success",
        }).then(() => {
          router.push("/login")
        }).catch(() => {})
      }).catch(() => {
        loading.value = false
        if (captchaEnabled) {
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
      registerForm.value.uuid = res.uuid
    }
  })
}

getCode()
</script>

<style scoped>
.input-icon { height: 1rem; width: 1rem; }
:deep(.el-form-item) { margin-bottom: 18px; }
html.dark .register .tw-card {
  border-color: rgb(51 65 85 / 0.5);
  background-color: rgb(15 23 42 / 0.9);
}
html.dark .register h3 { color: #f1f5f9; }
</style>
