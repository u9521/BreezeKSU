#!/usr/bin/env node
/**
 * Gate: the fork ledger under `doc/` stays readable and connected.
 *
 * The preset gates cover the agent framework and the root entry file; they do
 * not read `doc/`, which belongs to the fork. That gap matters once the ledger
 * is split into one file per decision: a dangling link or an orphaned entry
 * costs a whole lookup cycle and nothing else would notice.
 *
 * Four checks:
 *
 *   1. every relative link under `doc/` resolves;
 *   2. every entry carries the required sections and front-matter fields;
 *   3. every entry is referenced from the index in `PRUNING.md`;
 *   4. no entry exceeds its line budget, because an entry that grows into a
 *      second copy of the function map is doing the wrong job.
 *
 * Content quality is deliberately not checked: a gate that guesses at meaning
 * trains people to write filler. The human review step in `breeze-sync-gates`
 * covers substance.
 */
import { readFileSync, existsSync, readdirSync, statSync } from 'node:fs'
import { dirname, join, posix, relative, resolve, sep } from 'node:path'
import { fileURLToPath } from 'node:url'

const ROOT = resolve(dirname(fileURLToPath(import.meta.url)), '..', '..')
const DOC_DIR = 'doc'
const ENTRY_DIR = 'doc/entries'
const INDEX = 'doc/PRUNING.md'
const TEMPLATE = 'doc/entries/TEMPLATE.md'
const ENTRY_LINE_BUDGET = 150

const REQUIRED_SECTIONS = [
  'Why',
  'Surface',
  'Residue',
  'Verified',
  'Sync risk',
  'Rollback',
]
const REQUIRED_FRONTMATTER = ['status', 'mechanism', 'residue-rev']
const STATUSES = ['prune', 'add', 'modify']

const abs = (relPath) => join(ROOT, relPath)
const read = (relPath) => readFileSync(abs(relPath), 'utf8')
const rel = (absPath) => relative(ROOT, absPath).split(sep).join('/')

/** Every markdown file under `doc/`, repository-relative. */
function walkMarkdown(dirRel) {
  const out = []
  const visit = (dir) => {
    let entries
    try {
      entries = readdirSync(dir, { withFileTypes: true })
    } catch {
      return
    }
    for (const entry of entries.sort((a, b) => a.name.localeCompare(b.name))) {
      const full = join(dir, entry.name)
      if (entry.isDirectory()) visit(full)
      else if (entry.name.endsWith('.md')) out.push(rel(full))
    }
  }
  visit(abs(dirRel))
  return out
}

const failures = []
const linkPattern = /!?\[([^\]]*)\]\(([^)\s]+)(?:\s+"[^"]*")?\)/g
const scheme = /^[a-z][a-z0-9+.-]*:/i

/* 1. Links resolve. */
let linkCount = 0
for (const relPath of walkMarkdown(DOC_DIR)) {
  const fileLines = read(relPath).split('\n')
  let inFence = false
  fileLines.forEach((line, index) => {
    if (/^\s*(```|~~~)/.test(line)) {
      inFence = !inFence
      return
    }
    if (inFence) return
    for (const match of line.matchAll(linkPattern)) {
      const rawTarget = match[2]
      if (scheme.test(rawTarget) || rawTarget.startsWith('#') || rawTarget.startsWith('/')) continue
      linkCount += 1
      const targetPath = rawTarget.split('#')[0]
      if (!targetPath) continue
      const target = posix.normalize(posix.join(dirname(relPath), targetPath))
      if (!existsSync(abs(target))) {
        failures.push(`${relPath}:${index + 1} — 链接目标不存在: ${rawTarget}`)
      }
    }
  })
}

/* 2–3. Entry shape and index closure. */
const indexText = existsSync(abs(INDEX)) ? read(INDEX) : ''
const entryFiles = walkMarkdown(ENTRY_DIR).filter(
  (p) => p !== TEMPLATE,
)

for (const relPath of entryFiles) {
  const text = read(relPath)
  const fileLines = text.split('\n')

  if (fileLines[0]?.trim() !== '---') {
    failures.push(`${relPath}:1 — 必须以 YAML front-matter 开头`)
  } else {
    const close = fileLines.findIndex((line, i) => i > 0 && line.trim() === '---')
    if (close === -1) {
      failures.push(`${relPath}: front-matter 未闭合`)
    } else {
      const front = fileLines.slice(1, close).join('\n')
      for (const field of REQUIRED_FRONTMATTER) {
        if (!new RegExp(`^${field}:\\s*\\S`, 'm').test(front)) {
          failures.push(`${relPath}: front-matter 缺少 "${field}"`)
        }
      }
      const status = /^status:\s*(\S+)/m.exec(front)?.[1]
      if (status && !STATUSES.includes(status)) {
        failures.push(
          `${relPath}: status "${status}" 非法，允许 ${STATUSES.join(' / ')}`,
        )
      }
    }
  }

  for (const section of REQUIRED_SECTIONS) {
    const escaped = section.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
    if (!new RegExp(`^##\\s+${escaped}\\s*$`, 'm').test(text)) {
      failures.push(`${relPath}: 缺少 "## ${section}"`)
    }
  }

  /* The residue table must be a table: it is what forces one verdict per surface. */
  const residue = text.split(/^##\s+Residue\s*$/m)[1]?.split(/^##\s+/m)[0] ?? ''
  if (!/^\s*\|/m.test(residue)) {
    failures.push(`${relPath}: "## Residue" 必须是表格，逐面给出结论`)
  }

  if (fileLines.length > ENTRY_LINE_BUDGET) {
    failures.push(
      `${relPath}: ${fileLines.length} 行超出 ${ENTRY_LINE_BUDGET} 行上限；entry 记这次做了什么，不是上游的功能说明`,
    )
  }

  const basename = relPath.split('/').at(-1)
  if (!indexText.includes(basename)) {
    failures.push(
      `${relPath}: 未被 ${INDEX} 的索引表引用（孤儿 entry）`,
    )
  }
}

/* 4. The template has to stay usable as a template. */
if (!existsSync(abs(TEMPLATE))) {
  failures.push(`${TEMPLATE}: 模板缺失；新增 entry 没有可照抄的骨架`)
}

finish(
  'verify-doc-entries',
  failures,
  `${linkCount} 条链接可解析，${entryFiles.length} 个 entry 结构合规且被索引引用`,
)

function finish(gate, problems, summary) {
  if (problems.length === 0) {
    console.log(`✓ ${gate}: ${summary}`)
    return
  }
  console.error(`✗ ${gate}: ${problems.length} 处违规`)
  for (const problem of problems) console.error(`  - ${problem}`)
  process.exitCode = 1
}
