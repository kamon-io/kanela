# Kamon Agent <img align="right" src="https://rawgit.com/kamon-io/Kamon/master/kamon-logo.svg" height="150px" style="padding-left: 20px"/>
[![Build Status](https://travis-ci.org/kamon-io/kamon-agent.svg?branch=master)](https://travis-ci.org/kamon-io/kamon-agent)

The **kamon-agent** is developed in order to provide a simple way to instrument an application running on the JVM and
introduce kamon features such as, creation of traces, metric measures, trace propagation, and so on.

It's a simple Java Agent written in Java 8 and powered by [ByteBuddy] with some additionally [ASM] features. It has a Pure-Java API and a
Scala-Friendly API to define the custom instrumentation in a declarative manner.

- Continue to the new :sparkles: [Microsite](http://kamon-io.github.io/kamon-agent/) :sparkles:
- Read the [**Changelog**](CHANGELOG.md) for more info.
