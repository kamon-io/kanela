# Kanela: The Kamon Instrumentation Agent <img align="right" src="https://raw.githubusercontent.com/kamon-io/kamon.io/279f4d3a658437a5182e10d75aa3d55b811b2836/assets/img/kamon/kamon-icon-light.svg" height="150px" style="padding-left: 20px"/>

[![Discord](https://img.shields.io/discord/866301994074243132?label=Join%20the%20Comunity%20on%20Discord)](https://discord.gg/5JuYsDJ7au)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.kamon/kanela-agent/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.kamon/kanela-agent)

**Kanela** is a Java Agent written in Java 8+ and powered by [ByteBuddy] with some additionally [ASM] features to provide a simple way to instrument applications running on the JVM and allow introduce Kamon features such as context propagation and metrics.


## Java Support
Check this compatibility matrix to see which versions of java are supported:
[ByteBuddy Jdk Versions](https://github.com/raphw/byte-buddy#java-version-compatibility)

# Building & Testing

This project uses Mill as its build tool.
## Installing Mill

Linux/macOS:
curl -L https://github.com/com-lihaoyi/mill/releases/download/0.11.7/0.11.7 > ./mill && chmod +x mill

Or use Homebrew (macOS):
brew install mill

Windows:
Download the executable from Mill releases

## Running Tests

From the project root directory:
### Run all tests
    ./mill __.test

### Clean and rebuild
    ./mill clean
    ./mill __.compile
    ./mill __.test

### Build the agent JAR
    ./mill __.assembly

Test artifacts will be available under the out/ directory.

Note: Make sure you have JDK 8 or higher installed before building.

[ByteBuddy]:http://bytebuddy.net/#/
[ASM]:http://asm.ow2.org/
