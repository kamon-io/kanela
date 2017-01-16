# Kamon Agent

The `kamon-agent` is developed in order to provide a simple way to implement an application running on the JVM and
introduce kamon features such as, creation of traces, metric measures, trace propagation, and so on.

It's a simple Java Agent written in Java 8 and is powered by [ByteBuddy] and some additionally [ASM] features. It has a Pure-Java API and a
Scala-Friendly API to define the custom instrumentation in a declarative manner.

### How to use the Agent API?

The agent is able to modify the code during the runtime of a Java application. It uses [ByteBuddy] as a facility
library to manipulate the desirable classes and exposes an API to define the transformation to be made.

The agent provides 2 kinds of transformation:

* **Advisor:** introduce changes on methods and constructors for a given type matcher.
* **Mixin:** compose existing types with new types in order to enrich them with new capabilities.

The API has a version for *Java* and other one for *Scala*. To define the transformations you have to extends the
`KamonInstrumentation` type (picking the Java or the Scala version) and call the available methods to introduce transformations:

* `forTargetType(name: String)(builder: InstrumentationDescription.Builder ⇒ InstrumentationDescription)`
* `forSubtypeOf(name: String)(builder: InstrumentationDescription.Builder ⇒ InstrumentationDescription)`

In the builder introduced in the function argument, you must define the transformations for a particular type matcher,
as shown in the following example.

A simple use case using the Scala version:

```scala

import kamon.agent.scala.KamonInstrumentation

// And other imports !

class ServletInstrumentation extends KamonInstrumentation {

    val SetStatusMethod: Junction[MethodDescription] = named("setStatus")
    val SendErrorMethod: Junction[MethodDescription] = named("sendError").and(takesArguments(classOf[Int]))

    forSubtypeOf("javax.servlet.http.HttpServletResponse") { builder ⇒
        builder
          .withMixin(classOf[HttpServletResponseMixin])
          .withAdvisorFor(SetStatusMethod, classOf[ResponseStatusAdvisor])
          .withAdvisorFor(SendErrorMethod, classOf[ResponseStatusAdvisor])
          .build()
    }
}

class ResponseStatusAdvisor
object ResponseStatusAdvisor {
  @OnMethodEnter
  def onEnter(@This response: TraceContextAwareExtension, @Argument(0) status: Int): Unit = {
    response.traceContext().collect { ctx ⇒
      ServletExtension.httpServerMetrics.recordResponse(ctx.name, status.toString)
    }
  }
}

```
## Lombok
This project uses [Lombok](https://projectlombok.org/) to reduce boilerplate. You can setup
 the [IntelliJ plugin](https://plugins.jetbrains.com/plugin/6317) to add IDE support. 
 
## License

This software is licensed under the Apache 2 license, quoted below.

Copyright © 2013-2017 the kamon project <http://kamon.io>

Licensed under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License. You may obtain a copy of
the License at

    [http://www.apache.org/licenses/LICENSE-2.0]

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations under
the License.

[ByteBuddy]:(http://bytebuddy.net/#/)
[ASM]:(http://asm.ow2.org/)
