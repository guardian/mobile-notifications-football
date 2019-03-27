name := "football"

version := "1.0"

scalaVersion := "2.12.8"

resolvers ++= Seq(
  "Guardian Platform Bintray" at "https://dl.bintray.com/guardian/platforms",
  "Guardian Frontend Bintray" at "https://dl.bintray.com/guardian/frontend",
  "Guardian Mobile Bintray" at "https://dl.bintray.com/guardian/mobile"
)

libraryDependencies ++= Seq(
  "org.slf4j" % "slf4j-simple" % "1.7.25",
  "com.typesafe" % "config" % "1.3.2",
  "com.gu" %% "mobile-notifications-client" % "1.5",
  "com.gu" %% "pa-client" % "6.1.0",
  "com.gu" %% "scanamo" % "0.8.1",
  "com.gu" %% "content-api-client-default" % "12.18",
  "com.gu" %% "simple-configuration-ssm" % "1.4.1",
  "com.amazonaws" % "aws-java-sdk-dynamodb" % "1.11.60",
  "com.amazonaws" % "aws-lambda-java-core" % "1.1.0",
  "com.squareup.okhttp3" % "okhttp" % "3.8.1",
  "com.google.code.findbugs" % "jsr305" % "3.0.2",
  "org.specs2" %% "specs2-core" % "4.5.1" % "test",
  "org.specs2" %% "specs2-mock" % "4.5.1" % "test",
  "org.typelevel" %% "cats" % "0.8.1"
)

enablePlugins(RiffRaffArtifact)

assemblyJarName := s"${name.value}.jar"
riffRaffPackageType := assembly.value
riffRaffManifestProjectName := s"mobile-notifications:${name.value}"
riffRaffUploadArtifactBucket := Option("riffraff-artifact")
riffRaffUploadManifestBucket := Option("riffraff-builds")
riffRaffArtifactResources += (file("cfn.yaml"), s"${name.value}-cfn/cfn.yaml")

assemblyMergeStrategy in assembly := {
  case PathList(ps @ _*) if ps.last endsWith ".thrift" => MergeStrategy.discard
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}