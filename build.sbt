organization  := "com.gu.mobile.notifications.football"

version       := "0.1"

scalaVersion  := "2.10.3"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

resolvers ++= Seq(
  "spray repo" at "http://repo.spray.io/",
  "Guardian GitHub Releases" at "http://guardian.github.io/maven/repo-releases"
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
    "com.gu"              %%  "management-internal" % "5.32",
    "com.gu"              %%  "configuration" % "3.10",
    "com.gu"              %%  "mobile-notifications-client" % "0.1",
    "com.gu"              %%  "pa-client" % "4.0" 
  )
}

seq(Revolver.settings: _*)
