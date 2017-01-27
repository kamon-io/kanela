# Kamon Agent

The `kamon-agent` is developed in order to provide a simple way to instrument an application running on the JVM and
introduce kamon features such as, creation of traces, metric measures, trace propagation, and so on.

It's a simple Java Agent written in Java 8 and powered by [ByteBuddy] with some additionally [ASM] features. It has a Pure-Java API and a
Scala-Friendly API to define the custom instrumentation in a declarative manner.

Kamon has several module that need to instrument the app to introduce itself in the internal components. Introducing this Agent,
you have other way to instrument your app / library /framework through a simple and declarative API and get additional features such as
retransformation of the loaded classes (so it's possible to attach agent on the runtime), revoke the instrumentation
when the app is in a critical state, and so on.

### How to use the Agent API?

The agent provides 3 kinds of transformation:

* **Advisor:** introduce changes on methods and constructors for a given type matcher.
* **Interceptor:** intercept a method execution, introducing a proxy method in the middle.
* **Mixin:** compose existing types with new types in order to enrich them with new capabilities.

The API has a version for *Java* and other one for *Scala*. To define the transformations you have to extends the
`KamonInstrumentation` type (picking the Java or the Scala version) and define a new module in the configuration, as you can see
in the following example.

## Example

Suppose you have a simple worker that perform a simple operation:

```scala
import scala.util.Random

case class Worker() {
  def performTask(): Unit = Thread.sleep((Random.self.nextFloat() * 500) toLong)
}
```

You might want to mixin it with a type that provide a way to accumulate metrics, such as the following:

```scala
trait MonitorAware {
  def execTimings: Map[String, Vector[Long]]
  def addExecTimings(methodName: String, time: Long): Vector[Long]
}
```

And introduce some transformations in order to modify the bytecode and hook into the internal app.

```scala

import kamon.agent.scala.KamonInstrumentation

// And other imports !

class MonitorInstrumentation extends KamonInstrumentation {

  forTargetType("app.kamon.Worker") { builder ⇒
    builder
      .withMixin(classOf[MonitorMixin])
      .withAdvisorFor(named("performTask"), classOf[WorkerAdvisor])
      .build()
  }
}


class MonitorMixin extends MonitorAware {
  import collection.JavaConverters._

  private var _execTimings: ConcurrentMap[String, Vector[Long]] = _

  def execTimings: Map[String, Vector[Long]] = this._execTimings.asScala.toMap
  def addExecTimings(methodName: String, time: Long): Vector[Long] = {
    this._execTimings.compute(methodName, (_, oldValues) ⇒ Option(oldValues).map(_ :+ time).getOrElse(Vector(time)))
  }

  @Initializer
  def init(): Unit = this._execTimings = new ConcurrentHashMap()

}

class WorkerAdvisor
object WorkerAdvisor {

  @OnMethodEnter
  def onMethodEnter(): Long = {
    System.nanoTime() // Return current time, entering as parameter in the onMethodExist
  }

  @OnMethodExit
  def onMethodExit(@This instance: MonitorAware, @Enter start: Long, @Origin origin: String): Unit = {
    val timing = System.nanoTime() - start
    instance.addExecTimings(origin, timing)
    println(s"Method $origin was executed in $timing ns.")
  }
}

```

Finally, we need to define a new module in the kamon agent configuration:

```hocon
kamon.agent {
  modules {
    example-module {
      name = "Example Module"
      stoppable = false
      instrumentations = [
        "app.kamon.instrumentation.MonitorInstrumentation"
      ]
      within = [ "app.kamon\\..*" ] // List of patterns to match the types to instrument.
    }
  }
}
```

Some other configuration that you can define is indicated in the agent [`reference.conf`](https://github.com/kamon-io/kamon-agent/blob/master/agent/src/main/resources/reference.conf)

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
