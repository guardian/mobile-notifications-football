organization  := "com.gu.mobile.notifications"

version       := "0.1"

scalaVersion  := "2.10.3"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

resolvers ++= Seq(
  "Sonatype Releases" at "http://oss.sonatype.org/content/repositories/releases",
  "spray repo" at "http://repo.spray.io/",
  "Guardian GitHub Releases" at "http://guardian.github.io/maven/repo-releases",
  "Guardian GitHub Snapshots" at "http://guardian.github.io/maven/repo-snapshots"
)

libraryDependencies ++= {
  val akkaV = "2.2.3"
  val sprayV = "1.2.0"
  Seq(
    "io.spray"            %   "spray-can"     % sprayV,
    "io.spray"            %   "spray-routing" % sprayV,
    "io.spray"            %   "spray-testkit" % sprayV,
    "com.typesafe.akka"   %%  "akka-actor"    % akkaV,
    "com.typesafe.akka"   %%  "akka-testkit"  % akkaV,
    "org.specs2"          %%  "specs2"        % "2.2.3" % "test",
    "org.scalacheck"      %%  "scalacheck"    % "1.10.0" % "test",
    "com.gu"              %%  "management-internal" % "5.32",
    "com.gu"              %%  "configuration" % "3.10",
    "com.gu"              %%  "mobile-notifications-client" % "0.1-SNAPSHOT",
    "com.gu"              %%  "pa-client"     % "4.0"
  )
}

seq(Revolver.settings: _*)
