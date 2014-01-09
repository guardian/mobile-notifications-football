import sbt._

import sbt._

object Plugins extends Build {
  lazy val plugins = Project("plugins", file("."))
    .dependsOn(
      uri("git://github.com/guardian/sbt-dist-plugin.git#1.7")
    )
}
