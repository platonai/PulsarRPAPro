# 🚀 PulsarRPAPro

**简体中文** | [English](README.md) | [中国镜像 🇨🇳](https://gitee.com/platonai_galaxyeye/exotic)

💖 PulsarRPAPro：AI驱动的极速浏览器自动化解决方案！💖

---

# 🌟 主要特性

**PulsarRPAPro 核心能力：**

* 👽 **自动抽取**
  * 🤖 机器学习智能体驱动，无需Token费用！
  * 🎯 高精度数据抽取
  * 🏃‍♂️ 极速性能

* 🌐 **Web UI** — 简单易用

* ⌨️ **命令行界面（CLI）** — 可脚本化，自动化友好

**高级特性：**

* 🤖 **LLM集成** — 大模型驱动更智能的自动化
* ⚡ **超快自动化** — 协程安全的浏览器并发，爬虫级速度
* 🧠 **深度网页理解** — 智能解析动态、JS丰富页面
* 📊 **结构化数据API** — 轻松提取干净结构化数据

---

🤖 只需文本即可大规模自动化浏览器并抽取数据：

```text
访问 https://www.amazon.com/dp/B08PP5MSVB
页面加载后：滚动到中间。

总结该商品。
提取：商品名称、价格、评分。
找出所有包含 /dp/ 的链接。
```

---

👽 机器学习智能体自动抽取数据：

![自动抽取结果快照](docs/assets/images/amazon.png)

---

# 🎥 演示视频

* **YouTube**：[观看视频](https://www.youtube.com/watch?v=qoXbnL4wdtc)
* **Bilibili**：[https://www.bilibili.com/video/BV1Qg4y1d7kA](https://www.bilibili.com/video/BV1Qg4y1d7kA)

---

# 🚀 快速开始

## ▶️ 运行 PulsarRPAPro

### 📦 运行可执行Jar

下载：

```bash
curl -L -o PulsarRPAPro.jar https://github.com/platonai/PulsarRPAPro/releases/download/v3.0.0/PulsarRPAPro.jar
```

运行：

```bash
java -jar PulsarRPAPro.jar
```

<details>
<summary>📂 相关资料</summary>

* 🟦 [GitHub Release Download](https://github.com/platonai/PulsarRPA/releases/download/v3.1.0/PulsarRPA.jar)
* 📁 [Mirror / Backup Download](https://static.platonai.cn/repo/ai/platon/pulsar/)
* 🛠️ [LLM Configuration Guide](docs/config/llm/llm-config.md)
* 🛠️ [Configuration Guide](docs/config.md)

</details>

### ⚙️ 依赖

MongoDB需运行在27017端口，无需认证：

```bash
docker run -d --name mongodb -p 27017:27017 mongo:latest
```

### 🐳 Docker用户

直接运行Docker镜像：

```shell
docker run -d -p 8182:8182 -e DEEPSEEK_API_KEY=${DEEPSEEK_API_KEY} galaxyeye88/pulsar-rpa-pro:latest
```

---

# 📚 自动抽取指南：只要结构化数据！无需代码，无需Token！

使用`harvest`命令自动抽取商品列表数据：

```bash
java -jar PulsarRPAPro.jar harvest "https://www.amazon.com/b?node=1292115011" -diagnose -refresh
```

---

# 🖥️ 运行PulsarRPAPro服务端

```bash
java -DDEEPSEEK_API_KEY=${DEEPSEEK_API_KEY} -jar PulsarRPAPro.jar serve
```

---

# 🧠 LLM能力：只需文本，无需代码！

使用`ai/command` API通过自然语言指令执行操作和抽取数据。

```bash
curl -X POST "http://localhost:8182/api/ai/command" \
  -H "Content-Type: text/plain" \
  -d '
    访问 https://www.amazon.com/dp/B08PP5MSVB
    总结该商品。
    提取：商品名称、价格、评分。
    找出所有包含 /dp/ 的链接。
    页面加载后：点击 #title，然后滚动到中间。
  '
```

---

# 🔍 LLM + X-SQL：精准、灵活、强大

利用`x/e` API实现高精度、灵活、智能的数据抽取。

```bash
curl -X POST "http://localhost:8182/api/x/e" -H "Content-Type: text/plain" -d "
select
  llm_extract(dom, 'product name, price, ratings') as llm_extracted_data,
  dom_base_uri(dom) as url,
  dom_first_text(dom, '#productTitle') as title,
  dom_first_slim_html(dom, 'img:expr(width > 400)') as img
from load_and_select('https://www.amazon.com/dp/B08PP5MSVB', 'body');
"
```

🔎 示例输出：

```json
{
  "llm_extracted_data": {
    "product name": "Apple iPhone 15 Pro Max",
    "price": "$1,199.00",
    "ratings": "4.5 out of 5 stars"
  },
  "url": "https://www.amazon.com/dp/B08PP5MSVB",
  "title": "Apple iPhone 15 Pro Max",
  "img": "<img src=\"https://example.com/image.jpg\" />"
}
```

---

# 🔧 代理设置

设置环境变量：

```bash
export PROXY_ROTATION_URL=https://your-proxy-provider.com/rotation-endpoint
```

此URL应返回新的代理IP。

---

# 📞 联系我们

* 💬 **微信**：galaxyeye
* 🌐 **微博**：[galaxyeye](https://weibo.com/galaxyeye)
* 📧 **邮箱**：[galaxyeye@live.cn](mailto:galaxyeye@live.cn)，[ivincent.zhang@gmail.com](mailto:ivincent.zhang@gmail.com)
* 🐦 **Twitter**：[@galaxyeye8](https://twitter.com/galaxyeye8)
* 🌍 **官网**：[platon.ai](https://platon.ai)

<div style="display: flex;">
  <img src="docs/assets/images/wechat-author.png" width="300" height="365" alt="微信二维码" />
</div>
