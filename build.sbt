import AssemblyKeys._

import sbtassembly.Plugin._

import com.gu.SbtDistPlugin._

organization  := "com.gu"

name          := "mobile-notifications-football"

version       := "0.1"

scalaVersion := "2.11.8"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

resolvers ++= Seq(
  "Sonatype Releases" at "http://oss.sonatype.org/content/repositories/releases",
  "spray repo" at "http://repo.spray.io/",
  "Guardian GitHub Releases" at "http://guardian.github.io/maven/repo-releases",
  "Guardian GitHub Snapshots" at "http://guardian.github.io/maven/repo-snapshots",
  "Guardian Frontend Bintray" at "http://dl.bintray.com/guardian/frontend",
  "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/"
)

libraryDependencies ++= {
  val akkaV = "2.4.7"
  val sprayV = "1.3.2"
  Seq(
    "io.spray" %% "spray-can" % sprayV,
    "io.spray" %% "spray-routing" % sprayV,
    "io.spray" %% "spray-testkit" % sprayV,
    "io.spray" %% "spray-json" % sprayV,
    "com.typesafe.akka" %% "akka-agent" % akkaV,
    "com.typesafe.akka" %% "akka-actor" % akkaV,
    "com.typesafe.akka" %% "akka-testkit" % akkaV,
    "com.typesafe.akka" %% "akka-slf4j" % akkaV,
    "org.scalatest" %% "scalatest" % "2.2.6" % "test",
    "org.scalacheck" %% "scalacheck" % "1.10.0" % "test",
    "com.gu" %% "configuration" % "4.0",
    "com.gu" %% "mobile-notifications-client" % "0.5.27",
    "com.gu" %% "dynamo-db-switches" % "0.2",
    "com.gu" %% "pa-client" % "6.0.2",
    "org.scalaj" % "scalaj-time_2.10.0-M7" % "0.6",
    "ch.qos.logback" % "logback-classic" % "1.0.13",
    "com.netflix.rxjava" % "rxjava-scala" % "0.20.7",
    "com.amazonaws" % "aws-java-sdk" % "1.6.3",
    "net.databinder.dispatch" %% "dispatch-core" % "0.11.1"
  )
}

seq(assemblySettings: _*)

seq(distSettings :_*)

distFiles <+= (assembly in Compile) map { _ -> "packages/mobile-notifications-football/mobile-notifications-football.jar" }

distFiles <++= baseDirectory map { dir =>
  val deployRoot = dir / "deploy"
  (deployRoot ***) x rebase (deployRoot, "")
}

seq(Revolver.settings: _*)
