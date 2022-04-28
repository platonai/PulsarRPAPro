Exotic README
===================

Exotic is the professional edition of Pulsar with advanced AI support to do auto web mining.

There are already some scraping examples for the most popular websites, we are keeping adding more cases.

Exotic demonstrates:

- How to use pulsar as a library
- How to use pulsar driver to access the Pulsar REST Service
- How to extract almost every field in any webpages automatically in an unsupervised manner using our advanced AI

## Requirements

- Memory 4G+
- Maven 3.2+
- The latest version of the Java 11 OpenJDK
- java and jar on the PATH
- Google Chrome 90+
- MongoDB for REST Services

## Build

    cd exotic
    mvn clean && mvn

## Run the standalone server and web console

    ./start.sh

## Run auto web mining examples

    ./harvest.sh
