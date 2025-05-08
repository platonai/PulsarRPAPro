# Docker for development

Create a docker image for development:

```shell
docker build -t pulsar-rpa-pro . -f docker/pulsar-rpa-dev/Dockerfile
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

Test MongoDB on docker:

```shell
 mongosh mongodb://mongodb:27017
```
