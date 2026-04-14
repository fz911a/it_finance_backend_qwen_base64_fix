# AI 接入说明

当前后端已经预留这些接口：

- POST /api/ai/invoice/ocr
- POST /api/ai/receipt/ocr
- POST /api/ai/bookkeeping/chat
- POST /api/ai/report/summary
- POST /api/ai/risk/analyze

## 当前状态

当前 `AiServiceImpl` 返回的是演示数据，前端已经能联调页面流程。

## 真正接入 AI 的位置

你只需要修改：

`src/main/java/com/example/itfinance/service/impl/AiServiceImpl.java`

把里面演示返回改成真实 HTTP 请求即可。

## 推荐接入方式

使用 OpenAI 兼容格式的模型服务，例如：
- OpenAI 兼容网关
- DeepSeek
- 通义千问兼容接口
- 智谱兼容接口

## 配置位置

`src/main/resources/application.yml`

```yaml
app:
  ai:
    provider: openai-compatible
    base-url: https://你的模型接口地址
    api-key: 你的key
    chat-model: 你的文本模型
    vision-model: 你的视觉模型
    enabled: true
```

## 建议实现思路

### 发票识别
- 前端上传图片
- 后端拿到 fileUrl
- 读取图片并交给视觉模型
- 要求模型返回 JSON：
  - invoiceNo
  - invoiceDate
  - amount
  - taxAmount
  - customerName
  - projectName

### 小票识别
- 前端上传小票
- 后端调用视觉模型
- 要求模型返回 JSON：
  - merchantName
  - amount
  - expenseType
  - expenseDate
  - projectId

### 风险分析
- 后端先查数据库拿财务汇总
- 再把汇总结果作为 prompt 发给大模型
- 返回风险等级和建议
