import kamon.sbt.plugin.SbtKamon
import sbt.Keys._

object Agent {

  lazy val settings = SbtKamon.compileSettings ++ Seq(
    fork  := true,
    javaOptions <++= SbtKamon.sbtKamonKeys.kamonRunnerOptions in SbtKamon.Runner
  )
}