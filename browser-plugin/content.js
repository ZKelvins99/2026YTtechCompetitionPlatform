// ==============================================================
// CRM 智能表单助手 — Content Script
// 比武项目 2.8 得分点：多字段校验 + 模板保存 + 一键填充
// ==============================================================
// 运行方式：Chrome / Edge 加载已解压的扩展
// 匹配页面：所有 /crm/* 路径
// ==============================================================

(function () {
  'use strict';

  // ──────────────────────── 状态 ────────────────────────
  let toolbarEl = null;        // 浮动工具栏 DOM
  let overlayEl = null;        // 模态浮层 DOM
  let isDialogOpen = false;    // 当前是否有 Element Plus 弹窗打开
  let scanTimer = null;        // 轮询检测弹窗定时器

  // ──────────────────────── 校验规则 ────────────────────────
  // label 匹配规则（支持子串匹配）
  const RULES = [
    { label: '客户名称',  required: true,  validate: v => v.length >= 2 && v.length <= 100, msg: '2~100 个字符' },
    { label: '商机名称',  required: true,  validate: v => v.length >= 2 && v.length <= 100, msg: '2~100 个字符' },
    { label: '合同名称',  required: true,  validate: v => v.length >= 2 && v.length <= 100, msg: '2~100 个字符' },
    { label: '联系电话',  required: true,  validate: v => /^1[3-9]\d{9}$/.test(v.replace(/\s/g, '')), msg: '11 位手机号' },
    { label: '邮箱',      required: false, validate: v => !v || /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v), msg: '邮箱格式不正确' },
    { label: '所属行业',  required: true,  validate: v => v.length > 0, msg: '不能为空' },
    { label: '客户等级',  required: true,  validate: v => v.length > 0, msg: '请选择客户等级' },
    { label: '商机阶段',  required: true,  validate: v => v.length > 0, msg: '请选择阶段' },
    { label: '省份',      required: true,  validate: v => v.length > 0, msg: '不能为空' },
    { label: '预计金额',  required: true,  validate: v => parseFloat(v) > 0, msg: '须大于 0' },
    { label: '合同金额',  required: true,  validate: v => parseFloat(v) > 0, msg: '须大于 0' },
    { label: '详细地址',  required: false, validate: v => !v || v.length >= 5, msg: '建议至少 5 个字符' },
    { label: '合同编号',  required: true,  validate: v => v.length > 0, msg: '不能为空' },
  ];

  // ──────────────────────── UI 注入 ────────────────────────

  function createToolbar() {
    if (document.getElementById('crm-fp-toolbar')) return;
    const div = document.createElement('div');
    div.id = 'crm-fp-toolbar';
    div.className = 'crm-fp-toolbar';
    div.innerHTML = `
      <div class="crm-fp-toolbar-label">📋 表单助手</div>
      <button class="crm-fp-btn crm-fp-btn-primary" data-action="validate">✅ 校验表单</button>
      <button class="crm-fp-btn crm-fp-btn-success" data-action="save-template">💾 保存模板</button>
      <button class="crm-fp-btn crm-fp-btn-warning" data-action="load-template">📂 选择模板</button>
    `;
    div.addEventListener('click', (e) => {
      const btn = e.target.closest('[data-action]');
      if (!btn) return;
      e.stopPropagation();
      switch (btn.dataset.action) {
        case 'validate':       handleValidate();       break;
        case 'save-template':  handleSaveTemplate();   break;
        case 'load-template':  handleLoadTemplate();   break;
      }
    });
    document.body.appendChild(div);
    toolbarEl = div;
    // 延迟显示（确保页面加载）
    setTimeout(() => div.classList.remove('crm-fp-hidden'), 300);
  }

  function hideToolbar() {
    if (toolbarEl) toolbarEl.classList.add('crm-fp-hidden');
  }

  function showToolbar() {
    if (toolbarEl) toolbarEl.classList.remove('crm-fp-hidden');
  }

  // ──────────────────────── 表单字段扫描 ────────────────────────

  /**
   * 获取当前可见弹窗中的所有表单字段
   * @returns {{ label: string, value: string, el: HTMLElement, type: string }[]}
   */
  function getFormFields() {
    const dialog = findVisibleDialog();
    if (!dialog) return [];
    const items = dialog.querySelectorAll('.el-form-item');
    const fields = [];
    items.forEach((item) => {
      const labelEl = item.querySelector('.el-form-item__label');
      if (!labelEl) return;
      const label = labelEl.textContent.trim().replace(/:*\s*$/, '');
      const content = item.querySelector('.el-form-item__content');
      if (!content) return;

      let value = '';
      let type = 'text';

      // el-input
      const input = content.querySelector('input.el-input__inner');
      if (input && input.type !== 'hidden') {
        value = input.value || '';
        type = 'text';
      }
      // el-textarea
      const textarea = content.querySelector('textarea.el-textarea__inner');
      if (textarea) {
        value = textarea.value || '';
        type = 'textarea';
      }
      // el-select — 获取选中的文本
      const selectedItem = content.querySelector('.el-select__selected-item');
      if (selectedItem) {
        value = selectedItem.textContent.trim();
        type = 'select';
      }
      // el-date-picker — input with placeholder-like value
      const dateInput = content.querySelector('.el-date-editor input.el-input__inner');
      if (dateInput && !input) {
        // already handled above if input exists
        value = dateInput.value || '';
        type = 'date';
      }
      // el-input-number — inner input
      const numInput = content.querySelector('.el-input-number input.el-input__inner');
      if (numInput && !input) {
        value = numInput.value || '';
        type = 'number';
      }

      fields.push({ label, value, el: item, type });
    });
    return fields;
  }

  function findVisibleDialog() {
    const dialogs = document.querySelectorAll('.el-dialog');
    for (const d of dialogs) {
      if (d.offsetParent !== null) return d;
    }
    return null;
  }

  // ──────────────────────── 校验 ────────────────────────

  function validateFields(fields) {
    const results = fields.map((f) => {
      const rule = RULES.find(r => f.label.includes(r.label) || r.label.includes(f.label));
      if (!rule) {
        return { ...f, pass: 'skip', msg: '未配置校验规则' };
      }
      if (!rule.required && !f.value) {
        return { ...f, pass: 'skip', msg: '选填项，跳过' };
      }
      const ok = rule.validate(f.value);
      return {
        ...f,
        pass: ok ? 'pass' : 'fail',
        msg: ok ? '✓' : rule.msg,
      };
    });
    return results;
  }

  // ──────────────────────── 校验结果浮层 ────────────────────────

  function showResultModal(results) {
    closeOverlay();
    const passCount = results.filter(r => r.pass === 'pass').length;
    const failCount = results.filter(r => r.pass === 'fail').length;
    const skipCount = results.filter(r => r.pass === 'skip').length;

    const itemsHtml = results.map((r) => {
      const iconClass = r.pass === 'pass' ? 'pass' : r.pass === 'fail' ? 'fail' : 'skip';
      const iconText = r.pass === 'pass' ? '✓' : r.pass === 'fail' ? '✗' : '–';
      return `
        <li class="crm-fp-result-item">
          <div class="crm-fp-result-icon ${iconClass}">${iconText}</div>
          <div style="flex:1;min-width:0">
            <div style="display:flex;gap:12px;align-items:baseline;flex-wrap:wrap">
              <span class="crm-fp-result-field">${escHtml(r.label)}</span>
              <span class="crm-fp-result-value">${escHtml(r.value || '(空)')}</span>
              <span class="crm-fp-result-msg">${escHtml(r.msg)}</span>
            </div>
          </div>
        </li>
      `;
    }).join('');

    const overlay = document.createElement('div');
    overlay.className = 'crm-fp-overlay';
    overlay.innerHTML = `
      <div class="crm-fp-modal">
        <div class="crm-fp-modal-header">
          <span>📋 表单校验结果</span>
          <button class="crm-fp-modal-close" data-action="close">✕</button>
        </div>
        <div class="crm-fp-modal-body">
          <div class="crm-fp-summary">
            <span>共 <span class="num-total">${results.length}</span> 个字段</span>
            <span>✅ <span class="num-pass">${passCount}</span> 通过</span>
            ${failCount > 0 ? `<span>❌ <span class="num-fail">${failCount}</span> 失败</span>` : ''}
            ${skipCount > 0 ? `<span>⏭️ ${skipCount} 跳过</span>` : ''}
          </div>
          <ul class="crm-fp-result-list">${itemsHtml}</ul>
        </div>
        <div class="crm-fp-modal-footer">
          ${failCount === 0 ? '<button class="crm-fp-btn crm-fp-btn-success" data-action="save-after-validate">💾 全部通过，保存为模板</button>' : ''}
          <button class="crm-fp-btn crm-fp-btn-primary" data-action="close">关闭</button>
        </div>
      </div>
    `;
    overlay.addEventListener('click', (e) => {
      const btn = e.target.closest('[data-action]');
      if (!btn) return;
      if (btn.dataset.action === 'close') {
        closeOverlay();
      } else if (btn.dataset.action === 'save-after-validate') {
        closeOverlay();
        // 收集当前字段值
        const fields = getFormFields();
        const data = {};
        fields.forEach(f => { data[f.label] = f.value; });
        showSaveTemplateDialog(data);
      }
    });
    document.body.appendChild(overlay);
    overlayEl = overlay;
  }

  function handleValidate() {
    const fields = getFormFields();
    if (fields.length === 0) {
      showToast('没有找到表单字段，请先打开新增/编辑弹窗', 'warning');
      return;
    }
    const results = validateFields(fields);
    showResultModal(results);
  }

  // ──────────────────────── 模板管理 ────────────────────────

  function showSaveTemplateDialog(prefillData) {
    closeOverlay();
    const overlay = document.createElement('div');
    overlay.className = 'crm-fp-overlay';
    overlay.innerHTML = `
      <div class="crm-fp-modal" style="width:420px">
        <div class="crm-fp-modal-header">
          <span>💾 保存为模板</span>
          <button class="crm-fp-modal-close" data-action="close">✕</button>
        </div>
        <div class="crm-fp-modal-body">
          <div class="crm-fp-save-input">
            <input id="crm-fp-template-name" placeholder="输入模板名称，如：标准客户" />
          </div>
          <div style="font-size:13px;color:#909399">将保存当前表单中 ${Object.keys(prefillData).length} 个字段的值</div>
        </div>
        <div class="crm-fp-modal-footer">
          <button class="crm-fp-btn crm-fp-btn-primary" data-action="confirm-save">保存</button>
          <button class="crm-fp-btn" data-action="close" style="border:1px solid #dcdfe6;background:#fff">取消</button>
        </div>
      </div>
    `;
    overlay.addEventListener('click', async (e) => {
      const btn = e.target.closest('[data-action]');
      if (!btn) return;
      if (btn.dataset.action === 'close') { closeOverlay(); return; }
      if (btn.dataset.action === 'confirm-save') {
        const nameInput = overlay.querySelector('#crm-fp-template-name');
        const name = nameInput.value.trim();
        if (!name) { showToast('请输入模板名称', 'error'); return; }
        await saveTemplate(name, prefillData);
        closeOverlay();
        showToast(`模板「${name}」已保存`, 'success');
      }
    });
    document.body.appendChild(overlay);
    overlayEl = overlay;
    // Auto focus
    setTimeout(() => overlay.querySelector('#crm-fp-template-name').focus(), 100);
  }

  async function handleSaveTemplate() {
    const fields = getFormFields();
    if (fields.length === 0) {
      showToast('没有找到表单字段，请先打开新增/编辑弹窗', 'warning');
      return;
    }
    const data = {};
    fields.forEach(f => { data[f.label] = f.value; });
    showSaveTemplateDialog(data);
  }

  function showTemplateList(templates) {
    closeOverlay();
    const itemsHtml = templates.length === 0
      ? '<div class="crm-fp-empty">暂无保存的模板</div>'
      : templates.map((t, i) => `
        <li class="crm-fp-template-item">
          <div>
            <div class="crm-fp-template-name">${escHtml(t.name)}</div>
            <div class="crm-fp-template-meta">${Object.keys(t.data).length} 个字段 · ${new Date(t.savedAt).toLocaleString()}</div>
          </div>
          <div class="crm-fp-template-actions">
            <button class="crm-fp-template-btn fill" data-action="fill" data-index="${i}">一键填充</button>
            <button class="crm-fp-template-btn del" data-action="delete-template" data-index="${i}">删除</button>
          </div>
        </li>
      `).join('');

    const overlay = document.createElement('div');
    overlay.className = 'crm-fp-overlay';
    overlay.innerHTML = `
      <div class="crm-fp-modal" style="width:480px">
        <div class="crm-fp-modal-header">
          <span>📂 选择模板</span>
          <button class="crm-fp-modal-close" data-action="close">✕</button>
        </div>
        <div class="crm-fp-modal-body">
          <ul class="crm-fp-template-list">${itemsHtml}</ul>
        </div>
        <div class="crm-fp-modal-footer">
          <button class="crm-fp-btn" data-action="close" style="border:1px solid #dcdfe6;background:#fff">关闭</button>
        </div>
      </div>
    `;
    overlay.addEventListener('click', async (e) => {
      const btn = e.target.closest('[data-action]');
      if (!btn) return;
      if (btn.dataset.action === 'close') { closeOverlay(); return; }
      const idx = parseInt(btn.dataset.index, 10);
      if (btn.dataset.action === 'fill') {
        closeOverlay();
        await applyTemplate(templates[idx].data);
        showToast(`已填充模板「${templates[idx].name}」`, 'success');
      } else if (btn.dataset.action === 'delete-template') {
        await deleteTemplate(idx);
        // Refresh
        const updated = await getAllTemplates();
        showTemplateList(updated);
      }
    });
    document.body.appendChild(overlay);
    overlayEl = overlay;
  }

  async function handleLoadTemplate() {
    const templates = await getAllTemplates();
    showTemplateList(templates);
  }

  // ──────────────────────── Chrome Storage ────────────────────────

  const STORAGE_KEY = 'crm_form_templates';

  function getAllTemplates() {
    return new Promise((resolve) => {
      chrome.storage.local.get([STORAGE_KEY], (result) => {
        resolve(result[STORAGE_KEY] || []);
      });
    });
  }

  function saveTemplate(name, data) {
    return new Promise(async (resolve) => {
      const list = await getAllTemplates();
      list.unshift({ name, data, savedAt: Date.now() });
      // 限制最多 50 个
      if (list.length > 50) list.length = 50;
      chrome.storage.local.set({ [STORAGE_KEY]: list }, resolve);
    });
  }

  function deleteTemplate(index) {
    return new Promise(async (resolve) => {
      const list = await getAllTemplates();
      list.splice(index, 1);
      chrome.storage.local.set({ [STORAGE_KEY]: list }, resolve);
    });
  }

  // ──────────────────────── 一键填充 ────────────────────────

  async function applyTemplate(data) {
    const entries = Object.entries(data);
    let filledCount = 0;

    for (const [label, value] of entries) {
      const dialog = findVisibleDialog();
      if (!dialog) break;
      const items = dialog.querySelectorAll('.el-form-item');
      for (const item of items) {
        const labelEl = item.querySelector('.el-form-item__label');
        if (!labelEl) continue;
        const itemLabel = labelEl.textContent.trim().replace(/:*\s*$/, '');
        // 匹配 label（支持包含关系）
        if (!itemLabel.includes(label) && !label.includes(itemLabel)) continue;

        const content = item.querySelector('.el-form-item__content');
        if (!content) continue;

        // el-input
        const input = content.querySelector('input.el-input__inner');
        if (input && input.type !== 'hidden') {
          setNativeValue(input, value);
          filledCount++;
          continue;
        }
        // textarea
        const textarea = content.querySelector('textarea.el-textarea__inner');
        if (textarea) {
          setNativeValue(textarea, value);
          filledCount++;
          continue;
        }
        // el-select — click to expand, then find option
        const selectWrapper = content.querySelector('.el-select__wrapper');
        if (selectWrapper) {
          await selectAndWait(selectWrapper, value);
          filledCount++;
          continue;
        }
      }
    }
    showToast(`已填充 ${filledCount} 个字段`, filledCount > 0 ? 'success' : 'warning');
  }

  /** 设置原生 input/textarea 的值并触发 Vue 响应式更新 */
  function setNativeValue(el, value) {
    // 获取 input / textarea 的原生 value setter（触发 Vue 响应式需要）
    let setter;
    if (el instanceof window.HTMLTextAreaElement) {
      setter = Object.getOwnPropertyDescriptor(window.HTMLTextAreaElement.prototype, 'value');
    } else {
      setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value');
    }
    if (setter && setter.set) {
      setter.set.call(el, value);
    } else {
      el.value = value;
    }
    el.dispatchEvent(new Event('input', { bubbles: true }));
    el.dispatchEvent(new Event('change', { bubbles: true }));
    el.dispatchEvent(new Event('blur', { bubbles: true }));
  }

  /** 点击 el-select 后等待下拉菜单出现，再点匹配项 */
  function selectAndWait(wrapper, targetText) {
    return new Promise((resolve) => {
      // 先点击展开
      wrapper.click();

      let attempts = 0;
      const check = () => {
        attempts++;
        const popper = document.querySelector('.el-select__popper');
        if ((!popper || popper.style.display === 'none' || popper.style.visibility === 'hidden') && attempts < 20) {
          setTimeout(check, 60);
          return;
        }
        if (popper) {
          const options = popper.querySelectorAll('.el-select-dropdown__item');
          let clicked = false;
          options.forEach((opt) => {
            const text = opt.textContent.trim();
            if (text === targetText || targetText.includes(text) || text.includes(targetText)) {
              opt.click();
              clicked = true;
            }
          });
          if (!clicked && options.length > 0) {
            // 尝试用 index>0（跳过 placeholder）
            for (let i = 1; i < options.length; i++) {
              if (options[i].textContent.trim()) {
                options[i].click();
                break;
              }
            }
          }
        }
        // 关闭下拉菜单
        setTimeout(() => {
          document.body.click();
          resolve();
        }, 100);
      };
      setTimeout(check, 80);
    });
  }

  // ──────────────────────── Toast 提示 ────────────────────────

  function showToast(msg, type) {
    const colors = { success: '#67c23a', error: '#f56c6c', warning: '#e6a23c', info: '#909399' };
    const el = document.createElement('div');
    el.style.cssText = `
      position: fixed; top: 20px; left: 50%; transform: translateX(-50%);
      z-index: 100001; background: ${colors[type] || colors.info}; color: #fff;
      padding: 10px 22px; border-radius: 6px; font-size: 14px;
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
      box-shadow: 0 4px 12px rgba(0,0,0,0.15);
      animation: crm-fp-fadeIn 0.2s; max-width: 400px; text-align: center;
    `;
    el.textContent = msg;
    document.body.appendChild(el);
    setTimeout(() => {
      el.style.transition = 'opacity 0.3s';
      el.style.opacity = '0';
      setTimeout(() => el.remove(), 300);
    }, 2500);
  }

  // ──────────────────────── 工具 ────────────────────────

  function closeOverlay() {
    if (overlayEl) { overlayEl.remove(); overlayEl = null; }
  }

  function escHtml(s) {
    if (typeof s !== 'string') return String(s || '');
    return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
  }

  // ──────────────────────── 弹窗检测 ────────────────────────

  function startDialogWatcher() {
    // 每 500ms 检测是否有弹窗打开
    scanTimer = setInterval(() => {
      const dialog = findVisibleDialog();
      const nowOpen = !!dialog;
      if (nowOpen !== isDialogOpen) {
        isDialogOpen = nowOpen;
        if (nowOpen) {
          showToolbar();
        } else {
          // 弹窗关闭时不清空，让用户还能操作
        }
      }
    }, 500);
  }

  // ──────────────────────── 初始化 ────────────────────────

  function init() {
    // 仅在 CRM 页面激活
    if (!window.location.pathname.includes('/crm/')) return;

    createToolbar();
    startDialogWatcher();

    // 点击页面空白或 ESC 关闭浮层
    document.addEventListener('keydown', (e) => {
      if (e.key === 'Escape') closeOverlay();
    });
  }

  // DOM 就绪后启动
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();
