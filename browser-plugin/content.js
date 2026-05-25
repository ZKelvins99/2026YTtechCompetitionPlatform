// ==============================================================
// CRM 智能表单助手 — Content Script
// 比武项目 2.8 得分点：多字段校验 + 模板保存 + 一键填充
// ==============================================================

(function () {
  'use strict';

  const STORAGE_KEY = 'crm_form_templates';
  const TOOLBAR_POS_KEY = 'crm_fp_toolbar_pos';

  let toolbarEl = null;
  let overlayEl = null;
  let isDialogOpen = false;
  let scanTimer = null;

  /** 11 位数字即可（如 12345678910）；不含空格与横线 */
  function isPhone11Digits(raw) {
    const digits = String(raw || '').replace(/[\s\-()]/g, '');
    return /^\d{11}$/.test(digits);
  }

  const RULES = [
    { label: '客户名称', required: true, validate: v => v.length >= 2 && v.length <= 100, msg: '2~100 个字符' },
    { label: '商机名称', required: true, validate: v => v.length >= 2 && v.length <= 100, msg: '2~100 个字符' },
    { label: '合同名称', required: true, validate: v => v.length >= 2 && v.length <= 100, msg: '2~100 个字符' },
    { label: '联系电话', required: true, validate: v => isPhone11Digits(v), msg: '须为 11 位数字' },
    { label: '邮箱', required: false, validate: v => !v || /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v), msg: '邮箱格式不正确' },
    { label: '所属行业', required: true, validate: v => v.length > 0, msg: '不能为空' },
    { label: '客户等级', required: true, validate: v => v.length > 0, msg: '请选择客户等级' },
    { label: '商机阶段', required: true, validate: v => v.length > 0, msg: '请选择阶段' },
    { label: '省份', required: true, validate: v => v.length > 0, msg: '不能为空' },
    { label: '预计金额', required: true, validate: v => parseFloat(v) > 0, msg: '须大于 0' },
    { label: '合同金额', required: true, validate: v => parseFloat(v) > 0, msg: '须大于 0' },
    { label: '详细地址', required: false, validate: v => !v || v.length >= 5, msg: '建议至少 5 个字符' },
    { label: '合同编号', required: true, validate: v => v.length > 0, msg: '不能为空' },
  ];

  // ──────────────────────── 工具栏（可拖动） ────────────────────────

  function createToolbar() {
    if (document.getElementById('crm-fp-toolbar')) return;
    const div = document.createElement('div');
    div.id = 'crm-fp-toolbar';
    div.className = 'crm-fp-toolbar crm-fp-hidden';
    div.innerHTML = `
      <div class="crm-fp-drag-handle" title="按住拖动">⠿</div>
      <div class="crm-fp-toolbar-head">
        <span class="crm-fp-toolbar-label">📋 表单助手</span>
        <button type="button" class="crm-fp-mini-btn" data-action="collapse" title="收起">−</button>
      </div>
      <div class="crm-fp-toolbar-body">
        <button type="button" class="crm-fp-btn crm-fp-btn-primary" data-action="validate">✅ 校验表单</button>
        <button type="button" class="crm-fp-btn crm-fp-btn-success" data-action="save-template">💾 保存模板</button>
        <button type="button" class="crm-fp-btn crm-fp-btn-warning" data-action="load-template">📂 选择模板</button>
      </div>
    `;
    div.addEventListener('click', (e) => {
      const btn = e.target.closest('[data-action]');
      if (!btn) return;
      e.stopPropagation();
      if (btn.dataset.action === 'collapse') {
        div.classList.toggle('crm-fp-collapsed');
        return;
      }
      switch (btn.dataset.action) {
        case 'validate': handleValidate(); break;
        case 'save-template': handleSaveTemplate(); break;
        case 'load-template': handleLoadTemplate(); break;
      }
    });
    document.body.appendChild(div);
    toolbarEl = div;
    restoreToolbarPosition(div);
    makeToolbarDraggable(div);
    setTimeout(() => div.classList.remove('crm-fp-hidden'), 300);
  }

  function restoreToolbarPosition(el) {
    try {
      const raw = localStorage.getItem(TOOLBAR_POS_KEY);
      if (!raw) return;
      const pos = JSON.parse(raw);
      if (typeof pos.left === 'number' && typeof pos.top === 'number') {
        el.style.left = `${pos.left}px`;
        el.style.top = `${pos.top}px`;
        el.style.right = 'auto';
      }
    } catch (_) { /* ignore */ }
  }

  function saveToolbarPosition(el) {
    const rect = el.getBoundingClientRect();
    localStorage.setItem(TOOLBAR_POS_KEY, JSON.stringify({
      left: Math.round(rect.left),
      top: Math.round(rect.top)
    }));
  }

  function makeToolbarDraggable(el) {
    const handle = el.querySelector('.crm-fp-drag-handle');
    if (!handle) return;

    let dragging = false;
    let startX = 0;
    let startY = 0;
    let startLeft = 0;
    let startTop = 0;

    const onMove = (e) => {
      if (!dragging) return;
      const dx = e.clientX - startX;
      const dy = e.clientY - startY;
      let left = startLeft + dx;
      let top = startTop + dy;
      const maxLeft = window.innerWidth - el.offsetWidth - 4;
      const maxTop = window.innerHeight - el.offsetHeight - 4;
      left = Math.max(4, Math.min(left, maxLeft));
      top = Math.max(4, Math.min(top, maxTop));
      el.style.left = `${left}px`;
      el.style.top = `${top}px`;
      el.style.right = 'auto';
    };

    const onUp = () => {
      if (!dragging) return;
      dragging = false;
      document.removeEventListener('mousemove', onMove);
      document.removeEventListener('mouseup', onUp);
      el.classList.remove('crm-fp-dragging');
      saveToolbarPosition(el);
    };

    handle.addEventListener('mousedown', (e) => {
      if (e.button !== 0) return;
      e.preventDefault();
      e.stopPropagation();
      const rect = el.getBoundingClientRect();
      dragging = true;
      startX = e.clientX;
      startY = e.clientY;
      startLeft = rect.left;
      startTop = rect.top;
      el.style.left = `${startLeft}px`;
      el.style.top = `${startTop}px`;
      el.style.right = 'auto';
      el.classList.add('crm-fp-dragging');
      document.addEventListener('mousemove', onMove);
      document.addEventListener('mouseup', onUp);
    });
  }

  function showToolbar() {
    if (toolbarEl) toolbarEl.classList.remove('crm-fp-hidden');
  }

  // ──────────────────────── 可见性与弹窗 ────────────────────────

  function isElementVisible(el) {
    if (!el || !el.isConnected) return false;
    const style = window.getComputedStyle(el);
    if (style.display === 'none' || style.visibility === 'hidden') return false;
    if (parseFloat(style.opacity) === 0) return false;
    const rect = el.getBoundingClientRect();
    return rect.width > 0 || rect.height > 0;
  }

  function findVisibleDialog() {
    const overlays = document.querySelectorAll('.el-overlay-dialog');
    for (const ov of overlays) {
      if (!isElementVisible(ov)) continue;
      const dialog = ov.querySelector('.el-dialog');
      if (dialog && isElementVisible(dialog)) return dialog;
    }
    const dialogs = document.querySelectorAll('.el-dialog');
    for (const d of dialogs) {
      if (isElementVisible(d)) return d;
    }
    return null;
  }

  function sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
  }

  // ──────────────────────── 字段识别（Element Plus） ────────────────────────

  function readSelectDisplayValue(content) {
    const selected = content.querySelectorAll(
      '.el-select__selected-item:not(.el-select__placeholder):not(.is-transparent)'
    );
    if (selected.length) {
      const texts = Array.from(selected)
        .map(n => (n.textContent || '').trim())
        .filter(t => t && t !== '请选择' && !t.startsWith('请'));
      if (texts.length) return texts.join(', ');
    }
    const tags = content.querySelector('.el-select__tags-text');
    if (tags) return tags.textContent.trim();
    const caret = content.querySelector('.el-select__caret');
    const wrapper = content.querySelector('.el-select__wrapper, .el-select .el-input__wrapper');
    if (wrapper) {
      const clone = wrapper.cloneNode(true);
      clone.querySelectorAll('.el-select__caret, .el-icon, input').forEach(n => n.remove());
      const t = (clone.textContent || '').trim();
      if (t && t !== '请选择') return t;
    }
    return '';
  }

  function detectFieldControl(content) {
    const selectRoot = content.querySelector('.el-select');
    if (selectRoot) {
      return { type: 'select', value: readSelectDisplayValue(content), content };
    }
    const dateEditor = content.querySelector('.el-date-editor');
    if (dateEditor) {
      const inp = dateEditor.querySelector('input.el-input__inner');
      return { type: 'date', value: inp?.value || '', inputEl: inp, content };
    }
    const numWrap = content.querySelector('.el-input-number');
    if (numWrap) {
      const inp = numWrap.querySelector('input.el-input__inner');
      return { type: 'number', value: inp?.value || '', inputEl: inp, content };
    }
    const textarea = content.querySelector('textarea.el-textarea__inner');
    if (textarea) {
      return { type: 'textarea', value: textarea.value || '', inputEl: textarea, content };
    }
    const inputs = content.querySelectorAll('input.el-input__inner');
    for (const inp of inputs) {
      if (inp.type === 'hidden' || inp.closest('.el-select')) continue;
      return { type: 'text', value: inp.value || '', inputEl: inp, content };
    }
    return null;
  }

  function collectFormFields(dialog) {
    if (!dialog) return [];
    const fields = [];
    dialog.querySelectorAll('.el-form-item').forEach((item) => {
      const labelEl = item.querySelector('.el-form-item__label');
      if (!labelEl) return;
      const label = labelEl.textContent.trim().replace(/[*:：\s]+$/g, '').replace(/^\*/, '');
      const content = item.querySelector('.el-form-item__content');
      if (!content) return;
      const control = detectFieldControl(content);
      if (!control) return;
      fields.push({
        label,
        value: control.value,
        type: control.type,
        el: item,
        content,
        control
      });
    });
    return fields;
  }

  function getFormFields() {
    return collectFormFields(findVisibleDialog());
  }

  function runFormValidation(dialog) {
    const fields = collectFormFields(dialog || findVisibleDialog());
    if (!fields.length) {
      return { ok: true, results: [], fields: [] };
    }
    const results = validateFields(fields);
    const ok = !results.some(r => r.pass === 'fail');
    return { ok, results, fields };
  }

  function labelsMatch(itemLabel, templateLabel) {
    return itemLabel === templateLabel
      || itemLabel.includes(templateLabel)
      || templateLabel.includes(itemLabel);
  }

  function findFormItemByLabel(dialog, label) {
    for (const item of dialog.querySelectorAll('.el-form-item')) {
      const labelEl = item.querySelector('.el-form-item__label');
      if (!labelEl) continue;
      const itemLabel = labelEl.textContent.trim().replace(/[*:：\s]+$/g, '').replace(/^\*/, '');
      if (labelsMatch(itemLabel, label)) return item;
    }
    return null;
  }

  // ──────────────────────── 校验 ────────────────────────

  function validateFields(fields) {
    return fields.map((f) => {
      const rule = RULES.find(r => f.label.includes(r.label) || r.label.includes(f.label));
      if (!rule) return { ...f, pass: 'skip', msg: '未配置校验规则' };
      if (!rule.required && !f.value) return { ...f, pass: 'skip', msg: '选填项，跳过' };
      const ok = rule.validate(f.value);
      return { ...f, pass: ok ? 'pass' : 'fail', msg: ok ? '✓' : rule.msg };
    });
  }

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
          <button type="button" class="crm-fp-modal-close" data-action="close">✕</button>
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
          ${failCount === 0 ? '<button type="button" class="crm-fp-btn crm-fp-btn-success" data-action="save-after-validate">💾 全部通过，保存为模板</button>' : ''}
          <button type="button" class="crm-fp-btn crm-fp-btn-primary" data-action="close">关闭</button>
        </div>
      </div>
    `;
    overlay.addEventListener('click', (e) => {
      const btn = e.target.closest('[data-action]');
      if (!btn) return;
      if (btn.dataset.action === 'close') closeOverlay();
      else if (btn.dataset.action === 'save-after-validate') {
        closeOverlay();
        const data = {};
        getFormFields().forEach(f => { data[f.label] = f.value; });
        showSaveTemplateDialog(data);
      }
    });
    document.body.appendChild(overlay);
    overlayEl = overlay;
  }

  function handleValidate() {
    const { results, fields } = runFormValidation();
    if (fields.length === 0) {
      showToast('没有找到表单字段，请先打开新增/编辑弹窗', 'warning');
      return;
    }
    showResultModal(results);
  }

  /** 拦截弹窗「确定/保存」：有配置规则且存在失败项时不允许提交 */
  function guardFormSubmit(e) {
    const btn = e.target?.closest?.('.el-dialog__footer .el-button--primary');
    if (!btn) return;
    if (btn.closest('.crm-fp-overlay, #crm-fp-toolbar')) return;

    const dialog = btn.closest('.el-dialog');
    if (!dialog || !isElementVisible(dialog)) return;

    const { ok, results, fields } = runFormValidation(dialog);
    if (fields.length === 0) return;

    const hasRule = results.some(r => r.pass !== 'skip');
    if (!hasRule) return;

    if (!ok) {
      e.preventDefault();
      e.stopPropagation();
      e.stopImmediatePropagation();
      showResultModal(results);
      showToast('表单校验未通过，无法提交', 'error');
    }
  }

  // ──────────────────────── 模板 ────────────────────────

  function showSaveTemplateDialog(prefillData) {
    closeOverlay();
    const overlay = document.createElement('div');
    overlay.className = 'crm-fp-overlay';
    overlay.innerHTML = `
      <div class="crm-fp-modal" style="width:420px">
        <div class="crm-fp-modal-header">
          <span>💾 保存为模板</span>
          <button type="button" class="crm-fp-modal-close" data-action="close">✕</button>
        </div>
        <div class="crm-fp-modal-body">
          <div class="crm-fp-save-input">
            <input id="crm-fp-template-name" placeholder="输入模板名称，如：标准客户" />
          </div>
          <div style="font-size:13px;color:#909399">将保存当前表单中 ${Object.keys(prefillData).length} 个字段的值</div>
        </div>
        <div class="crm-fp-modal-footer">
          <button type="button" class="crm-fp-btn crm-fp-btn-primary" data-action="confirm-save">保存</button>
          <button type="button" class="crm-fp-btn" data-action="close" style="border:1px solid #dcdfe6;background:#fff">取消</button>
        </div>
      </div>
    `;
    overlay.addEventListener('click', async (e) => {
      const btn = e.target.closest('[data-action]');
      if (!btn) return;
      if (btn.dataset.action === 'close') { closeOverlay(); return; }
      if (btn.dataset.action === 'confirm-save') {
        const name = overlay.querySelector('#crm-fp-template-name').value.trim();
        if (!name) { showToast('请输入模板名称', 'error'); return; }
        await saveTemplate(name, prefillData);
        closeOverlay();
        showToast(`模板「${name}」已保存`, 'success');
      }
    });
    document.body.appendChild(overlay);
    overlayEl = overlay;
    setTimeout(() => overlay.querySelector('#crm-fp-template-name')?.focus(), 100);
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
            <button type="button" class="crm-fp-template-btn fill" data-action="fill" data-index="${i}">一键填充</button>
            <button type="button" class="crm-fp-template-btn del" data-action="delete-template" data-index="${i}">删除</button>
          </div>
        </li>
      `).join('');

    const overlay = document.createElement('div');
    overlay.className = 'crm-fp-overlay';
    overlay.innerHTML = `
      <div class="crm-fp-modal" style="width:480px">
        <div class="crm-fp-modal-header">
          <span>📂 选择模板</span>
          <button type="button" class="crm-fp-modal-close" data-action="close">✕</button>
        </div>
        <div class="crm-fp-modal-body">
          <ul class="crm-fp-template-list">${itemsHtml}</ul>
        </div>
        <div class="crm-fp-modal-footer">
          <button type="button" class="crm-fp-btn" data-action="close" style="border:1px solid #dcdfe6;background:#fff">关闭</button>
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
        const n = await applyTemplate(templates[idx].data);
        showToast(n > 0 ? `已填充 ${n} 个字段` : '未匹配到可填充字段', n > 0 ? 'success' : 'warning');
      } else if (btn.dataset.action === 'delete-template') {
        await deleteTemplate(idx);
        showTemplateList(await getAllTemplates());
      }
    });
    document.body.appendChild(overlay);
    overlayEl = overlay;
  }

  async function handleLoadTemplate() {
    showTemplateList(await getAllTemplates());
  }

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

  // ──────────────────────── 填充 ────────────────────────

  function setNativeValue(el, value) {
    if (!el) return;
    let setter;
    if (el instanceof window.HTMLTextAreaElement) {
      setter = Object.getOwnPropertyDescriptor(window.HTMLTextAreaElement.prototype, 'value');
    } else {
      setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value');
    }
    if (setter?.set) setter.set.call(el, value);
    else el.value = value;
    el.dispatchEvent(new Event('input', { bubbles: true }));
    el.dispatchEvent(new Event('change', { bubbles: true }));
  }

  function isPopperVisible(popper) {
    if (!popper || !popper.isConnected) return false;
    if (popper.getAttribute('aria-hidden') === 'true') return false;
    const style = window.getComputedStyle(popper);
    if (style.display === 'none' || style.visibility === 'hidden') return false;
    const rect = popper.getBoundingClientRect();
    return rect.width > 0 && rect.height > 0;
  }

  function findOpenSelectPopper() {
    const poppers = document.querySelectorAll('.el-select__popper.el-popper, .el-popper.el-select__popper');
    for (let i = poppers.length - 1; i >= 0; i--) {
      if (isPopperVisible(poppers[i])) return poppers[i];
    }
    return null;
  }

  function clickOption(optionEl) {
    optionEl.dispatchEvent(new MouseEvent('mousedown', { bubbles: true, cancelable: true, view: window }));
    optionEl.click();
  }

  /** Close el-select popper only — never document.body / Escape (closes el-dialog). */
  async function closeSelectDropdown(trigger, dialog) {
    await sleep(80);
    if (!findOpenSelectPopper()) return;

    const hostDialog = dialog || trigger?.closest?.('.el-dialog');

    // EP 2.x: toggle trigger or click inside dialog body (outside popper) closes dropdown.
    trigger.dispatchEvent(new MouseEvent('mousedown', { bubbles: true, cancelable: true, view: window }));
    trigger.click();
    await sleep(80);
    if (!findOpenSelectPopper()) return;

    if (hostDialog) {
      const clickTarget = hostDialog.querySelector('.el-dialog__body') || hostDialog;
      clickTarget.dispatchEvent(new MouseEvent('mousedown', { bubbles: true, cancelable: true, view: window }));
      clickTarget.dispatchEvent(new MouseEvent('mouseup', { bubbles: true, cancelable: true, view: window }));
      clickTarget.dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true, view: window }));
      await sleep(80);
      if (!findOpenSelectPopper()) return;
    }

    const inp = trigger.querySelector('input')
      || trigger.closest('.el-select')?.querySelector('input');
    if (inp && document.activeElement === inp) {
      inp.blur();
      await sleep(80);
    }
  }

  async function fillSelect(content, targetText, dialog) {
    const select = content.querySelector('.el-select');
    if (!select || !targetText) return false;

    const trigger = content.querySelector('.el-select__wrapper')
      || content.querySelector('.el-select .el-input__wrapper')
      || content.querySelector('.el-select .el-input')
      || select;
    trigger.dispatchEvent(new MouseEvent('mousedown', { bubbles: true, cancelable: true, view: window }));
    trigger.click();

    let popper = null;
    for (let i = 0; i < 30; i++) {
      await sleep(50);
      popper = findOpenSelectPopper();
      if (popper) break;
    }
    if (!popper) return false;

    const options = popper.querySelectorAll(
      '.el-select-dropdown__item:not(.is-disabled), .el-option:not(.is-disabled)'
    );
    const target = String(targetText).trim();
    let matched = null;
    for (const opt of options) {
      const text = (opt.textContent || '').trim();
      if (!text || text === '请选择' || text === '无数据') continue;
      if (text === target || text.includes(target) || target.includes(text)) {
        matched = opt;
        break;
      }
    }
    if (!matched && options.length) {
      matched = options[0];
    }
    if (!matched) return false;

    clickOption(matched);
    await closeSelectDropdown(trigger, dialog);
    return true;
  }

  async function fillFieldControl(control, value, dialog) {
    if (!control || value == null || value === '') return false;
    const strVal = String(value);

    if (control.type === 'select') {
      return fillSelect(control.content, strVal, dialog);
    }
    if (control.inputEl) {
      setNativeValue(control.inputEl, strVal);
      return true;
    }
    return false;
  }

  async function applyTemplate(data) {
    const dialog = findVisibleDialog();
    if (!dialog) {
      showToast('请先打开表单弹窗', 'warning');
      return 0;
    }

    let filledCount = 0;
    for (const [label, value] of Object.entries(data)) {
      const item = findFormItemByLabel(dialog, label);
      if (!item) continue;
      const content = item.querySelector('.el-form-item__content');
      if (!content) continue;
      const control = detectFieldControl(content);
      if (!control) continue;
      const ok = await fillFieldControl(control, value, dialog);
      if (ok) filledCount++;
      await sleep(80);
    }
    return filledCount;
  }

  // ──────────────────────── UI 工具 ────────────────────────

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

  function closeOverlay() {
    if (overlayEl) { overlayEl.remove(); overlayEl = null; }
  }

  function escHtml(s) {
    if (typeof s !== 'string') return String(s || '');
    return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
  }

  function startDialogWatcher() {
    scanTimer = setInterval(() => {
      const nowOpen = !!findVisibleDialog();
      if (nowOpen !== isDialogOpen) {
        isDialogOpen = nowOpen;
        if (nowOpen) showToolbar();
      }
    }, 500);
  }

  function init() {
    if (!window.location.pathname.includes('/crm/')) return;
    createToolbar();
    startDialogWatcher();
    document.addEventListener('click', guardFormSubmit, true);
    document.addEventListener('keydown', (e) => {
      if (e.key === 'Escape') closeOverlay();
    });
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();
