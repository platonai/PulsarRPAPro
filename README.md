# PulsarRPAPro README

**English** | [简体中文](README-CN.md) | [中国镜像](https://gitee.com/platonai_galaxyeye/exotic)

![Auto Extraction Result Snapshot](docs/amazon.png)

**PulsarRPAPro** is the professional version of PulsarRPA, featuring an upgraded server, a collection of top e-commerce site scraping examples, and an advanced AI-powered applet for automatic data extraction.

**Never write another web scraper. PulsarRPAPro learns from the website and delivers web data completely and accurately at scale.**

There are already dozens of [scraping cases](exotic-app/exotic-examples/src/main/kotlin/ai/platon/exotic/examples/sites/) for the most popular websites, and we are constantly adding more.

## Videos

YouTube: 
[![Watch the video](https://img.youtube.com/vi/qoXbnL4wdtc/0.jpg)](https://www.youtube.com/watch?v=qoXbnL4wdtc)

Bilibili: 
[https://www.bilibili.com/video/BV1Qg4y1d7kA](https://www.bilibili.com/video/BV1Qg4y1d7kA)

## Features

- Fully Automated Web Data Extraction——No Rules, Just Results!
- Web spider: browser rendering, ajax data crawling
- High performance: optimized for rendering hundreds of pages in parallel on a single machine without being blocked
- Low cost: scrape 100,000 browser-rendered e-commerce webpages or millions of data points daily with only 8-core CPU/32GB memory
- Web UI: a simple yet powerful web interface to manage spiders and download data
- Machine learning: automatically extract every field in webpages using unsupervised machine learning, generating extraction rules and SQLs
- Data quality assurance: smart retry, accurate scheduling, web data lifecycle management
- Large scale: fully distributed, designed for large-scale crawling
- Simple API: a single line of code to scrape, or a single SQL query to turn a website into a table
- X-SQL: extended SQL to manage web data — web crawling, scraping, content mining, and web BI
- Bot stealth: IP rotation, web driver stealth, and anti-ban mechanisms
- RPA: simulate human behaviors, SPA crawling, or perform other advanced tasks
- Big data: supports various backend storage systems like MongoDB, HBase, and Gora
- Logs & metrics: all events are monitored and recorded for detailed tracking

## System Requirements

- Memory 4G+
- JDK 17+
- Google Chrome 90+
- MongoDB started

## Download & Run

Download the latest executable jar:

```bash
wget http://static.platonic.fun/repo/ai/platon/exotic/PulsarRPAPro.jar
# start MongoDB
docker-compose -f docker/docker-compose.yaml up
java -jar PulsarRPAPro.jar
java -jar PulsarRPAPro.jar harvest "https://www.amazon.com/b?node=1292115011" -diagnose -refresh
```

## Build from Source

Add the following lines to your `.m2/settings.xml`:

```xml
<mirrors>
    <mirror>
        <id>maven-default-http-blocker</id>
        <mirrorOf>dummy</mirrorOf>
        <name>Dummy mirror to override default blocking mirror that blocks http</name>
        <url>http://0.0.0.0/</url>
    </mirror>
</mirrors>
```

```bash
git clone https://github.com/platonai/PulsarRPAPro.git
cd PulsarRPAPro
./mvnw clean && ./mvnw
cd PulsarRPAPro/target/

# Don't forget to start MongoDB
docker-compose -f docker/docker-compose.yaml up
```

For Chinese developers, we strongly suggest following [this](https://github.com/platonai/pulsarr/blob/master/bin/tools/maven/maven-settings.adoc) guide to accelerate the build process.

## Run the Standalone Server and Open Web Console

```bash
java -jar PulsarRPAPro.jar serve
```

If PulsarRPAPro is running in GUI mode, the web console should open within a few seconds, or you can open it manually at:

[http://localhost:2718/exotic/crawl/](http://localhost:2718/exotic/crawl/)

## Run Auto Extraction

You can use the `harvest` command to learn from a set of item pages using unsupervised machine learning.

```bash
java -jar PulsarRPAPro.jar harvest "https://www.amazon.com/b?node=1292115011" -diagnose -refresh
```

The URL in the command should be a portal URL, such as a product listing page URL.

PulsarRPAPro will visit the portal, identify the optimal set of links for item pages, retrieve those pages, and analyze them.

Here is the full page of the auto extraction result in HTML format:

[Auto Extraction Result of Amazon](docs/amazon-harvest-result.html)

## Explore the PulsarRPAPro Executable Jar

Run the executable jar directly for help and to explore more features:

```bash
java -jar PulsarRPAPro.jar
```

This command will print the help message and some of the most useful examples.

## Q&A

**Q: How to use proxies?**

**A:** Follow [this guide](bin/tools/proxy/README.md) for proxy rotation.

