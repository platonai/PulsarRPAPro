# PulsarRPAPro 🚀

[English](README.md) | **简体中文** | [中国镜像](https://gitee.com/platonai_galaxyeye/exotic)

*全自动网页数据提取——无需规则，即刻获取结果！* ✨

## 核心特性 🌟

* 🏃‍♂️ 超快速！
* 🎯 高准确！
* 🤖 基于机器学习，无需支付代币！

![自动提取结果截图](docs/amazon.png)

## 演示视频 🎥

YouTube: 
[![观看视频](https://img.youtube.com/vi/qoXbnL4wdtc/0.jpg)](https://www.youtube.com/watch?v=qoXbnL4wdtc)

Bilibili: 
[https://www.bilibili.com/video/BV1Qg4y1d7kA](https://www.bilibili.com/video/BV1Qg4y1d7kA)

## 快速开始 🚀

### 下载

下载最新的可执行jar包：

```bash
wget http://static.platonic.fun/repo/ai/platon/exotic/PulsarRPAPro.jar
```

### 环境要求

启动MongoDB服务：
```shell
# 确保MongoDB在27017端口启动且无需认证
docker-compose -f docker/docker-compose.yaml up
```

## 自动提取指南 📚

使用 `harvest` 命令通过无监督机器学习从一组商品页面中学习：

```bash
java -jar PulsarRPAPro.jar harvest "https://www.amazon.com/b?node=1292115011" -diagnose -refresh
```

> 💡 URL应为门户URL，例如商品列表页面URL。

PulsarRPAPro将：
1. 访问门户页面
2. 识别商品页面的最优链接集合
3. 获取这些页面
4. 自动分析内容

### 示例结果

查看我们的HTML格式自动提取结果示例：

[亚马逊自动提取结果](docs/amazon-harvest-result.html)
