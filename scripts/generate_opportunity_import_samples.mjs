/**
 * 生成商机 Excel 导入样例（无错误 / 含各类校验错误）
 * 用法: node scripts/generate_opportunity_import_samples.mjs
 */
import { mkdirSync, writeFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
// 依赖：在仓库根目录执行 npm install xlsx --no-save，或已全局安装 xlsx
import XLSX from 'xlsx'

const __dirname = dirname(fileURLToPath(import.meta.url))
const outDir = join(__dirname, '..', 'data', 'opportunity-import')

const HEADERS = ['商机名称', '客户名称', '阶段编码', '预计金额', '预计成交日期']

/** 与 SQL/08_opportunity_import_test_data.sql 中客户名称一致 */
export const TEST_CUSTOMERS = [
  'CRM导入测试客户A',
  'CRM导入测试客户B',
  'CRM导入测试客户C'
]

const validRows = [
  ['云原生平台升级项目', 'CRM导入测试客户A', 'QUOTE', '150000', '2026-08-30'],
  ['智能制造MES二期', 'CRM导入测试客户B', 'ANALYSIS', '280000.50', '2026-09-15'],
  ['年度维保服务续约', 'CRM导入测试客户C', 'NEGOTIATE', '36000', '2026-06-30'],
  ['智慧园区弱电改造', 'CRM导入测试客户A', 'CONTACT', '89000', '2026-10-01'],
  ['数据中台咨询', 'CRM导入测试客户B', 'WIN', '520000', '2026-12-31']
]

/** 每行预期导入结果说明（仅供 README） */
const errorRows = [
  // 对照行：全部合法（成功 1）
  ['【对照】合法样例行', 'CRM导入测试客户A', 'CONTACT', '10000', '2026-07-01'],
  // 必填为空（失败，列 1）
  ['', 'CRM导入测试客户A', 'QUOTE', '5000', '2026-07-02'],
  // 必填为空（失败，列 2）
  ['缺少客户名称商机', '', 'ANALYSIS', '8000', '2026-07-03'],
  // 同行多错误：名称+客户都空（失败，列 1、2 各一条）
  ['', '', 'QUOTE', '1000', '2026-07-04'],
  // 金额格式（失败，列 4）
  ['金额格式错误商机', 'CRM导入测试客户A', 'QUOTE', '十二万五', '2026-07-05'],
  // 阶段编码（失败，列 3）
  ['阶段编码错误商机', 'CRM导入测试客户B', 'NOT_A_STAGE', '12000', '2026-07-06'],
  // 日期格式（失败，列 5）
  ['日期格式错误商机', 'CRM导入测试客户C', 'NEGOTIATE', '9000', '2026年7月7日'],
  // Excel 内重复：第 8、9 行相同 商机名称+客户名称（第 9 行失败，列 1）
  ['Excel内重复商机', 'CRM导入测试客户B', 'CONTACT', '3000', '2026-07-08'],
  ['Excel内重复商机', 'CRM导入测试客户B', 'CONTACT', '6000', '2026-07-09'],
  // 客户不存在（失败，列 2）
  ['客户不存在商机', '该公司不存在于CRM系统', 'QUOTE', '4000', '2026-07-10'],
  // 数据库已存在跳过：需先导入「无错误」文件中的首行（跳过，列 1，type=skip）
  ['云原生平台升级项目', 'CRM导入测试客户A', 'QUOTE', '150000', '2026-08-30']
]

function writeWorkbook(filename, rows) {
  const sheet = XLSX.utils.aoa_to_sheet([HEADERS, ...rows])
  sheet['!cols'] = [{ wch: 28 }, { wch: 22 }, { wch: 14 }, { wch: 12 }, { wch: 14 }]
  const wb = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(wb, sheet, '商机导入')
  const path = join(outDir, filename)
  XLSX.writeFile(wb, path)
  console.log('已生成:', path)
}

mkdirSync(outDir, { recursive: true })
writeWorkbook('商机导入_无错误样例.xlsx', validRows)
writeWorkbook('商机导入_校验与去重样例.xlsx', errorRows)

const readme = `# 商机 Excel 导入测试样例

路径：\`data/opportunity-import/\`

## 使用前准备

1. 在数据库执行 \`SQL/08_opportunity_import_test_data.sql\`，插入 3 个测试客户（名称须与 Excel 一致）。
2. 或自行在 **客户管理** 中新建客户，并把 Excel 里的「客户名称」改成你系统中已有的名称。

## 文件说明

| 文件 | 用途 |
|------|------|
| \`商机导入_无错误样例.xlsx\` | 5 条合法数据，应全部导入成功 |
| \`商机导入_校验与去重样例.xlsx\` | 覆盖格式校验、Excel 去重、客户不存在、库内重复跳过 |

## 无错误样例预期

- 成功：5
- 失败：0
- 跳过：0

## 校验样例预期（先执行无错误样例再导入本文件）

| Excel 行号 | 场景 | 列号 | 说明摘要 |
|------------|------|------|----------|
| 2 | 对照合法行 | - | 导入成功（绿色） |
| 3 | 商机名称为空 | 1 | 商机名称不能为空 |
| 4 | 客户名称为空 | 2 | 客户名称不能为空 |
| 5 | 双必填为空 | 1、2 | 两条错误 |
| 6 | 金额非数字 | 4 | 预计金额须为数字 |
| 7 | 阶段不存在 | 3 | 阶段编码不存在 |
| 8 | 日期格式错误 | 5 | 应为 yyyy-MM-dd |
| 9 | Excel 内重复首条 | - | 可成功或已存在则跳过 |
| 10 | Excel 内重复 | 1 | 与第 9 行重复 |
| 11 | 客户不存在 | 2 | 客户名称不存在 |
| 12 | 库内重复 | 1 | 数据库已存在相同记录（跳过，橙色） |

阶段编码可选值：\`CONTACT\` \`ANALYSIS\` \`QUOTE\` \`NEGOTIATE\` \`WIN\` \`LOST\`

页面：http://localhost:5173/crm/opportunity → **导入 Excel**
`

writeFileSync(join(outDir, 'README.md'), readme, 'utf8')
console.log('已生成:', join(outDir, 'README.md'))
