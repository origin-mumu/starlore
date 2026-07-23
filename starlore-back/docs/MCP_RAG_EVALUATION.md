# MCP 与 RAG 评估

## MCP 配置

生产后端默认不连接任何外部 MCP Server。通过 `APP_MCP_SERVERS_JSON` 配置受信任的
stdio Server，配置内容是 JSON 数组：

```json
[
  {
    "name": "filesystem",
    "command": "mcp-server-filesystem",
    "args": ["/mcp-data"],
    "env": {}
  }
]
```

Docker 镜像已安装官方 Filesystem MCP Server。若启用它，还需在部署环境中将允许访问的
目录显式挂载到容器内，例如只读挂载：

```yaml
services:
  starlore-back:
    volumes:
      - /opt/starlore/mcp-data:/mcp-data:ro
```

不要把密钥写入 JSON 文件或 Git。外部服务的密钥通过部署环境注入，配置中
使用 `"ZHIPU_API_KEY": "${ZHIPU_API_KEY}"` 引用；后端会在启动时解析环境变量，不输出值。

Agent 可使用两个本地桥接工具：

- `listMcpTools`：连接所有已配置 Server，动态获取工具名称、说明及输入 Schema。
- `callMcpTool`：根据 Server、工具名称和 JSON 参数调用外部工具。

MCP 子进程与会话在应用生命周期内保持连接，应用停止时统一关闭。

登录后可通过 `GET /api/ai/mcp/tools` 检查已连接的 Server 和动态发现的工具。

### 内置实时搜索

智谱联网搜索使用官方托管的远程 MCP Server，不需要在 Docker 镜像内安装 Node 包。生产环境只需提供：

```dotenv
ZHIPU_API_KEY=从智谱开放平台获取的 API Key
```

后端检测到该变量后，会自动注册名为 `zhipu-web-search` 的 MCP Server，不需要再手写
`APP_MCP_SERVERS_JSON`。未设置环境变量时，后端会尝试复用数据库中已启用的
`zhipu-embedding` 配置密钥；两处都没有密钥时实时搜索保持关闭，知识库工具和对话功能不受影响。

## RAG 四维评估

接口：

```http
POST /api/ai/rag/evaluate
Authorization: Bearer <token>
Content-Type: application/json
```

请求示例：

```json
{
  "question": "Starlore 的部署流程是什么？",
  "answer": "待评估的 RAG 回答",
  "groundTruth": "可选标准答案",
  "topK": 5
}
```

后端从当前用户的文章向量索引中召回上下文，再通过 LLM-as-judge 计算：

- `faithfulness`
- `answerRelevance`
- `contextPrecision`
- `contextRecall`
- 加权 `overall`

返回的 `evaluator` 为 `ragas-aligned-llm-judge`。这表示指标定义与常见 RAGAS 四维评估
对齐，但生产 Java 后端没有伪装成直接运行 Python `ragas` 包。
