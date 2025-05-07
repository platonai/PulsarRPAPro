# 🚀 PulsarRPAPro

**English** | [简体中文](README-CN.md) | [中国镜像 🇨🇳](https://gitee.com/platonai_galaxyeye/exotic)

💖 **PulsarRPA：AI驱动的极速浏览器自动化新纪元！** 💖

---

## 🌟 Key Features

PulsarRPAPro:

* 👽 AutoExtract
    * 🏃‍♂️ Superfast
    * 🎯 Accurate
    * 🤖 Machine Learning-based, no money for tokens!
* 🌐 Web UI
* ⌨️ Command Line Interface (CLI)

PulsarRPA Capabilities Included:

- 🤖 **AI Integration with LLMs** – Smarter automation powered by large language models.
- ⚡ **Ultra-Fast Automation** – Coroutine-safe browser automation concurrency, spider-level crawling performance.
- 🧠 **Web Understanding** – Deep comprehension of dynamic web content.
- 📊 **Data Extraction APIs** – Powerful tools to extract structured data effortlessly.

![Auto Extraction Result Snapshot](docs/amazon.png)

---

## 🎥 Demo Videos

* **YouTube**:
  [![Watch the video](https://img.youtube.com/vi/qoXbnL4wdtc/0.jpg)](https://www.youtube.com/watch?v=qoXbnL4wdtc)

* **Bilibili**:
  [https://www.bilibili.com/video/BV1Qg4y1d7kA](https://www.bilibili.com/video/BV1Qg4y1d7kA)

---

## 🚀 Quick Start

### 📦 Download

Download the latest executable JAR:

```bash
curl -L -o PulsarRPAPro.jar http://static.platonai.cn/repo/ai/platon/exotic/PulsarRPAPro.jar
```

### ⚙️ Prerequisites

Make sure MongoDB is running on port 27017 without authentication:

```bash
docker-compose -f docker/dev/docker-mongo.yaml up -d
```

---

## 📚 Auto Extraction Guide: Just Structured Data! No Code! No Token!

Use the `harvest` command to extract data from a product listing using unsupervised ML:

```bash
java -jar PulsarRPAPro.jar harvest "https://www.amazon.com/b?node=1292115011" -diagnose -refresh
```

> 💡 Make sure the URL is a portal page like a product category or listing.

The tool will:

1. Visit the portal
2. Identify optimal item page links
3. Retrieve those pages
4. Analyze them automatically

### 📄 Example Results

See a sample extraction result in HTML:
[Auto Extraction Result of Amazon](docs/amazon-harvest-result.html)

---

## 🖥️ Run PulsarRPAPro Server

```bash
java -DDEEPSEEK_API_KEY=${DEEPSEEK_API_KEY} -jar PulsarRPAPro.jar serve
```

---

## 🧠 LLM Capabilities: Just Text, No Code!

Send natural language instructions to control the browser:

```bash
curl -X POST "http://localhost:8182/api/ai/command" \
  -H "Content-Type: text/plain" \
  -d '
    Visit https://www.amazon.com/dp/B0C1H26C46
    Summarize the product.
    Extract: product name, price, ratings.
    Find all links containing /dp/.
    After page load: click #title, then scroll to the middle.
  '
```

---

## 🔍 LLM + X-SQL

```bash
curl -X POST "http://localhost:8182/api/x/e" -H "Content-Type: text/plain" -d "
select
  llm_extract(dom, 'product name, price, ratings') as llm_extracted_data,
  dom_base_uri(dom) as url,
  dom_first_text(dom, '#productTitle') as title,
  dom_first_slim_html(dom, 'img:expr(width > 400)') as img
from load_and_select('https://www.amazon.com/dp/B0C1H26C46', 'body');
"
```

🔎 Sample Output:

```json
{
  "llm_extracted_data": {
    "product name": "Apple iPhone 15 Pro Max",
    "price": "$1,199.00",
    "ratings": "4.5 out of 5 stars"
  },
  "url": "https://www.amazon.com/dp/B0C1H26C46",
  "title": "Apple iPhone 15 Pro Max",
  "img": "<img src=\"https://example.com/image.jpg\" />"
}
```

---

## 🔧 Proxies

Proxies are optional. Set the environment variable:

```bash
export PROXY_ROTATION_URL=https://your-proxy-provider.com/rotation-endpoint
```

This URL should return fresh proxy IPs when accessed.

---

## 📞 Contact Us

* 💬 **WeChat**: galaxyeye
* 🌐 **Weibo**: [galaxyeye](https://weibo.com/galaxyeye)
* 📧 **Email**: [galaxyeye@live.cn](mailto:galaxyeye@live.cn), [ivincent.zhang@gmail.com](mailto:ivincent.zhang@gmail.com)
* 🐦 **Twitter**: [@galaxyeye8](https://twitter.com/galaxyeye8)
* 🌍 **Website**: [platon.ai](https://platon.ai)

<div style="display: flex;">
  <img src="docs/images/wechat-author.png" width="300" height="365" alt="WeChat QR Code" />
</div>
