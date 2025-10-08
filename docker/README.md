# 🐳 Docker Guide

## ⚙️ Run with Docker Compose

```bash
export DEEPSEEK_API_KEY=your-api-key
# export PROXY_ROTATION_URL=https://your-proxy-provider.com/rotation-endpoint
docker compose up -d
```

> 💡 Make sure DEEPSEEK_API_KEY is set in your environment

## ✅ Test PulsarRPA API

```bash
curl -X POST "http://localhost:8182/api/ai/command" \
  -H "Content-Type: text/plain" \
  -d '
    Go to https://www.amazon.com/dp/B08PP5MSVB
    After page load: click #title, then scroll to the middle.

    Summarize the product.
    Extract: product name, price, ratings.
    Find all links containing /dp/.
  '
```

## 🌐 Run Docker Compose with Proxy Profile

```bash
docker compose up -d --profile proxy
```

## 🗄️ Run Only MongoDB Service

```bash
docker compose up -d mongodb
```

## 🔗 Run Only ProxyHub Service

```bash
docker compose up -d proxyhub
```
