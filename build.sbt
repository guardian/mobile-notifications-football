name := "football"

version := "1.0"

scalaVersion := "2.11.8"

libraryDependencies ++= {
  val akkaV = "2.4.14"
  val sprayV = "1.3.2"
  Seq(
    "org.slf4j" % "slf4j-simple" % "1.7.25",
    "com.gu" %% "mobile-notifications-client" % "0.5.29",
    "com.gu" %% "pa-client" % "6.0.2",
    "com.gu" %% "scanamo" % "0.8.1",
    "com.amazonaws" % "aws-java-sdk-dynamodb" % "1.11.60",
    "com.amazonaws" % "aws-lambda-java-core" % "1.1.0",
    "com.squareup.okhttp3" % "okhttp" % "3.8.1",
    "com.google.code.findbugs" % "jsr305" % "3.0.2",
    "org.specs2" %% "specs2-core" % "3.8.5" % "test",
    "com.typesafe.akka" %% "akka-testkit" % akkaV % "test",
    "org.typelevel" %% "cats" % "0.8.1"
  )
}

enablePlugins(RiffRaffArtifact)

assemblyJarName := s"${name.value}.jar"
riffRaffPackageType := assembly.value
riffRaffManifestProjectName := s"mobile-notifications:${name.value}"
riffRaffUploadArtifactBucket := Option("riffraff-artifact")
riffRaffUploadManifestBucket := Option("riffraff-builds")
riffRaffArtifactResources += (file("cfn.yaml"), s"${name.value}-cfn/cfn.yaml")