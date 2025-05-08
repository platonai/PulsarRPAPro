# Docker for development

Create a docker image for development:

```shell
docker build -t pulsar-rpa-pro-dev . -f docker/pulsar-dev/Dockerfile
```

Run all services:

```shell
docker-compose -f docker/dev/docker-compose.yaml up -d
```

Run MongoDB only:

```shell
docker-compose -f docker/dev/docker-compose.yaml up -d mongodb
```

Test MongoDB:

```shell
 mongosh mongodb://localhost:27017
```