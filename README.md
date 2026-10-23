<div align="center">

# Noesis

### 一个零依赖的 Java 智能体（Agent）与语义记忆工具集

让后端工程师用纯 Java 就能搭起「会调用工具、能检索私有知识」的大模型应用，
不引入任何第三方框架，全部代码可读、可改、可学。

![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-green)
![Dependencies](https://img.shields.io/badge/dependencies-0-blue)
![Platform](https://img.shields.io/badge/platform-JVM-brightgreen)
![Tests](https://img.shields.io/badge/tests-28%20passing-brightgreen)

[Why](#-为什么需要-noesis) ·
[Features](#-核心特性) ·
[Install](#-安装) ·
[Quick Start](#-快速开始) ·
[Architecture](#-架构) ·
[API](#-api-速查) ·
[Roadmap](#-路线图) ·
[Article](#-中文技术文章底稿引流用)

</div>

---

## ✨ 为什么需要 Noesis

大模型正在从「聊天框」走向「能干活的系统」：查数据库、算数、读文档、调内部接口。
但要把这种能力接进一个 Java 后端服务，常见路线要么是把活儿推给 Python 微服务，
要么是引入一套体量大、抽象深的框架，对只想「先跑通一个最小闭环」的工程师并不友好。

**Noesis 的出发点很简单**：用最朴素的 Java 标准库，把「模型 + 工具 + 记忆」
这三件事讲清楚、写完整。它不是一个黑盒 SDK，而是一份「可直接拿去改」的参考实现——

- 你能在 **1 个包** 里看懂 function calling 是怎么从一段 JSON 变成一次方法调用的；
- 你能在 **0 依赖** 的前提下，跑通一个带检索增强（RAG）的问答；
- 你能在 **不联网、不填密钥** 的情况下，用内置的脚本化 Provider 把整套流程跑通、写单测。

它尤其适合两类人：**想往 AI 方向补能力的 Java 后端同学**，以及 **想用一门语言把 Agent 原理吃透的学习者**。

## 🚀 核心特性

- **🔌 流式对话客户端**：`OpenAICompatibleClient` 基于 JDK 原生 `HttpClient` 实现，
  兼容 OpenAI、Azure、Ollama、vLLM 等任意 OpenAI 协议端点，支持 SSE 流式输出。
- **🛠️ 工具调用（Function Calling）**：`ToolRegistry` + `ToolExecutor` 自动生成
  JSON Schema、解析模型回传的 `tool_calls`、执行并把结果回灌对话，失败可恢复。
- **🧠 ReAct 智能体循环**：`Agent` 在「模型决策 → 调工具 → 看结果」之间反复迭代，
  直到产出最终答案或达到步数上限，自带上下文裁剪。
- **🔎 语义检索 / 向量库**：`HashEmbedding` + `VectorStore` 提供可复现的稠密向量检索，
  余弦值排序，离线即可运行；可无缝替换真实嵌入模型。
- **📚 检索增强生成（RAG）**：`RagPipeline` 把「切块 → 嵌入 → 检索 → 组装提示 → 生成」
  串成一条流水线，让模型只基于你的私有语料作答并给出出处。
- **📜 自研轻量 JSON**：为保持「零依赖」，内置一个约 300 行的递归下降 JSON 解析/生成器，
  足够覆盖与模型交互所需的全部报文形状。
- **✅ 28 个单元测全过**：核心算法（余弦相似度、SSE 解析、工具分发、Agent 循环、RAG 排序）
  均有测试守护，CI 一键复现。

## 📦 安装

Noesis 没有任何运行时依赖，拿到源码即可编译运行。

```bash
# 需要 JDK 21（用到了文本块、record 等语法）
git clone https://github.com/WsNodeX/noesis.git
cd noesis

# 编译主代码
mkdir -p build/main
find src/main/java -name '*.java' > sources.txt
javac -d build/main @sources.txt
```

要在项目里使用，把 `src/main/java` 下的 `noesis` 包整体拷进你的工程即可，
或打成 jar：`jar --create --file noesis.jar -C build/main .`

## ⚡ 快速开始

### 1) 一行命令跑通离线演示（无需 API Key）

```bash
java -cp build/main noesis.examples.Demo
```

预期输出：

```
Noesis 1.0.0 — agent + semantic-memory toolkit for the JVM
================================================================

[1] Tool-calling agent
  User   : What is 7 times 5?
  Tool   : calculator(7, 5, mul) -> 35
  Agent  : 7 multiplied by 5 equals 35.

[2] Retrieval-augmented generation
  Indexed chunks : 3
  Top sources    : [{"id":"spec","score":0.4725,"text":"Aurora supports streaming ..."}, ...]
  Answer         : Aurora supports streaming responses and tool calling, as stated in passages [1] and [2].
```

### 2) 接一个真实模型，写一个会算账的 Agent

```java
import noesis.core.agent.*;
import noesis.core.llm.*;
import noesis.core.tools.*;

public class Main {
    public static void main(String[] args) {
        LlmProvider provider = OpenAICompatibleClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .model("gpt-4o-mini")
                .build();

        ToolRegistry tools = new ToolRegistry().register(ToolSpec.of(
                "calculator", "Multiply two numbers",
                /* parameters JSON-Schema */ Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "a", Map.of("type", "number"),
                                "b", Map.of("type", "number")),
                        "required", java.util.List.of("a", "b")),
                args -> {
                    double a = ((Number) args.get("a")).doubleValue();
                    double b = ((Number) args.get("b")).doubleValue();
                    return String.valueOf(a * b);
                }));

        Agent agent = new Agent(AgentConfig.builder().model("gpt-4o-mini").build(),
                provider, tools)
                .system("You are a careful assistant. Use the calculator when needed.");

        System.out.println(agent.run("What is 1234 times 5678?"));
    }
}
```

### 3) 用私有文档做 RAG 问答

```java
RagPipeline rag = new RagPipeline(
        new HashEmbedding(),          // 生产可换 text-embedding-3-small 等
        provider, "gpt-4o-mini");
rag.ingest("manual", "你的产品说明书第一段……");
rag.ingest("manual", "你的产品说明书第二段……");
Answer answer = rag.answer("这个功能怎么用？");
System.out.println(answer.content());
```

## 🏗️ 架构

Noesis 由四层组成，依赖方向单向向下，每一层都能脱离其他层单独使用：

```
┌─────────────────────────────────────────────────────────────┐
│                         examples                            │
│              Demo / 你的业务接入代码                         │
└───────────────┬───────────────────────────┬─────────────────┘
                │                           │
        ┌───────▼────────┐          ┌───────▼────────┐
        │  core.agent    │          │  core.rag      │
        │  Agent (ReAct) │          │  RagPipeline   │
        └───────┬────────┘          └───────┬────────┘
                │                           │
   ┌────────────▼──────────┐    ┌───────────▼───────────┐
   │  core.tools           │    │  core.retrieval       │
   │  Registry/Executor    │    │  Embedding/VectorStore│
   │  (function calling)   │    │  Chunker/Retriever    │
   └────────────┬──────────┘    └───────────┬───────────┘
                │                           │
        ┌───────▼───────────────────────────▼────────┐
        │  core.llm  +  core.json                     │
        │  Chat messages · SSE client · JSON 解析      │
        └─────────────────────────────────────────────┘
                          │
                  JDK 标准库（HttpClient 等）
```

- **core.llm / core.json**：最底层，负责与模型打交道的所有「报文」细节。
- **core.tools**：把模型发出的「我想调某个函数」翻译成真实 Java 方法调用。
- **core.retrieval**：把文本变成向量、做相似度排序，是记忆的底座。
- **core.agent / core.rag**：编排层，把上面三块拼成「会思考」和「有知识」的能力。

## 🔌 API 速查

| 类 / 接口 | 关键方法 | 作用 |
| --- | --- | --- |
| `LlmProvider` | `chat(req)` / `stream(req, h)` | 对话抽象；可替换为任意后端 |
| `OpenAICompatibleClient` | `builder().baseUrl().apiKey().model().build()` | 真实流式客户端 |
| `ChatMessage` | `user()/assistant()/system()/tool()` | 构造各类消息 |
| `ToolSpec` | `of(name, desc, schema, handler)` | 声明一个工具 |
| `ToolRegistry` | `register()` / `functionDefs()` | 工具注册与协议生成 |
| `ToolExecutor` | `execute(calls, registry)` | 执行模型请求的工具调用 |
| `Agent` | `system(prompt).run(query)` | ReAct 智能体循环 |
| `HashEmbedding` | `embed(text)` | 确定性向量化（零依赖） |
| `VectorStore` | `add()` / `search(q, k)` | 内存向量索引 + 余弦排序 |
| `RagPipeline` | `ingest()` / `answer(q)` | 检索增强生成流水线 |

## 🧭 路线图

- [ ] 接入真实嵌入模型（OpenAI / 本地 `bge-small` 等）的 `Embedding` 实现
- [ ] `VectorStore` 的持久化（内存映射 / SQLite）与近似索引（HNSW）
- [ ] 多轮对话的紧凑摘要，替代简单的尾部裁剪
- [ ] 工具调用结果的缓存与超时/重试
- [ ] 更完整的 OpenAI 协议覆盖（logprobs、function 流式增量）
- [ ] Maven / Gradle 打包与中央仓库发布

## 🤝 贡献

欢迎以 Issue 或 Pull Request 的形式参与：补一个工具、换一种嵌入、写一份示例都很有价值。
本地验证只需 `javac` 编译 + JUnit 运行（见 `.github/workflows/ci.yml`），保持「零运行时依赖」是这条线的底线。

## 📄 License

[MIT](LICENSE) © Noesis contributors

---

# 📝 中文技术文章底稿（引流用）

> 下面的内容本身就是一篇可直接发布的科普文。复制后按末尾提示润色，即可发到
> 掘金 / 知乎 / 小红书 / B 站。

## 用纯 Java 手搓一个会「调工具、查文档」的 AI 智能体

**痛点开场**：很多 Java 后端同学想给系统接上大模型能力，却卡在第一步——
要么被 Python 微服务劝退，要么被动辄几十个模块的 Agent 框架劝退。其实，
一个最小可用的智能体，核心就是「模型 + 工具 + 记忆」三件事，用标准库就能写。

### 它解决什么问题

把大模型从「问答机器人」升级成「能执行动作的同事」：让它查库、算账、读内部文档。
本文项目 Noesis 用 **零依赖的纯 Java** 给出了一个完整、可读、可改的参考实现，
覆盖 function calling 与 RAG 两条最实用的落地路径。

### 核心原理（三句话讲清）

1. **ReAct 循环**：把「模型输出」分成两种——要么是最终答案，要么是一串「工具调用请求」。
   如果是后者，我们就本地执行这些工具，把结果再塞回对话，然后让模型继续想，直到它给出答案。
2. **Function Calling**：模型不直接执行代码，而是回传一段结构化的 JSON（函数名 + 参数）；
   我们在服务端用 `ToolRegistry` 找到对应 Java 方法并执行，再把结果包装成 `tool` 消息回灌。
3. **语义检索 / RAG**：把文档切块、向量化，用户提问时先检索最相关的几段，拼接成提示词让模型
   「只依据这些上下文作答」，从而解决大模型「不知道你私有知识」的问题。

### 关键实现点（技术读者最爱看）

- **零依赖 JSON**：内置一个递归下降解析器，覆盖对象/数组/转义/数字，足以处理模型报文；
  既避免引入三方库，也让「模型参数如何被序列化」完全透明。
- **SSE 流式解析**：`SseParser` 把 `data:` 行逐条抽出来，遇到 `[DONE]` 收尾，与具体 HTTP 库解耦。
- **工具分发与容错**：`ToolExecutor` 对每次调用独立 try-catch，工具抛错会变成 `ERROR:` 文本结果
  而非崩溃，给模型「自我纠错」的机会——这正是生产级 Agent 与小玩具的分水岭。
- **可替换的嵌入**：默认 `HashEmbedding` 用哈希技巧确定性地把文本映射到向量（便于离线测试），
  生产时只需实现一个 `Embedding` 接口换成真实模型即可，检索层一行不用改。

### 与同类方案对比

| 维度 | Noesis | 大型 Agent 框架（如 LangChain4j 等） |
| --- | --- | --- |
| 依赖 | 0 运行时依赖 | 较多（且常绑定生态） |
| 学习成本 | 一个包读完原理 | 抽象层厚，需先学其概念 |
| 可控性 | 每个环节都可见可改 | 黑盒较多，定制要翻源码 |
| 适合场景 | 教学 / 最小闭环 / 嵌入式 | 大型生产、需要现成集成 |

> 结论：Noesis 不是要取代成熟框架，而是帮你**先把原理跑通、把闭环跑通**；
> 真到复杂业务，你已具备「读懂并驾驭框架」的底气。

### 如何扩展成一个真实系统

1. 把 `HashEmbedding` 换成线上嵌入模型，检索质量立刻提升；
2. 给 `Agent` 多注册几个工具（查订单、发消息、读配置），它就真的能干活；
3. `VectorStore` 接上持久化与近似索引，支持百万级文档；
4. 用 `OpenAICompatibleClient` 指向你自托管的推理服务（Ollama / vLLM），数据不出内网。

### 如何把本文扩展成一篇发布稿（提示）

- **标题党版**：「不装任何框架，2000 行 Java 手搓 AI 智能体」
- **配图**：把上面的架构 ASCII 图换成 draw.io / Excalidraw 彩色版，再截一张 Demo 运行图；
- **节奏**：痛点 → 原理三句话 → 代码逐段拆解 → 对比表 → 扩展路线；
- **互动结尾**：抛一个问题「你最想让 AI 帮你自动做哪件事？」引导评论；
- **平台适配**：小红书重「图解 + 清单体」，B 站重「边写边跑的录屏」，知乎重「深度对比」。
