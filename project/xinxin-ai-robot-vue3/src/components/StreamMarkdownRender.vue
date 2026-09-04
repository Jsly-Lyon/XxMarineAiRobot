<template>
  <div class="markdown-container" :class="customCss" @click="handleMarkdownClick">
    <div v-html="renderedContent"></div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js/lib/core'
import bash from 'highlight.js/lib/languages/bash'
import css from 'highlight.js/lib/languages/css'
import java from 'highlight.js/lib/languages/java'
import javascript from 'highlight.js/lib/languages/javascript'
import json from 'highlight.js/lib/languages/json'
import markdown from 'highlight.js/lib/languages/markdown'
import python from 'highlight.js/lib/languages/python'
import sql from 'highlight.js/lib/languages/sql'
import xml from 'highlight.js/lib/languages/xml'
import 'highlight.js/styles/github.css'

const logger = {
  debug(...args) {
    if (import.meta.env.DEV) {
      console.debug('[StreamMarkdownRender]', ...args)
    }
  },
  warn(...args) {
    if (import.meta.env.DEV) {
      console.warn('[StreamMarkdownRender]', ...args)
    }
  }
}

const props = defineProps({
  content: {
    type: String,
    default: ''
  },
  customCss: {
    type: String,
    default: ''
  }
})

const renderedContent = ref('')
let codeBlockIndex = 0
const codeBlockMap = new Map()
const languageDisplayNameMap = {
  bash: 'Bash',
  shell: 'Shell',
  css: 'CSS',
  html: 'HTML',
  java: 'Java',
  js: 'JavaScript',
  javascript: 'JavaScript',
  json: 'JSON',
  md: 'Markdown',
  markdown: 'Markdown',
  py: 'Python',
  python: 'Python',
  sql: 'SQL',
  xml: 'XML'
}
const languageFileExtensionMap = {
  bash: 'sh',
  shell: 'sh',
  css: 'css',
  html: 'html',
  java: 'java',
  javascript: 'js',
  json: 'json',
  markdown: 'md',
  python: 'py',
  sql: 'sql',
  xml: 'xml'
}

hljs.registerLanguage('bash', bash)
hljs.registerLanguage('shell', bash)
hljs.registerLanguage('css', css)
hljs.registerLanguage('java', java)
hljs.registerLanguage('js', javascript)
hljs.registerLanguage('javascript', javascript)
hljs.registerLanguage('json', json)
hljs.registerLanguage('markdown', markdown)
hljs.registerLanguage('md', markdown)
hljs.registerLanguage('python', python)
hljs.registerLanguage('py', python)
hljs.registerLanguage('sql', sql)
hljs.registerLanguage('html', xml)
hljs.registerLanguage('xml', xml)

const normalizeLanguageName = (language = '') => {
  const langName = language.trim().split(/\s+/)[0].toLowerCase()
  return languageDisplayNameMap[langName] ?? langName
}

const renderIcon = (name) => `<svg class="code-block__action-icon" aria-hidden="true"><use href="#icon-${name}"></use></svg>`

const renderActionButtonContent = (iconName, text) => `${renderIcon(iconName)}<span>${text}</span>`

const renderToolButton = (className, codeId, iconName, text) => (
  `<button class="${className}" type="button" data-code-id="${codeId}">${renderActionButtonContent(iconName, text)}</button>`
)

const md = new MarkdownIt({
  html: false,
  xhtmlOut: true,
  linkify: true,
  typographer: true,
  breaks: true,
  langPrefix: 'hljs language-',
  highlight(code, language) {
    if (language && hljs.getLanguage(language)) {
      try {
        return hljs.highlight(code, { language }).value
      } catch (error) {
        logger.warn(`代码高亮失败，语言：${language}`, error)
      }
    }

    if (language) {
      logger.debug(`未注册的代码语言：${language}`)
    }

    return md.utils.escapeHtml(code)
  }
})

md.renderer.rules.fence = (tokens, idx, options) => {
  const token = tokens[idx]
  const code = token.content
  const codeId = `code-${codeBlockIndex++}`
  const langName = normalizeLanguageName(token.info)
  const languageClass = langName ? ` language-${md.utils.escapeHtml(langName.toLowerCase())}` : ''
  const highlightedCode = options.highlight(code, langName.toLowerCase(), '') || md.utils.escapeHtml(code)
  const displayName = langName || 'Plain text'
  codeBlockMap.set(codeId, { code, language: langName })

  return [
    '<div class="code-block">',
    '<div class="code-block__header">',
    `<span>${md.utils.escapeHtml(displayName)}</span>`,
    '<div class="code-block__actions">',
    renderToolButton('code-block__action code-block__download', codeId, 'download', '下载'),
    renderToolButton('code-block__action code-block__copy', codeId, 'copy', '复制'),
    '</div>',
    '</div>',
    `<pre><code class="hljs${languageClass}">${highlightedCode}</code></pre>`,
    '</div>'
  ].join('')
}

const getDownloadFilename = (language) => {
  const langKey = language?.toLowerCase?.() ?? ''
  const extension = languageFileExtensionMap[langKey] ?? 'txt'
  return `code-${Date.now()}.${extension}`
}

const copyText = async (text) => {
  if (navigator.clipboard?.writeText) {
    await navigator.clipboard.writeText(text)
    return
  }

  const textarea = document.createElement('textarea')
  textarea.value = text
  textarea.setAttribute('readonly', '')
  textarea.style.position = 'fixed'
  textarea.style.left = '-9999px'
  document.body.appendChild(textarea)
  textarea.select()
  document.execCommand('copy')
  document.body.removeChild(textarea)
}

const downloadCode = ({ code, language }) => {
  const blob = new Blob([code], { type: 'text/plain;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')

  link.href = url
  link.download = getDownloadFilename(language)
  link.style.display = 'none'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
  logger.debug('Code downloaded', {
    codeLength: code.length,
    language
  })
}

const showCopySuccess = () => {
  message.success('复制成功')
}

const updateCopyButtonStatus = (copyButton) => {
  copyButton.classList.add('code-block__copy--success')
  copyButton.innerHTML = renderActionButtonContent('copy-success', '已复制')

  window.setTimeout(() => {
    copyButton.classList.remove('code-block__copy--success')
    copyButton.innerHTML = renderActionButtonContent('copy', '复制')
  }, 1400)
}

const handleMarkdownClick = async (event) => {
  const copyButton = event.target.closest?.('.code-block__copy')
  const downloadButton = event.target.closest?.('.code-block__download')
  const actionButton = copyButton || downloadButton
  if (!actionButton) return

  const codeBlock = codeBlockMap.get(actionButton.dataset.codeId)
  if (!codeBlock) return

  if (downloadButton) {
    downloadCode(codeBlock)
    return
  }

  try {
    await copyText(codeBlock.code)
    showCopySuccess()
    updateCopyButtonStatus(copyButton)
    logger.debug('Code copied', { codeLength: codeBlock.code.length })
  } catch (error) {
    logger.warn('代码复制失败', error)
  }
}

watch(
  () => props.content,
  (newVal) => {
    try {
      codeBlockIndex = 0
      codeBlockMap.clear()
      renderedContent.value = newVal ? md.render(newVal) : ''
      logger.debug('Markdown rendered', {
        sourceLength: newVal?.length ?? 0,
        htmlLength: renderedContent.value.length
      })
    } catch (error) {
      logger.warn('Markdown render failed', error)
      renderedContent.value = props.content
    }
  },
  { immediate: true }
)
</script>

<style scoped>
.markdown-container {
  width: 100%;
  color: #111827;
  font-family: Inter, ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", "Noto Sans SC", "Microsoft YaHei", sans-serif;
  font-size: 15px;
  line-height: 1.75;
}

:deep(*) {
  box-sizing: border-box;
}

:deep(:first-child) {
  margin-top: 0;
}

:deep(:last-child) {
  margin-bottom: 0;
}

:deep(p) {
  margin: 0.5rem 0;
}

:deep(h1),
:deep(h2),
:deep(h3),
:deep(h4),
:deep(h5),
:deep(h6) {
  color: #111827;
  font-weight: 650;
  letter-spacing: 0;
  line-height: 1.32;
  margin: 1.05rem 0 0.45rem;
}

:deep(h1) {
  font-size: 1.2rem;
}

:deep(h2) {
  font-size: 1.08rem;
}

:deep(h3) {
  font-size: 0.98rem;
}

:deep(h4),
:deep(h5),
:deep(h6) {
  font-size: 0.92rem;
}

:deep(ul),
:deep(ol) {
  margin: 0.35rem 0 0.65rem;
  padding-left: 1.25rem;
}

:deep(ul) {
  list-style: disc;
}

:deep(ol) {
  list-style: decimal;
}

:deep(li) {
  margin: 0.18rem 0;
  padding-left: 0.1rem;
}

:deep(li::marker) {
  color: #8b949e;
}

:deep(li > p) {
  margin: 0.25rem 0;
}

:deep(strong) {
  color: #111827;
  font-weight: 650;
}

:deep(a) {
  color: #315efb;
  text-decoration: none;
  text-underline-offset: 3px;
}

:deep(a:hover) {
  text-decoration: underline;
}

:deep(blockquote) {
  margin: 0.75rem 0;
  padding: 0.1rem 0 0.1rem 0.8rem;
  color: #57606a;
  border-left: 3px solid #d0d7de;
}

:deep(:not(pre) > code) {
  color: #1f2328;
  background-color: #f6f8fa;
  border: 1px solid #d8dee4;
  border-radius: 5px;
  font-family: "SF Mono", "Cascadia Code", "Fira Code", Consolas, monospace;
  font-size: 0.82em;
  padding: 0.08rem 0.28rem;
}

:deep(.code-block) {
  max-width: 100%;
  margin: 1.15rem 0;
  overflow: hidden;
  background: #f7f8fa;
  border: 1px solid #edf0f3;
  border-radius: 10px;
}

:deep(.code-block__header) {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  min-height: 44px;
  padding: 0 1rem 0 1.05rem;
  color: #6b7280;
  font-size: 12px;
  font-weight: 500;
  line-height: 1;
  background: #f7f8fa;
  border-bottom: 0;
}

:deep(.code-block__actions) {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
}

:deep(.code-block__action) {
  appearance: none;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  flex-shrink: 0;
  height: 24px;
  padding: 0 0.45rem;
  color: #9ca3af;
  font: inherit;
  font-size: 12px;
  line-height: 22px;
  background: transparent;
  border: 1px solid transparent;
  border-radius: 5px;
  cursor: pointer;
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease;
}

:deep(.code-block__action-icon) {
  width: 14px;
  height: 14px;
  color: currentColor;
}

:deep(.code-block__action:hover) {
  color: #4b5563;
  background: transparent;
  border-color: transparent;
}

:deep(.code-block__copy--success) {
  color: #1a7f37;
}

:deep(pre) {
  max-width: 100%;
  margin: 0;
  padding: 0.7rem 1.05rem 1.35rem;
  overflow-x: auto;
  color: #0f172a;
  background: transparent;
  border: 0;
  border-radius: 0;
  line-height: 1.68;
  white-space: pre;
}

:deep(pre code) {
  display: block;
  width: max-content;
  min-width: 100%;
  padding: 0;
  background: transparent;
  border: 0;
  border-radius: 0;
  font-family: "SF Mono", "Cascadia Mono", "Cascadia Code", "Fira Code", Consolas, monospace;
  font-size: 12.5px;
}

:deep(.hljs) {
  color: #0f172a;
  background: transparent;
}

:deep(pre::-webkit-scrollbar) {
  height: 7px;
}

:deep(pre::-webkit-scrollbar-track) {
  background: transparent;
}

:deep(pre::-webkit-scrollbar-thumb) {
  background-color: #c9d1d9;
  border-radius: 999px;
}

:deep(table) {
  display: block;
  width: 100%;
  margin: 0.75rem 0;
  overflow-x: auto;
  border-collapse: collapse;
  font-size: 0.92em;
}

:deep(th),
:deep(td) {
  padding: 0.45rem 0.6rem;
  border: 1px solid #d8dee4;
  text-align: left;
  vertical-align: top;
}

:deep(th) {
  color: #111827;
  font-weight: 650;
  background-color: #f8fafc;
}

:deep(hr) {
  height: 1px;
  margin: 1rem 0;
  background-color: #d8dee4;
  border: 0;
}
</style>
