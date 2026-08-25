# GCAP Client (Google Cloud Agent Platform Client)

<p align="center">
  <img src="docs/icon.svg" width="120" height="120" alt="GCAP Client Icon" />
</p>

<p align="center">
  <b>面向 Google Cloud Agent Platform 与 OpenAI 兼容服务商的现代化 Android 原生 AI 客户端</b><br>
  极简 Express Mode · 1:1 Vertex AI 协议复刻 · OpenAI 兼容中转接入 · 多会话并行流式接收 · 全文件多模态交互
</p>

---

## 📖 项目简介

**GCAP Client** 是一款专为 Google Cloud Agent Platform / Vertex AI 以及 OpenAI 兼容服务商（如 One-API、New-API、DeepSeek、Moonshot Kimi、智谱 GLM、MiniMax、Grok 等）设计的现代化 Android 原生 AI 客户端。

用户不仅可以通过 Google AI Studio 或 GCP API Key（Express Mode）直连 Google Vertex AI 全系列官方模型，还可自由添加任意第三方兼容服务商，一键探测导入模型并调控视觉与推理能力。

应用采用现代 Android 响应式架构（Jetpack Compose + Material 3 + Hilt + Room + Kotlinx Serialization + OkHttp SSE），全面支持多对话并行流式生成、思维链独立折叠、网页实时检索引用、图片创作生成以及全格式文件多模态分析。

---

## ✨ 核心特性

### 1. 双协议引擎与 1:1 全参数 GUI 调控
- **Google Cloud Agent Platform 原生协议**：
  - **Gemini 3.7 / 3.6 / 3.5 系列**：系统指令（`systemInstruction`）、思考强度（`OFF`、`MINIMAL`、`LOW`、`MEDIUM`、`HIGH`）、Google Search / Maps 工具、语言检索配置（`toolConfig`）。
  - **全参数采样调节**：精确调控采样温度（`temperature`）、Top P、最大输出 Tokens 及 4 项安全过滤阈值。
- **OpenAI 兼容协议支持**：
  - 支持自定义服务商（Base URL + API Key）管理，一键 `GET /models` 探测导入模型列表；
  - 单模型能力覆盖调控（支持视觉输入、思考推理开关与 `reasoning_effort` 传参）；
  - 双模式思维链自动提取：标准 `<think>...</think>` 标签解析与 `delta.reasoning_content` 流式提取；
  - 视频多模态 Base64 `video_url` 自动封装。

### 2. ⚡ 多对话并行流式接收（Multi-Conversation Background Streaming）
- **会话级流式调度池**：基于 `ConcurrentHashMap<String, ActiveStreamState>` 架构，每个对话的流式请求与 Token 接收完全独立；
- **后台平滑生成与持久化**：在模型回复未完成时切换至其他对话或开启新对话，**正在生成的回复不会被中断或丢弃**，后台持续接收并在完成后自动存入本地数据库；
- **无缝恢复渲染**：切回正在生成的历史会话时自动恢复实时打字机动画与状态。

### 3. 沉浸式现代 AI 交互与动效
- **无气泡全宽排版**：模型回复在全屏背景上全宽展示，排版开阔自然；
- **💭 思维链独立折叠**：自动分离思考流（Thinking），胶囊折叠块实时展示字数并支持一键展开/折叠；
- **🛠️ 工具调用与网页来源**：实时展示 Google Search Grounding 关键词与参考来源，支持点击直接跳转网页；
- **Markdown 深度渲染**：支持代码块一键复制、超长表格横向平滑滚动；
- **滑动吸底与触觉反馈**：长回复自适应「回到底部」浮钮，全局按键与交互集成轻量触觉震动反馈（Haptic Feedback）；
- **Token 消耗统计**：模型消息底部实时展示输入与输出 Token 统计（`↑输入 ↓输出 tok`）。

### 4. 全格式多模态文件上传与媒体查看
- 支持从相册、相机或系统文件管理器上传任意素材：
  - 🖼️ **图片**（JPEG、PNG、WEBP、HEIC 等）
  - 🎬 **视频**（MP4、MOV、AVI、MKV 等）
  - 🎵 **音频**（MP3、WAV、AAC、FLAC、M4A 等）
  - 📄 **文档与代码**（PDF、TXT、Markdown、JSON、Python、Java、C++ 等）
- **应用内全屏大图预览**：点击用户上传或模型生成的图片 Chip 即可直接进入手势缩放全屏预览弹窗（`FullScreenImageDialog`），支持双指缩放、拖拽与一键保存至相册；
- **FileProvider 安全文件调起**：音视频与文档通过安全 Content URI 唤起系统应用选择器播放或查看。

### 5. 消息精细交互与会话管理
- **文本自由划选**：消息正文与思维链内容均支持系统原生光标选中与复制；
- **模型回复覆盖编辑**：编辑模型回复后仅更新当前消息，不破坏上下文；
- **用户消息原位重发**：编辑用户消息后支持原位截断并触发模型重新生成；
- **新建对话纯净重置**：新建对话时自动清空系统提示词，防止上下文污染；
- **历史对话二次确认**：会话列表增加删除二次确认弹窗，防止误删重要历史。

### 6. 高性能本地离线存储
- **Room 数据库**：采用原生 `@Upsert` 策略与版本化迁移架构（Version 4），持久化记录各会话的独立系统提示词与各条消息的真实模型名称（`modelName`）；
- **大图独立存储**：通过 `ImageStorageManager` 将大图与媒体附件存入应用私有沙盒目录，数据库仅保留 URI 引用，彻底杜绝 SQLite 2MB CursorWindow 溢出崩溃；
- **设置持久化**：DataStore 存储 API Key、默认模型偏好与主题外观（深色/浅色/跟随系统）。

---

## 🤖 支持模型一览

| 类别 | 模型 ID / 系列 | 核心特性 |
| :--- | :--- | :--- |
| **Vertex AI 对话** | `gemini-3.7-flash` | 混合推理、思维链控制、Google Search & Maps 工具 |
| **Vertex AI 对话** | `gemini-3.6-flash` | 高速生成、思维链控制、工具检索 |
| **Vertex AI 对话** | `gemini-3.5-flash-lite` | 轻量极速响应、工具检索 |
| **Vertex AI 对话** | `gemini-3.5-flash` | 全参数采样（Temperature、Top P）、思考控制 |
| **Vertex AI 对话** | `gemini-3.1-flash-lite` | 轻量通用、全采样控制 |
| **Vertex AI 对话** | `gemini-3.1-pro-preview` | 复杂推理与深度分析、全采样控制 |
| **Vertex AI 创作** | `gemini-3.1-flash-image` | Nano Banana 2、图文混合生成、全套 ImageConfig 调控 |
| **OpenAI 兼容模型** | DeepSeek V3/V4 / Kimi K 系列 / GLM-5 系列 / MiniMax / Grok / GPT-4o 等 | 自定义 Base URL，支持视觉分析与思维链推理 |

---

## 🛠️ 技术栈与工程架构

```
app/src/main/java/com/gcap/client/
├── GcapApplication.kt               # Hilt Application 入口
├── MainActivity.kt                  # Edge-to-Edge Single Activity
├── data/
│   ├── model/
│   │   ├── ApiModels.kt            # 1:1 Vertex AI 请求与响应 DTO
│   │   ├── OpenAiModels.kt         # OpenAI 兼容协议请求/响应与 SSE 解析实体
│   │   ├── ChatMessage.kt          # UI 状态实体
│   │   └── ModelDefinition.kt      # 模型注册中心与特性元数据
│   ├── network/
│   │   ├── GeminiApiService.kt     # Vertex AI SSE 流式网络接口
│   │   ├── OpenAiApiService.kt     # OpenAI 兼容流式与探测接口
│   │   └── SseStreamParser.kt      # SSE / JSON 流容错解析器
│   ├── local/
│   │   ├── AppDatabase.kt         # Room 数据库 (Schema v4)
│   │   ├── ConversationEntity.kt  # 会话、消息、服务商与自定义模型实体
│   │   ├── ConversationDao.kt     # 会话与消息 DAO (Upsert 与顺序截断)
│   │   ├── CustomProviderDao.kt   # 自定义服务商与模型 DAO
│   │   ├── ImageStorageManager.kt # 本地文件持久化与 URI 映射
│   │   └── SettingsDataStore.kt   # Preferences DataStore 设置仓库
│   └── repository/
│       └── GeminiRepository.kt    # 数据仓库聚合层
├── di/
│   ├── AppModule.kt               # Room & DataStore 依赖注入
│   └── NetworkModule.kt           # OkHttpClient & Json 配置
└── ui/
    ├── theme/                     # Material 3 主题配色与排版
    ├── navigation/
    │   └── MainNavHost.kt         # 底部导航与输入法避让协同
    ├── chat/
    │   ├── ChatScreen.kt          # 聊天主界面与回到底部浮钮
    │   ├── ChatViewModel.kt       # 聊天业务状态机与多会话流式调度池
    │   ├── MessageBubble.kt       # 全宽消息视图、全屏大图预览与折叠卡片
    │   └── ParameterPanel.kt      # 全参数配置抽屉
    ├── image/
    │   ├── ImageChatScreen.kt     # 图片创作主界面
    │   ├── ImageChatViewModel.kt  # 图片创作状态机与流式调度
    │   └── ImageParameterPanel.kt # 图片参数抽屉
    ├── settings/
    │   ├── SettingsScreen.kt      # 基础设置与服务商管理列表
    │   ├── CustomProviderDialog.kt# 自定义服务商配置与模型探测弹窗
    │   └── ModelCapabilityDialog.kt# 单模型能力覆盖配置弹窗
    └── components/
        ├── MarkdownText.kt        # Markdown 渲染与横向滑动表格
        ├── ModelSelector.kt       # 手风琴折叠式多服务商模型选择器
        ├── SafetySettingsPanel.kt # 4项安全拦截阈值组件
        ├── ImagePicker.kt         # 多模态文件与相册选择器
        └── ConversationDrawer.kt  # 历史会话侧边栏与删除确认弹窗
```

---

## 🚀 编译与安装

### 环境要求
- **Android SDK**：`minSdk = 26`（Android 8.0+），`targetSdk = 35`
- **JDK**：Java 17 或 Java 21
- **Gradle**：8.13+

### 编译构建
```bash
# 克隆仓库
git clone https://github.com/chamgent/gcap-client.git
cd gcap-client

# 执行单元测试
./gradlew testDebugUnitTest

# 构建 Debug APK
./gradlew assembleDebug

# 构建 Release APK
./gradlew assembleRelease
```
产物位置：
- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release: `app/build/outputs/apk/release/app-release-unsigned.apk` (或配置了签名的 release APK)

### ADB 一键安装
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 📄 开源许可证

本项目基于 [MIT License](LICENSE) 开源。欢迎提交 Issue 与 Pull Request！

---

## 📄 开源许可证

本项目基于 [MIT License](LICENSE) 开源。
