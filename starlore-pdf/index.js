const express = require('express')
const puppeteer = require('puppeteer')
const Mustache = require('mustache')
const fs = require('fs')
const path = require('path')

const app = express()
app.use(express.json({ limit: '5mb' }))

const PORT = process.env.PORT || 3001
const TEMPLATES_DIR = path.join(__dirname, 'templates')

// Cache template HTML
const templateCache = {}
function getTemplate(name) {
  if (!templateCache[name]) {
    const filePath = path.join(TEMPLATES_DIR, `${name}.html`)
    if (!fs.existsSync(filePath)) return null
    templateCache[name] = fs.readFileSync(filePath, 'utf-8')
  }
  return templateCache[name]
}

// Detect browser executable path (cross-platform)
function getBrowserPath() {
  const winPaths = [
    'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe',
    'C:\\Program Files\\Microsoft\\Edge\\Application\\msedge.exe',
  ]
  // Only use system browser on Windows, use Puppeteer's bundled Chromium on Linux
  if (process.platform === 'win32') {
    return winPaths.find(p => fs.existsSync(p))
  }
  return undefined
}

// Reuse browser instance
let browserPromise = null
function getBrowser() {
  if (!browserPromise) {
    const launchOptions = {
      headless: true,
      args: ['--no-sandbox', '--disable-setuid-sandbox', '--disable-dev-shm-usage', '--disable-gpu'],
    }
    // Use system browser if available (Edge on Windows)
    const browserPath = getBrowserPath()
    if (browserPath) {
      launchOptions.executablePath = browserPath
      console.log('Using browser:', browserPath)
    }
    browserPromise = puppeteer.launch(launchOptions).then(browser => {
      browser.on('disconnected', () => { browserPromise = null })
      return browser
    })
  }
  return browserPromise
}

// Health check
app.get('/health', (_req, res) => {
  res.json({ status: 'ok' })
})

// Generate PDF
app.post('/api/pdf/resume', async (req, res) => {
  const startTime = Date.now()
  try {
    const { template = 'classic', data } = req.body
    if (!data) {
      return res.status(400).json({ error: 'Missing data field' })
    }

    const tmpl = getTemplate(template)
    if (!tmpl) {
      return res.status(400).json({ error: `Template "${template}" not found` })
    }

    // Prepare template data
    const spacing = data.content?.spacing || {}
    console.log(`[PDF] spacing values: moduleGap=${spacing.moduleGap}, lineHeight=${spacing.lineHeight}, fontSize=${spacing.fontSize}`)
    const tplData = {
      title: data.title || '简历',
      name: data.name || '',
      jobTitle: data.jobTitle || '',
      phone: data.phone || '',
      email: data.email || '',
      photoUrl: data.photoUrl || '',
      moduleGap: spacing.moduleGap || 25,
      lineHeight: spacing.lineHeight || 6,
      fontSize: spacing.fontSize || 14,
      education: (data.content?.education || []).map(item => ({
        ...item,
        hasDetail: !!item.detail,
      })),
      experience: (data.content?.experience || []).map(item => ({
        ...item,
        hasDetail: !!item.detail,
      })),
      projects: (data.content?.projects || []).map(item => ({
        ...item,
        hasDetail: !!item.detail,
      })),
      skills: data.content?.skills || '',
    }

    const html = Mustache.render(tmpl, tplData)

    const browser = await getBrowser()
    const page = await browser.newPage()

    try {
      // Set viewport to match preview exactly (A4 at 96 DPI)
      await page.setViewport({ width: 794, height: 1123 })
      await page.setContent(html, { waitUntil: 'networkidle0', timeout: 15000 })
      await page.emulateMediaType('print')

      // Debug: measure actual rendered dimensions
      const dims = await page.evaluate(() => {
        const el = document.querySelector('.resume-page')
        if (!el) return null
        const rect = el.getBoundingClientRect()
        const style = getComputedStyle(el)
        return {
          width: rect.width,
          height: rect.height,
          scrollHeight: el.scrollHeight,
          padding: style.padding,
          fontSize: style.fontSize,
          lineHeight: style.lineHeight,
          devicePixelRatio: window.devicePixelRatio,
        }
      })
      console.log('[PDF] actual rendered dimensions:', JSON.stringify(dims, null, 2))

      const pdfBuffer = await page.pdf({
        width: '794px',
        height: '1123px',
        printBackground: true,
        margin: { top: 0, right: 0, bottom: 0, left: 0 },
      })

      const fileName = `${data.name || '简历'}.pdf`
      // Use writeHead to avoid Express auto-adding charset=utf-8 to binary Content-Type
      res.writeHead(200, {
        'Content-Type': 'application/pdf',
        'Content-Disposition': `attachment; filename*=UTF-8''${encodeURIComponent(fileName)}`,
        'Content-Length': pdfBuffer.length,
      })
      res.end(pdfBuffer)

      console.log(`PDF generated: ${fileName}, ${(pdfBuffer.length / 1024).toFixed(1)}KB, ${Date.now() - startTime}ms`)
    } finally {
      await page.close()
    }
  } catch (err) {
    console.error('PDF generation failed:', err)
    res.status(500).json({ error: err.message || 'PDF generation failed' })
  }
})

// Graceful shutdown
process.on('SIGINT', async () => {
  if (browserPromise) {
    const browser = await browserPromise
    await browser.close()
  }
  process.exit(0)
})

app.listen(PORT, () => {
  console.log(`PDF service running on http://localhost:${PORT}`)
})
