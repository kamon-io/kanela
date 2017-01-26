resolvers += Resolver.bintrayIvyRepo("kamon-io", "sbt-plugins")

addSbtPlugin("io.kamon" % "kamon-sbt-umbrella" % "0.0.14")
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.3")
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.6.1")
addSbtPlugin("com.lightbend.sbt" % "sbt-javaagent" % "0.1.2")
