# PulsarRPAPro 🚀

**English** | [简体中文](README-CN.md) | [中国镜像](https://gitee.com/platonai_galaxyeye/exotic)

*Fully Automated Web Data Extraction—No Rules, Just Results!* ✨

## Key Features 🌟

* 🏃‍♂️ Superfast!
* 🎯 Accurate!
* 🤖 Machine Learning based, no money for tokens!

![Auto Extraction Result Snapshot](docs/amazon.png)

## Demo Videos 🎥

YouTube: 
[![Watch the video](https://img.youtube.com/vi/qoXbnL4wdtc/0.jpg)](https://www.youtube.com/watch?v=qoXbnL4wdtc)

Bilibili: 
[https://www.bilibili.com/video/BV1Qg4y1d7kA](https://www.bilibili.com/video/BV1Qg4y1d7kA)

## Quick Start 🚀

### Download

Download the latest executable jar:

```shell
curl -L -o PulsarRPAPro.jar http://static.platonai.cn/repo/ai/platon/exotic/PulsarRPAPro.jar
```

### Prerequisites

Start the MongoDB service:
```shell
# MAKE sure MongoDB is started at port 27017 without authentication
docker-compose -f docker/docker-compose.yaml up
```

## Auto Extraction Guide 📚

Use the `harvest` command to learn from a set of item pages using unsupervised machine learning:

```bash
java -jar PulsarRPAPro.jar harvest "https://www.amazon.com/b?node=1292115011" -diagnose -refresh
```

> 💡 The URL should be a portal URL, such as a product listing page URL.

PulsarRPAPro will:
1. Visit the portal
2. Identify the optimal set of links for item pages
3. Retrieve those pages
4. Analyze them automatically

### Example Results

Check out our sample auto extraction result in HTML format:

[Auto Extraction Result of Amazon](docs/amazon-harvest-result.html)
