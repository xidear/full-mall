#!/usr/bin/env node
/**
 * 权限说明生成器：把"权限说明"和"权限真正拦截的接口"合成一份可被页面/手册共用的产物。
 *
 * 为什么需要它：权限页上的说明一旦手写，就会和 PermissionService 里的路径映射各说一套
 * （取消某个权限后实际发生了什么，只有那段 if 链知道）。所以：
 *   - 说明：只允许写在 docs/permissions.catalog.json；
 *   - 接口映射：从 PermissionService.requiredPermission() 解析，不手抄；
 *   - 三方集合（catalog / 迁移播种 / 映射）必须一致，否则以非零码失败。
 *
 * 产物：
 *   - web/packages/api-client/src/permissions.generated.ts （页面 tooltip 用）
 *   - docs/PERMISSIONS.md                                 （内部帮助手册）
 *
 * 用法：
 *   node scripts/generate-permission-help.mjs            # 生成
 *   node scripts/generate-permission-help.mjs --check    # 只校验产物是否最新（CI/契约检查用）
 */
import { readFileSync, writeFileSync, readdirSync, existsSync } from 'node:fs'
import { join, dirname } from 'node:path'
import { fileURLToPath } from 'node:url'

const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..')
const CATALOG = join(ROOT, 'docs/permissions.catalog.json')
const SERVICE = join(ROOT, 'server/src/main/java/com/fullmall/permission/app/PermissionService.java')
const MIGRATIONS = join(ROOT, 'server/src/main/resources/db/migration')
const TS_OUT = join(ROOT, 'web/packages/api-client/src/permissions.generated.ts')
const MD_OUT = join(ROOT, 'docs/PERMISSIONS.md')

const checkOnly = process.argv.includes('--check')
const problems = []
const fail = message => problems.push(message)

/** 迁移里的权限播种：code/label/group/sort_order 的真值。 */
function readSeeds() {
  const seeds = new Map()
  const files = readdirSync(MIGRATIONS).filter(name => /^V\d+__.*\.sql$/.test(name))
    .sort((a, b) => Number(a.slice(1).split('__')[0]) - Number(b.slice(1).split('__')[0]))
  for (const file of files) {
    const sql = readFileSync(join(MIGRATIONS, file), 'utf8')
    if (!/INSERT INTO permission\s*\(/i.test(sql)) continue
    const tuple = /\('([a-z][a-z0-9._]*)',\s*'([^']*)',\s*'([^']*)',\s*(\d+)\)/g
    for (const match of sql.matchAll(tuple)) {
      seeds.set(match[1], { label: match[2], group: match[3], sortOrder: Number(match[4]), file })
    }
  }
  return seeds
}

/** 路径 → 权限：运行时真正生效的映射，唯一可信来源。 */
function readPathMap() {
  const text = readFileSync(SERVICE, 'utf8')
  const pairs = new Map()
  for (const match of text.matchAll(/path\.startsWith\("([^"]+)"\)\)?\s*return\s+"([^"]+)";/g)) {
    const [, prefix, code] = match
    pairs.set(prefix, code)
  }
  const fallbackMatch = [...text.matchAll(/^\s*return\s+"([^"]+)";\s*$/gm)].pop()
  return { pairs, fallback: fallbackMatch ? fallbackMatch[1] : null }
}

/**
 * 该权限实际拦截的接口前缀。
 *
 * 后缀要先判断"被更长前缀抢走"的情况：/admin/v1/distribution/** 被 /admin/v1/distribution/withdrawals/**
 * 覆盖，但两者是不同权限（promotion.manage 与 withdrawal.review）。不标注的话，
 * 说明会写成"promotion.manage 管提现"，与运行时行为相反（运行时先匹配更长的 withdrawals）。
 */
function endpointList(code, map) {
  const own = [...map.pairs.entries()].filter(([, value]) => value === code).map(([prefix]) => prefix)
  return own.map(prefix => {
    const shadows = [...map.pairs.keys()]
      .filter(other => other.length > prefix.length && other.startsWith(prefix) && map.pairs.get(other) !== code)
    return prefix + '/**' + (shadows.length ? '（不含 ' + shadows.map(item => item + '/**').join('、') + '）' : '')
  })
}

const catalog = JSON.parse(readFileSync(CATALOG, 'utf8'))
const seeds = readSeeds()
const { pairs, fallback } = readPathMap()

const catalogCodes = new Set(catalog.permissions.map(item => item.code))
const seedCodes = new Set(seeds.keys())

for (const code of seedCodes) {
  if (!catalogCodes.has(code)) {
    fail(`权限 ${code} 在迁移里有播种，但 docs/permissions.catalog.json 没有说明（新增权限必须补说明）`)
  }
}
for (const code of catalogCodes) {
  if (!seedCodes.has(code)) {
    fail(`权限 ${code} 在 catalog 有说明，但没有任何迁移播种它（拼错了？或迁移没写）`)
  }
  if (![...pairs.values()].includes(code) && code !== fallback) {
    fail(`权限 ${code} 没有任何路径映射到它，也不在兜底分支：它在运行时永远不会被校验`)
  }
}
if (!fallback) {
  fail('PermissionService.requiredPermission() 里没有解析到兜底 return，权限过滤器会失去"未登记路径"的保护')
}

const docs = []
for (const item of catalog.permissions) {
  const seed = seeds.get(item.code)
  const endpoints = endpointList(item.code, { pairs })
  docs.push({
    code: item.code,
    label: seed ? seed.label : item.code,
    group: seed ? seed.group : '未分组',
    sortOrder: seed ? seed.sortOrder : 999,
    summary: item.summary,
    grants: item.grants ?? [],
    revokes: item.revokes ?? [],
    endpoints,
    fallback: item.code === fallback,
    note: item.note
  })
}
docs.sort((a, b) => a.sortOrder - b.sortOrder || a.code.localeCompare(b.code))

const quote = value => "'" + String(value).replaceAll('\\', '\\\\').replaceAll("'", "\\'") + "'"
const arrayLiteral = values => '[' + (values ?? []).map(quote).join(', ') + ']'

const ts = `/**
 * 由 scripts/generate-permission-help.mjs 生成，**勿手改**。
 *
 * 数据来源：说明来自 docs/permissions.catalog.json，接口映射来自
 * PermissionService.requiredPermission()。改完跑 \`npm run generate:permissions\`；
 * 产物与源不一致时 \`make contract-check\` 会失败。
 */
export interface PermissionDoc {
  code: string
  label: string
  group: string
  sortOrder: number
  summary: string
  grants: string[]
  revokes: string[]
  endpoints: string[]
  fallback: boolean
  note?: string
}

/** 关系总则：没有继承、没有父子层级；ADMIN 短路；菜单按角色而非权限。 */
export const PERMISSION_RELATIONS = {
  inheritance: ${quote(catalog.relations.inheritance)},
  adminBypass: ${quote(catalog.relations.adminBypass)},
  menu: ${quote(catalog.relations.menu)},
  fallback: ${quote(catalog.relations.fallback)}
} as const

export const PERMISSION_DOCS: Record<string, PermissionDoc> = {
${docs.map(doc => `  ${quote(doc.code)}: {
    code: ${quote(doc.code)},
    label: ${quote(doc.label)},
    group: ${quote(doc.group)},
    sortOrder: ${doc.sortOrder},
    summary: ${quote(doc.summary)},
    grants: ${arrayLiteral(doc.grants)},
    revokes: ${arrayLiteral(doc.revokes)},
    endpoints: ${arrayLiteral(doc.endpoints)},
    fallback: ${doc.fallback}${doc.note ? ',\n    note: ' + quote(doc.note) : ''}
  }`).join(',\n')}
}

export function permissionDoc(code: string): PermissionDoc | undefined {
  return PERMISSION_DOCS[code]
}
`

const md = `# 权限说明（自动生成）

> 本文件由 \`scripts/generate-permission-help.mjs\` 生成，**不要手改**。
> 改说明请改 [\`docs/permissions.catalog.json\`](permissions.catalog.json)，改接口映射请改
> \`PermissionService.requiredPermission()\`，然后执行 \`npm run generate:permissions\`。
> 漂移由 \`make contract-check\` 拦住。

## 关系总则

- **继承**：${catalog.relations.inheritance}
- **超管**：${catalog.relations.adminBypass}
- **菜单**：${catalog.relations.menu}
- **兜底**：${catalog.relations.fallback}

## 权限清单

| 权限码 | 名称 | 分组 | 拦截的接口 |
|---|---|---|---|
${docs.map(doc => `| \`${doc.code}\` | ${doc.label} | ${doc.group} | ${doc.endpoints.length ? doc.endpoints.map(e => '\`' + e + '\`').join('<br>') : (doc.fallback ? '兜底（未登记路径）' : '—')} |`).join('\n')}

${docs.map(doc => `### \`${doc.code}\` — ${doc.label}（${doc.group}）

${doc.summary}

- **勾选后可做**：
${doc.grants.map(item => '  - ' + item).join('\n') || '  - —'}
- **取消后**：
${doc.revokes.map(item => '  - ' + item).join('\n') || '  - —'}
- **关联接口**：${doc.endpoints.length ? doc.endpoints.map(e => '\`' + e + '\`').join('、') : '—'}${doc.fallback ? '（同时是所有未登记路径的兜底权限）' : ''}
- **关系**：无父级、无继承（各权限互相独立）；ADMIN 角色始终拥有本权限。${doc.note ? '\n- **注意**：' + doc.note : ''}`).join('\n\n')}
`

if (problems.length > 0) {
  console.error('权限说明生成失败：')
  for (const problem of problems) console.error('  - ' + problem)
  process.exit(1)
}

function writeOrCheck(path, content) {
  if (checkOnly) {
    const current = existsSync(path) ? readFileSync(path, 'utf8') : ''
    if (current !== content) {
      fail(`${path.replace(ROOT + '/', '')} 与源不一致：跑 npm run generate:permissions 重新生成`)
    }
    return
  }
  writeFileSync(path, content, 'utf8')
}

writeOrCheck(TS_OUT, ts)
writeOrCheck(MD_OUT, md)

if (problems.length > 0) {
  console.error('权限说明与源不一致：')
  for (const problem of problems) console.error('  - ' + problem)
  process.exit(1)
}

console.log((checkOnly ? '权限说明已是最新：' : '已生成权限说明：')
  + docs.length + ' 条，接口映射 ' + pairs.size + ' 条，兜底 ' + fallback)
