resolvers += "Kamon Repository Snapshots" at "http://snapshots.kamon.io"

resolvers += Resolver.bintrayIvyRepo("kamon-io", "sbt-plugins")

addSbtPlugin("io.kamon" % "kamon-sbt-umbrella" % "0.0.14")
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.3")
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.6.1")
addSbtPlugin("io.kamon" % "sbt-kamon" % "0.0.1-SNAPSHOT")
