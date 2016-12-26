# Kamon Agent

The `kamon-agent` is developed in order to provide a simple way to implement an application running on the JVM and introduce kamon features such as, creation of traces, metric measures, trace propagation, and so on.
It's a simple Java Agent powered by [ByteBuddy] and some additionally [ASM] features. It has a Pure-Java API and a Scala-Friendly API to define the custom instrumentation in a declarative manner.

[ByteBuddy]:(http://bytebuddy.net/#/)
[ASM]:(http://asm.ow2.org/)
