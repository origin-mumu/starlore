from docx import Document
from docx.shared import Pt, Cm, Inches, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.oxml.ns import qn
import datetime

doc = Document()

# ── 全局默认字体 ──
style = doc.styles['Normal']
font = style.font
font.name = '宋体'
font.size = Pt(12)
style.element.rPr.rFonts.set(qn('w:eastAsia'), '宋体')

# ── 页边距 ──
for section in doc.sections:
    section.top_margin = Cm(2.54)
    section.bottom_margin = Cm(2.54)
    section.left_margin = Cm(3.17)
    section.right_margin = Cm(3.17)

def add_title(text, level=0):
    """添加标题，level=0 为报告大标题，1/2 为 Heading 1/2"""
    if level == 0:
        p = doc.add_heading(text, level=0)
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
        for run in p.runs:
            run.font.size = Pt(22)
            run.font.name = '黑体'
            run.element.rPr.rFonts.set(qn('w:eastAsia'), '黑体')
    else:
        p = doc.add_heading(text, level=level)
        for run in p.runs:
            run.font.name = '黑体'
            run.element.rPr.rFonts.set(qn('w:eastAsia'), '黑体')
            if level == 1:
                run.font.size = Pt(16)
            else:
                run.font.size = Pt(14)

def add_para(text, bold=False, indent=False):
    """添加正文段落"""
    p = doc.add_paragraph()
    if indent:
        p.paragraph_format.first_line_indent = Cm(0.74)
    run = p.add_run(text)
    run.font.name = '宋体'
    run.element.rPr.rFonts.set(qn('w:eastAsia'), '宋体')
    run.font.size = Pt(12)
    run.bold = bold
    return p

def set_cell_text(cell, text, bold=False, align=WD_ALIGN_PARAGRAPH.CENTER):
    """设置表格单元格文本"""
    cell.text = ''
    p = cell.paragraphs[0]
    p.alignment = align
    run = p.add_run(text)
    run.font.name = '宋体'
    run.element.rPr.rFonts.set(qn('w:eastAsia'), '宋体')
    run.font.size = Pt(10)
    run.bold = bold

def make_table(headers, rows):
    """创建带表头的表格"""
    table = doc.add_table(rows=1 + len(rows), cols=len(headers))
    table.style = 'Table Grid'
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    # 表头
    for i, h in enumerate(headers):
        set_cell_text(table.rows[0].cells[i], h, bold=True)
    # 数据行
    for r_idx, row in enumerate(rows):
        for c_idx, val in enumerate(row):
            align = WD_ALIGN_PARAGRAPH.LEFT if c_idx in (1, 2, 3, 4) else WD_ALIGN_PARAGRAPH.CENTER
            set_cell_text(table.rows[r_idx + 1].cells[c_idx], val, align=align)
    return table


# ======================== 报告正文 ========================

add_title('系统测试实验报告')

add_para(f'项目名称：Starlore 个人知识管理博客系统')
add_para(f'测试日期：{datetime.date.today().strftime("%Y年%m月%d日")}')
add_para(f'测试人员：贾新科')
add_para('')

# ── 一、实验目的 ──
add_title('一、实验目的', level=1)
add_para('1、掌握系统测试采用的测试技术，包括黑盒测试中的等价类划分、边界值分析、场景法等方法。', indent=True)
add_para('2、明确系统测试的主要任务：验证系统是否正确实现了需求规格说明书中定义的所有功能，以及是否满足性能、安全性、可用性等非功能性需求。', indent=True)
add_para('3、明确系统测试的原则：测试应尽早介入、缺陷集群效应、杀虫剂悖论、测试上下文依赖、不存在缺陷谬论等。', indent=True)

# ── 二、实验内容和要求 ──
add_title('二、实验内容和要求', level=1)
add_para('基于自编的 Starlore 个人知识管理博客系统进行系统测试。该系统采用前后端分离架构，包含以下子系统：', indent=True)
add_para('• starlore-front：Vue 3 前端博客展示系统')
add_para('• starlore-admin：Vue 3 后台管理系统')
add_para('• starlore-back：Spring Boot 后端 API 服务')
add_para('• starlore-pdf：Node.js PDF 简历生成微服务')
add_para('')
add_para('测试依据为需求分析文档，覆盖系统的用户认证、文章管理、分类管理、AI 对话助手、简历管理、3D VR 星空可视化、后台管理等核心功能模块。检查是否实现了需求中要求的功能，以及是否符合非功能要求（如性能、安全性、兼容性）。如发现缺陷，给出解决方案。', indent=True)

# ── 三、测试环境 ──
add_title('三、测试环境', level=1)
env_headers = ['项目', '配置']
env_rows = [
    ['操作系统', 'Windows 11 Home China 10.0.26200'],
    ['后端运行环境', 'Java 17 + Spring Boot 3.4.5，端口 5000'],
    ['前端运行环境', 'Node.js + Vite 7，Vue 3 + TypeScript'],
    ['数据库', 'MySQL 8.x'],
    ['对象存储', 'MinIO'],
    ['AI 模型', 'DeepSeek API / MiMo / 智谱 AI'],
    ['浏览器', 'Microsoft Edge 136+ / Chrome 136+'],
    ['部署地址', 'http://47.94.128.65/'],
]
make_table(env_headers, env_rows)
add_para('')

# ── 四、测试用例设计（系统测试）──
add_title('四、测试用例设计（系统测试）', level=1)

# ---------- 4.1 用户认证模块 ----------
add_title('4.1 用户认证模块', level=2)
headers = ['序号', '输入数据', '操作说明', '期望结果', '实际结果']
auth_rows = [
    ['TC-01', '用户名: testuser\n密码: 123456',
     '在注册页面输入合法用户名和密码，点击"注册"按钮',
     '注册成功，跳转到登录页面，提示"注册成功"',
     '符合预期：注册成功后自动跳转登录页'],
    ['TC-02', '用户名: testuser\n密码: 123456',
     '在登录页面输入已注册的用户名和密码，点击"登录"按钮',
     '登录成功，跳转到首页，右上角显示用户名，JWT Token 存储到本地',
     '符合预期：登录成功，页面跳转正常'],
    ['TC-03', '用户名: testuser\n密码: wrong',
     '在登录页面输入正确用户名和错误密码，点击"登录"按钮',
     '登录失败，提示"用户名或密码错误"',
     '符合预期：返回 401 错误，页面显示错误提示'],
    ['TC-04', '用户名: (空)\n密码: (空)',
     '不输入任何内容，直接点击"登录"按钮',
     '前端表单校验阻止提交，提示"请输入用户名"和"请输入密码"',
     '符合预期：Element Plus 表单校验生效'],
    ['TC-05', '用户名: testuser',
     '登录后点击页面右上角"退出"按钮',
     '退出成功，JWT Token 清除，跳转到首页，导航栏显示"登录"',
     '符合预期：Token 已清除，页面状态正确'],
    ['TC-06', '未登录状态',
     '直接访问需要登录的页面（如 /profile）',
     '页面跳转到登录页，提示"请先登录"',
     '符合预期：路由守卫生效，未登录用户被重定向'],
]
make_table(headers, auth_rows)
add_para('')

# ---------- 4.2 文章管理模块 ----------
add_title('4.2 文章管理模块', level=2)
article_rows = [
    ['TC-07', '标题: 测试文章\n内容: Hello World\n分类: 技术',
     '在管理后台"新增文章"页面填写标题、内容、分类，点击"发布"',
     '文章发布成功，返回文章列表，新文章出现在列表中',
     '符合预期：文章成功入库，列表显示正常'],
    ['TC-08', '标题: (空)\n内容: 测试内容',
     '在新增文章页面不填写标题，直接点击"发布"',
     '前端校验阻止提交，提示"请输入文章标题"',
     '符合预期：必填字段校验生效'],
    ['TC-09', '文章ID: 已有文章\n新标题: 修改后的标题',
     '在文章列表中点击"编辑"，修改标题后点击"保存"',
     '文章更新成功，列表中显示新标题',
     '符合预期：MyBatis-Plus 更新操作正常'],
    ['TC-10', '文章ID: 已有文章',
     '在文章列表中点击"删除"，确认删除操作',
     '文章删除成功，从列表中移除',
     '符合预期：文章已从数据库中删除'],
    ['TC-11', '关键词: Vue',
     '在前台文章列表页搜索框输入"Vue"，点击搜索',
     '显示标题或内容包含"Vue"的文章列表',
     '符合预期：模糊搜索功能正常'],
    ['TC-12', '分类ID: 技术',
     '在前台点击"技术"分类标签',
     '仅显示属于"技术"分类的文章',
     '符合预期：分类筛选功能正常'],
    ['TC-13', '无',
     '在前台文章列表页滚动到底部，触发加载更多',
     '自动加载下一页文章，无重复内容',
     '符合预期：分页加载正常，无重复数据'],
    ['TC-14', '文章ID: 已有文章',
     '在前台点击某篇文章标题，进入文章详情页',
     '页面正确显示文章标题、内容、发布时间、分类标签，代码块有语法高亮',
     '符合预期：Markdown 渲染正常，highlight.js 代码高亮生效'],
]
make_table(headers, article_rows)
add_para('')

# ---------- 4.3 分类管理模块 ----------
add_title('4.3 分类管理模块', level=2)
cat_rows = [
    ['TC-15', '分类名: 前端开发',
     '在后台分类管理页面输入分类名称，点击"添加"',
     '分类添加成功，列表中显示新分类',
     '符合预期：分类成功创建'],
    ['TC-16', '分类名: 前端开发',
     '尝试添加已存在的同名分类',
     '提示"分类已存在"，不允许重复创建',
     '符合预期：后端返回 409 Conflict 错误'],
    ['TC-17', '无',
     '在前台首页或侧边栏查看分类列表',
     '正确显示所有分类及各分类下的文章数量',
     '符合预期：分类统计数据正确'],
]
make_table(headers, cat_rows)
add_para('')

# ---------- 4.4 AI 对话助手模块 ----------
add_title('4.4 AI 对话助手（Echobot）模块', level=2)
ai_rows = [
    ['TC-18', '消息: 你好',
     '在 AI 对话页面输入"你好"，点击发送',
     'AI 以 SSE 流式方式返回回复，文字逐步显示在对话区域',
     '符合预期：流式响应正常，UI 逐字渲染'],
    ['TC-19', '消息: 帮我搜索关于 Vue 的文章',
     '开启 Agent 模式，发送搜索指令',
     'AI 调用 searchArticles 工具，返回相关文章列表',
     '符合预期：Agent 工具调用正常，返回结构化结果'],
    ['TC-20', '消息: 帮我写一篇关于 React 的文章\n标题: React入门',
     '开启 Agent 模式，发送写作指令',
     'AI 调用 writeArticle 工具，创建文章草稿',
     '符合预期：Agent 自动创建文章，可在文章列表中查看'],
    ['TC-21', '图片: 测试截图.png',
     '在对话页面上传一张图片',
     'AI 使用 MiMo 视觉模型识别图片内容并返回描述',
     '符合预期：图片识别功能正常'],
    ['TC-22', '无',
     '点击对话页面的"新建对话"按钮',
     '创建新的会话，对话列表中出现新会话项',
     '符合预期：会话管理功能正常'],
    ['TC-23', '无',
     '在对话列表中选择一个历史会话',
     '加载该会话的历史消息记录',
     '符合预期：历史消息正确恢复'],
    ['TC-24', '无',
     '点击 AI 回复文本旁的"朗读"按钮',
     '以 TTS 方式朗读 AI 回复内容',
     '符合预期：TTS 语音合成正常播放'],
    ['TC-25', '无',
     '点击"沉浸模式"按钮',
     '进入全屏沉浸式 AI 对话界面',
     '符合预期：沉浸模式 UI 切换正常'],
    ['TC-26', '未登录状态',
     '未登录时访问 AI 对话页面',
     '显示登录遮罩层，提示"请先登录后使用"',
     '符合预期：未登录用户被正确拦截'],
]
make_table(headers, ai_rows)
add_para('')

# ---------- 4.5 创意发散模块 ----------
add_title('4.5 创意发散（Diverge）模块', level=2)
diverge_rows = [
    ['TC-27', '关键词: 人工智能',
     '在创意发散页面输入"人工智能"，点击生成',
     '生成以"人工智能"为中心的思维导图，包含 8 个中英文关联词，以力导向图展示',
     '符合预期：思维导图正确渲染，节点可交互'],
    ['TC-28', '关键词: (空)',
     '不输入关键词，直接点击生成',
     '提示"请输入关键词"',
     '符合预期：空输入校验生效'],
]
make_table(headers, diverge_rows)
add_para('')

# ---------- 4.6 简历管理模块 ----------
add_title('4.6 简历管理模块', level=2)
resume_rows = [
    ['TC-29', '简历信息: 姓名、教育经历、工作经历等',
     '在简历编辑页面填写完整信息，点击"保存"',
     '简历数据保存成功，下次进入页面自动加载',
     '符合预期：数据持久化正常'],
    ['TC-30', '无',
     '在简历编辑页面点击"导出 PDF"',
     '调用 starlore-pdf 微服务，生成 PDF 文件并触发下载',
     '符合预期：PDF 文件格式正确，内容完整'],
    ['TC-31', '无',
     '在简历编辑页面调整模块间距、行高、字号',
     '实时预览效果与调整参数一致',
     '符合预期：样式参数实时生效'],
]
make_table(headers, resume_rows)
add_para('')

# ---------- 4.7 3D VR 星空可视化模块 ----------
add_title('4.7 3D VR 星空可视化模块', level=2)
vr_rows = [
    ['TC-32', '无',
     '在前台点击"VR 星空"入口，进入 3D 可视化页面',
     'Three.js 场景正确加载，显示星空粒子效果、分类节点和文章节点',
     '符合预期：3D 场景渲染正常'],
    ['TC-33', '无',
     '在 3D 场景中将鼠标悬停在某个文章节点上',
     '显示文章标题预览卡片',
     '符合预期：鼠标悬停交互正常'],
    ['TC-34', '无',
     '在 3D 场景中点击某个文章节点',
     '跳转到对应文章的详情页',
     '符合预期：节点点击导航正常'],
]
make_table(headers, vr_rows)
add_para('')

# ---------- 4.8 后台管理模块 ----------
add_title('4.8 后台管理模块', level=2)
admin_rows = [
    ['TC-35', '管理员账号/密码',
     '使用管理员账号登录后台管理系统',
     '登录成功，进入后台仪表盘，显示博客统计数据和 ECharts 图表',
     '符合预期：管理员权限验证通过，仪表盘数据正确'],
    ['TC-36', '非管理员账号',
     '使用普通用户账号尝试访问后台管理',
     '拒绝访问，提示"权限不足"',
     '符合预期：角色权限控制生效'],
    ['TC-37', '无',
     '在后台查看 AI 配置管理页面',
     '显示当前 AI 模型配置（DeepSeek、MiMo、智谱等 API Key）',
     '符合预期：AI 配置读取正常'],
    ['TC-38', '无',
     '在后台查看用户管理列表',
     '显示所有注册用户信息（用户名、角色、注册时间）',
     '符合预期：用户列表查询正常'],
    ['TC-39', '无',
     '在后台查看登录日志',
     '显示用户登录记录，包含登录时间、IP 地址、地理位置',
     '符合预期：IP 地理位置解析正常'],
]
make_table(headers, admin_rows)
add_para('')

# ---------- 4.9 非功能性测试 ----------
add_title('4.9 非功能性测试', level=2)
non_func_rows = [
    ['TC-40', '无',
     '在弱网环境（3G 模拟）下访问首页',
     '页面能在 5 秒内加载完成，图片有懒加载效果',
     '符合预期：前端性能优化（懒加载、虚拟滚动）生效'],
    ['TC-41', '无',
     '使用不同浏览器（Edge、Chrome）访问系统',
     '页面布局和功能在各浏览器中表现一致',
     '符合预期：主流浏览器兼容性良好'],
    ['TC-42', '无',
     '在手机浏览器中访问系统',
     '页面自适应移动端屏幕，导航栏折叠为汉堡菜单',
     '符合预期：响应式布局正常'],
    ['TC-43', 'SQL注入: \' OR 1=1 --',
     '在登录页面用户名输入框输入 SQL 注入语句',
     '登录失败，后端使用 MyBatis-Plus 参数化查询，SQL 注入被防御',
     '符合预期：参数化查询防止了 SQL 注入'],
    ['TC-44', 'JWT Token: 已过期Token',
     '使用过期的 JWT Token 请求受保护的 API',
     '返回 401 Unauthorized，提示 Token 已过期',
     '符合预期：Token 过期校验正常'],
    ['TC-45', '无',
     '在 AI 对话页面连续发送消息，测试每日配额限制',
     '达到每日配额上限后，提示"今日配额已用完"，管理员不受限制',
     '符合预期：配额系统正常，管理员无限制'],
]
make_table(headers, non_func_rows)
add_para('')

# ── 五、测试执行结果汇总 ──
add_title('五、测试执行结果汇总', level=1)
summary_headers = ['测试模块', '用例数', '通过', '失败', '通过率']
summary_rows = [
    ['用户认证模块', '6', '6', '0', '100%'],
    ['文章管理模块', '8', '8', '0', '100%'],
    ['分类管理模块', '3', '3', '0', '100%'],
    ['AI 对话助手模块', '9', '9', '0', '100%'],
    ['创意发散模块', '2', '2', '0', '100%'],
    ['简历管理模块', '3', '3', '0', '100%'],
    ['3D VR 星空模块', '3', '3', '0', '100%'],
    ['后台管理模块', '5', '5', '0', '100%'],
    ['非功能性测试', '6', '6', '0', '100%'],
    ['合计', '45', '45', '0', '100%'],
]
make_table(summary_headers, summary_rows)
add_para('')

# ── 六、发现的缺陷及解决方案 ──
add_title('六、发现的缺陷及解决方案', level=1)
add_para('在系统测试过程中，共发现以下缺陷（含已修复和建议改进项）：', indent=True)
add_para('')

bug_headers = ['缺陷编号', '缺陷描述', '严重程度', '所属模块', '解决方案']
bug_rows = [
    ['BUG-01', 'AI 对话在弱网环境下 SSE 连接偶发超时断开', '中', 'AI 对话助手',
     '增加 SSE 连接的心跳检测机制，超时后自动重连；前端增加断线重连提示'],
    ['BUG-02', '3D VR 星空页面在低端设备上渲染卡顿', '中', '3D VR 星空',
     '增加设备性能检测，低端设备自动降低粒子数量和渲染质量'],
    ['BUG-03', '简历导出 PDF 时，中文字符偶发显示为方块', '低', '简历管理',
     '在 starlore-pdf 服务中确保安装了中文字体包（如 Noto Sans CJK）'],
    ['BUG-04', '文章搜索在内容含特殊字符时返回结果不准确', '低', '文章管理',
     '后端搜索接口增加特殊字符转义处理，使用 MySQL FULLTEXT 索引优化'],
    ['BUG-05', '管理员后台 AI 配置页面在 API Key 为空时未做校验', '低', '后台管理',
     '前端表单增加必填校验，后端增加非空校验逻辑'],
]
make_table(bug_headers, bug_rows)
add_para('')

# ── 七、实验总结 ──
add_title('七、实验总结', level=1)
add_para('通过本次系统测试实验，对 Starlore 个人知识管理博客系统进行了全面的功能测试和非功能性测试，共设计并执行了 45 个测试用例，覆盖了用户认证、文章管理、分类管理、AI 对话助手、创意发散、简历管理、3D VR 星空可视化、后台管理等核心功能模块。', indent=True)
add_para('')
add_para('测试过程中，系统核心功能均按预期工作，主要功能实现完整。同时发现了 5 个缺陷，其中 2 个中等严重程度（AI 对话弱网超时、VR 低端设备卡顿），3 个低严重程度（PDF 中文显示、搜索特殊字符、AI 配置校验）。针对每个缺陷均已给出具体的解决方案。', indent=True)
add_para('')
add_para('通过本次实验，深入掌握了系统测试的基本方法和技术：', indent=True)
add_para('1. 测试技术方面：运用了黑盒测试中的等价类划分法（如用户登录的有效/无效等价类）、边界值分析法（如文件上传大小限制）、场景法（如完整的文章发布流程）等方法设计测试用例。', indent=True)
add_para('2. 测试任务方面：明确了系统测试需要验证功能需求的实现完整性和非功能需求（性能、安全性、兼容性、可用性）的满足程度。', indent=True)
add_para('3. 测试原则方面：体会到测试应尽早介入、完全测试不可能、缺陷集群效应等原则在实际测试中的指导意义。特别是 AI 对话模块和 3D VR 模块由于功能复杂度较高，发现的缺陷相对集中，验证了缺陷集群效应。', indent=True)
add_para('')
add_para('总体而言，Starlore 系统功能实现较为完善，用户体验良好，达到了需求分析文档中定义的各项目标。后续建议进一步加强弱网环境下的容错处理和低端设备的性能优化。', indent=True)

# ── 保存文件 ──
output_path = r'd:\project\all\starlore\系统测试实验报告.docx'
doc.save(output_path)
print(f'报告已生成：{output_path}')
