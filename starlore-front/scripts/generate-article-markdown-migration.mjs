import fs from 'node:fs'
import TurndownService from 'turndown'

const [inputPath, outputPath] = process.argv.slice(2)
if (!inputPath || !outputPath) {
  throw new Error('Usage: node generate-article-markdown-migration.mjs <dump.sql> <output.sql>')
}

function readSqlString(source, start) {
  if (source[start] !== "'") throw new Error(`Expected SQL string at ${start}`)
  let value = ''
  let index = start + 1
  while (index < source.length) {
    const char = source[index++]
    if (char === "'") return { value, end: index }
    if (char !== '\\') {
      value += char
      continue
    }
    const escaped = source[index++]
    const escapes = { n: '\n', r: '\r', t: '\t', 0: '\0', b: '\b', Z: '\x1a' }
    value += escapes[escaped] ?? escaped
  }
  throw new Error('Unterminated SQL string')
}

const turndown = new TurndownService({
  headingStyle: 'atx',
  bulletListMarker: '-',
  codeBlockStyle: 'fenced',
  fence: '```',
  emDelimiter: '*',
  strongDelimiter: '**',
})

turndown.addRule('fencedCodeBlockWithLanguage', {
  filter: node => node.nodeName === 'PRE' && node.firstElementChild?.nodeName === 'CODE',
  replacement(_content, node) {
    const code = node.firstElementChild
    const language = code.className.match(/(?:language-|lang-)([\w+-]+)/)?.[1] ?? ''
    const text = code.textContent.replace(/\n$/, '')
    const longestFence = Math.max(3, ...Array.from(text.matchAll(/`+/g), match => match[0].length + 1))
    const fence = '`'.repeat(longestFence)
    return `\n\n${fence}${language}\n${text}\n${fence}\n\n`
  },
})

const dump = fs.readFileSync(inputPath, 'utf8')
const records = []
for (const line of dump.split(/\r?\n/)) {
  const idMatch = line.match(/^INSERT INTO `articles` VALUES \((\d+),\s*/)
  if (!idMatch) continue

  const id = Number(idMatch[1])
  let cursor = idMatch[0].length
  const title = readSqlString(line, cursor)
  cursor = title.end
  cursor = line.indexOf("'", cursor + 1)
  const content = readSqlString(line, cursor)
  if (!/^\s*<(?:h[1-6]|p|div|blockquote|pre|ul|ol|table)\b/i.test(content.value)) continue

  const markdown = turndown.turndown(content.value)
    .replace(/\u00a0/g, ' ')
    .replace(/[ \t]+\n/g, '\n')
    .replace(/\n{3,}/g, '\n\n')
    .trim()

  records.push({ id, title: title.value, markdown })
}

const stamp = new Date().toISOString().replace(/[-:TZ.]/g, '').slice(0, 14)
const backupTable = `articles_content_backup_${stamp}`
const ids = records.map(record => record.id).join(', ')
const sqlString = value => `'${value
  .replace(/\\/g, '\\\\')
  .replace(/\0/g, '\\0')
  .replace(/\x1a/g, '\\Z')
  .replace(/'/g, "''")}'`
const sql = [
  '-- 旧 HTML 文章正文迁移为 Markdown',
  '-- 由数据库导出文件离线生成；仅更新检测到 HTML 的记录。',
  'SET NAMES utf8mb4;',
  'SET FOREIGN_KEY_CHECKS = 0;',
  '',
  `CREATE TABLE \`${backupTable}\` AS`,
  `SELECT id, content, updatedAt FROM articles WHERE id IN (${ids});`,
  '',
  'START TRANSACTION;',
  ...records.flatMap(record => [
    `-- ${record.id}: ${record.title}`,
    `UPDATE articles SET content = ${sqlString(record.markdown)} WHERE id = ${record.id};`,
    '',
  ]),
  'COMMIT;',
  '',
  `SELECT id, title, LEFT(content, 120) AS content_preview FROM articles WHERE id IN (${ids}) ORDER BY id;`,
  'SET FOREIGN_KEY_CHECKS = 1;',
  '',
  `-- 如需回滚：`,
  `-- UPDATE articles a JOIN \`${backupTable}\` b ON b.id = a.id SET a.content = b.content, a.updatedAt = b.updatedAt;`,
  '',
].join('\n')

fs.writeFileSync(outputPath, sql, 'utf8')
console.log(JSON.stringify({ outputPath, backupTable, records: records.map(({ id, title, markdown }) => ({ id, title, markdownLength: markdown.length })) }, null, 2))
