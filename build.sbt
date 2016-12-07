name := "mobile-notifications-football-lambda"

version := "1.0"

scalaVersion := "2.11.8"

libraryDependencies ++= {
  val akkaV = "2.4.14"
  val sprayV = "1.3.2"
  Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaV,
    "com.typesafe.akka" %% "akka-slf4j" % akkaV,
    "org.clapper" % "grizzled-slf4j_2.10" % "1.3.0",
    "com.gu" %% "mobile-notifications-client" % "0.5.29",
    "com.gu" %% "pa-client" % "6.0.2",
    "com.gu" %% "scanamo" % "0.8.1",
    "ch.qos.logback" % "logback-classic" % "1.0.13",
    "com.amazonaws" % "aws-java-sdk-dynamodb" % "1.11.60",
    "com.amazonaws" % "aws-lambda-java-core" % "1.1.0",
    "net.databinder.dispatch" %% "dispatch-core" % "0.11.1",
    "org.scalatest" %% "scalatest" % "2.2.6" % "test",
    "org.scalacheck" %% "scalacheck" % "1.10.0" % "test",
    "com.typesafe.akka" %% "akka-testkit" % akkaV % "test",
    "org.typelevel" %% "cats" % "0.8.1"
  )
}

enablePlugins(RiffRaffArtifact)

assemblyJarName := s"${name.value}.jar"
riffRaffPackageType := assembly.value
riffRaffUploadArtifactBucket := Option("riffraff-artifact")
riffRaffUploadManifestBucket := Option("riffraff-builds")
riffRaffArtifactResources += (file("cfn.yaml"), s"${name.value}-cfn/cfn.yaml")