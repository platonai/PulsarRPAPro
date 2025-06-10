# PulsarRPAPro development guide


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
docker-compose -f docker/docker-compose.yml up
```

For Chinese developers, we strongly suggest following [this](https://github.com/platonai/pulsarr/blob/master/bin/tools/maven/maven-settings.adoc) guide to accelerate the build process.

## Run the Standalone Server and Open Web Console

```bash
java -jar PulsarRPAPro.jar serve
```

If PulsarRPAPro is running in GUI mode, the web console should open within a few seconds, or you can open it manually at:

[http://localhost:2718/exotic/crawl/](http://localhost:2718/exotic/crawl/)