# GCAP Client (Google Cloud Agent Platform Client)

<p align="center">
  <img src="docs/icon.svg" width="120" height="120" alt="GCAP Client Icon" />
</p>

<p align="center">
  <b>面向 Google Cloud Agent Platform 的现代化 Android 原生 AI 客户端</b><br>
  极简 Express Mode 模式 · 1:1 请求协议复刻 · 全参数 GUI 调控 · 多模态全文件交互
</p>

---

## 📖 项目简介

**GCAP Client** 是一款专为 Google Cloud Agent Platform / Vertex AI 设计的现代 Android 原生 AI 客户端。用户只需输入在 Google AI Studio 或 GCP 控制台创建的 API Key（Express Mode），即可直接调用 Agent Platform 上的全部前沿模型。

应用采用现代 Android 架构（Jetpack Compose + Material 3 + Hilt + Room + Kotlinx Serialization + OkHttp SSE），全面支持流式多轮对话、思维链过程折叠、搜索工具与网页来源检索、图片创作与全格式文件多模态分析。

---

## ✨ 核心特性

### 1. 1:1 接口协议复刻与全参数 GUI 调控
- **双格式聊天模型支持**：
  - **Format 1**（`Gemini 3.7 Flash`, `3.6 Flash`, `3.5 Flash Lite`）：支持系统指令（`systemInstruction`）、思考程度（`thinkingLevel`）、Google 搜索与地图工具（`tools`）、语言检索配置（`toolConfig`）。
  - **Format 2**（`Gemini 3.5 Flash`, `3.1 Flash Lite`, `3.1 Pro Preview`）：支持采样温度（`temperature`）、Top P、最大 Tokens、思考程度及工具开关。
- **图片生成专用格式**（`Gemini 3.1 Flash Image` / Nano Banana 2）：支持响应模态（`responseModalities`）、宽高比例（`aspectRatio`）、1K/2K 分辨率（`imageSize`）、PNG/JPEG 格式、人物生成限制（`personGeneration`）。

### 2. 沉浸式现代 AI 界面
- **无气泡全宽排版**：模型回复直接在全屏背景上全宽展示，排版开阔自然。
- **💭 思维链独立折叠**：自动分离思考流（Thinking），胶囊折叠块显示字数并支持一键展开/折叠。
- **🛠️ 工具调用与网页来源**：实时解析 Google Search Grounding 关键词与参考来源，支持点击跳转网页。
- **Token 消耗统计**：模型消息底部实时展示输入与输出 Token 统计（`↑输入 ↓输出 tok`）。

### 3. 全格式多模态文件上传
- 支持通过相册、现场拍照或系统文件管理器上传任意素材：
  - 🖼️ **图片**（JPEG、PNG、WEBP、HEIC 等）
  - 🎬 **视频**（MP4、MOV、AVI、MKV 等）
  - 🎵 **音频**（MP3、WAV、AAC、FLAC、M4A 等）
  - 📄 **文档与代码**（PDF、TXT、Markdown、JSON、Python、Java、C++ 等）
- 自动提取元数据并在输入栏生成预览 Chip，转为 Base64 供模型多模态理解。

### 4. 消息精细交互
- **文本自由划选**：消息内所有文本与思考内容均支持系统原生光标选中与复制。
- **模型回复覆盖编辑**：编辑模型回复后仅更新当前消息，不破坏后续对话上下文。
- **用户消息原位重发**：编辑用户消息后支持原位截断并触发模型重新生成。

### 5. 图片生成与创作工作流
- 对话式文生图与图生图（Image-to-Image）；
- 生成图片点击进入全屏手势缩放预览（双指缩放、拖拽移动）；
- 一键导出保存至系统相册（`Pictures/GCAP`）。

### 6. 高性能本地离线存储
- **Room 数据库**：采用原生 `@Upsert` 策略与轻量持久化架构，杜绝外键级联误删历史。
- **大图独立存储**：通过 `ImageStorageManager` 将大图与文件存入应用私有目录，数据库仅存 URI，彻底解决 SQLite 2MB CursorWindow 溢出崩溃。
- **设置持久化**：DataStore 存储 API Key、默认模型偏好与主题外观（深色/浅色/跟随系统）。

---

## 🤖 支持模型一览

| 类别 | 模型 ID | 核心特性 |
| :--- | :--- | :--- |
| **文本与代码** | `gemini-3.7-flash` | 混合推理、思维链控制、Google Search & Maps 工具 |
| **文本与代码** | `gemini-3.6-flash` | 高速生成、思维链控制、工具检索 |
| **文本与代码** | `gemini-3.5-flash-lite` | 轻量极速响应、工具检索 |
| **文本与代码** | `gemini-3.5-flash` | 全参数采样（Temperature、Top P）、思考控制 |
| **文本与代码** | `gemini-3.1-flash-lite` | 轻量通用、全采样控制 |
| **文本与代码** | `gemini-3.1-pro-preview` | 复杂推理与深度分析、全采样控制 |
| **图片创作** | `gemini-3.1-flash-image` | Nano Banana 2、图文混合生成、全套 ImageConfig 调控 |

---

## 🛠️ 技术栈与工程架构

```
app/src/main/java/com/gcap/client/
├── GcapApplication.kt               # Hilt Application 入口
├── MainActivity.kt                  # Edge-to-Edge Single Activity
├── data/
│   ├── model/
│   │   ├── ApiModels.kt            # 1:1 请求与响应 DTO (Kotlinx Serialization)
│   │   ├── ChatMessage.kt          # UI 状态实体
│   │   └── ModelDefinition.kt      # 模型注册中心与特性元数据
│   ├── network/
│   │   ├── GeminiApiService.kt     # OkHttp 4 SSE 流式网络接口
│   │   └── SseStreamParser.kt      # SSE / JSON 流容错解析器
│   ├── local/
│   │   ├── AppDatabase.kt         # Room 数据库
│   │   ├── ConversationEntity.kt  # 会话与消息实体
│   │   ├── ConversationDao.kt     # DAO (支持 Upsert 与顺序截断)
│   │   ├── ImageStorageManager.kt # 本地文件持久化与 URI 映射
│   │   └── SettingsDataStore.kt   # Preferences DataStore 设置仓库
│   └── repository/
│       └── GeminiRepository.kt    # 数据仓库层
├── di/
│   ├── AppModule.kt               # Room & DataStore 依赖注入
│   └── NetworkModule.kt           # OkHttpClient & Json 配置
└── ui/
    ├── theme/                     # Material 3 主题配色与排版
    ├── navigation/
    │   └── MainNavHost.kt         # 底部导航与输入法避让协同
    ├── chat/
    │   ├── ChatScreen.kt          # 聊天主界面
    │   ├── ChatViewModel.kt       # 聊天业务状态机
    │   ├── MessageBubble.kt       # 全宽消息视图与折叠卡片
    │   └── ParameterPanel.kt      # 全参数配置抽屉
    ├── image/
    │   ├── ImageChatScreen.kt     # 图片创作主界面
    │   ├── ImageChatViewModel.kt  # 图片创作状态机
    │   └── ImageParameterPanel.kt # 图片参数抽屉
    ├── settings/
    │   └── SettingsScreen.kt      # API Key 密码输入与连通性验证
    └── components/
        ├── MarkdownText.kt        # Markdown 渲染组件
        ├── ModelSelector.kt       # 顶部模型胶囊选择器
        ├── SafetySettingsPanel.kt # 4项安全拦截阈值组件
        ├── ImagePicker.kt         # 多模态文件与相册选择器
        └── ConversationDrawer.kt  # 历史会话侧边栏与滑动删除
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
./gradlew test

# 构建 Debug APK
./gradlew assembleDebug
```
产物位置：`app/build/outputs/apk/debug/app-debug.apk`

### ADB 一键安装
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 📄 开源许可证

本项目基于 [MIT License](LICENSE) 开源。
