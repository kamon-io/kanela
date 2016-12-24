resolvers += "Sonatype snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"

resolvers += "Kamon Repository Snapshots" at "http://snapshots.kamon.io"

addSbtPlugin("com.github.gseitz" % "sbt-release" % "0.8.2")
addSbtPlugin("com.typesafe.sbt" % "sbt-scalariform" % "1.3.0")
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.3")
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "0.2.1")
addSbtPlugin("com.typesafe.sbt" % "sbt-pgp" % "0.8.1")
addSbtPlugin("io.kamon" % "sbt-kamon" % "0.0.1-SNAPSHOT")
