# Kamon Agent

The `kamon-agent` is developed in order to provide a simple way to implement an application running on the JVM and
introduce kamon features such as, creation of traces, metric measures, trace propagation, and so on.
It's a simple Java Agent written in Java 8 and is powered by [ByteBuddy] and some additionally [ASM] features. It has a Pure-Java API and a
Scala-Friendly API to define the custom instrumentation in a declarative manner.

### How to use the Agent API?

The agent is able to modify the code during the runtime of a Java application. It uses [ByteBuddy] as a facility
library to manipulate the desirable classes and exposes an API to define the transformation to be made.

The agent supplies 2 kinds of transformation:

* Advisor: introduce changes on methods and constructors for a given type matcher.
* Mixin: compose existing types with new types in order to enrich them with new capabilities.

The API has a version for Java and other one for Scala. To define the transformations you have to extends the
`KamonInstrumentation` type (picking the Java or the Scala version) and call the available methods to introduce transformations:

* `forTargetType(name: String)(builder: InstrumentationDescription.Builder ⇒ InstrumentationDescription)`
* `forSubtypeOf(name: String)(builder: InstrumentationDescription.Builder ⇒ InstrumentationDescription)`

In the builder introduced in the function argument that receive the methods, you must define the transformations for a particular type matcher.

A simple use case using the Scala version:

```scala

import kamon.agent.scala.KamonInstrumentation

class ServletInstrumentation extends KamonInstrumentation {

    forSubtypeOf("javax.servlet.http.HttpServletResponse") { builder ⇒
        builder
          .withMixin(classOf[HttpServletResponseMixin])
          .withAdvisorFor(SetStatusMethod, classOf[ResponseStatusAdvisor])
          .withAdvisorFor(SendErrorMethod, classOf[ResponseStatusAdvisor])
          .build()
      }
    }
    
```

[ByteBuddy]:(http://bytebuddy.net/#/)
[ASM]:(http://asm.ow2.org/)
