import java.util.Date
import sbtprotobuf.ProtobufPlugin

val akkaVersion = "2.4.17"
val akkaHttpVersion = "10.0.5"
val kamonVersion = "0.6.6"

lazy val commonSettings = Seq(
  homepage := Some(url("https://monsantoco.github.io/kamon-prometheus")),
  organization := "com.monsanto.arch",
  organizationHomepage := Some(url("http://engineering.monsanto.org")),
  licenses := Seq("BSD New" → url("http://opensource.org/licenses/BSD-3-Clause")),
  scalaVersion := "2.12.1",
  scalacOptions ++= Seq(
    "-deprecation",
    "-unchecked",
    "-encoding", "UTF-8"
  ),
  resolvers += Resolver.jcenterRepo,
  apiMappingsScala ++= Map(
    ("com.typesafe.akka", "akka-actor") → "http://doc.akka.io/api/akka/%s",
    ("com.typesafe.akka", "akka-http-experimental") → "http://doc.akka.io/api/akka/%s"
  ),
  apiMappingsJava ++= Map(
    ("com.typesafe", "config") → "http://typesafehub.github.io/config/latest/api"
  )
)
publishTo := Some("Artifactory Realm" at "http://artifactory.navy.elama.ru:8081/artifactory/libs-release-local")
credentials += Credentials("Artifactory Realm", "artifactory.navy.elama.ru", "admin", "APwvYGBWnHuKC71xHkdbfsVYRCPNZMiGRhpnG")

val bintrayPublishing = Seq(

  bintrayOrganization := Some("monsanto"),
  bintrayPackageLabels := Seq("kamon", "prometheus", "metrics"),
  bintrayVcsUrl := Some("https://github.com/MonsantoCo/kamon-prometheus"),
  publishTo := {
    if (isSnapshot.value) Some("OJO Snapshots" at s"https://oss.jfrog.org/artifactory/oss-snapshot-local;build.timestamp=${new Date().getTime}")
    else publishTo.value
  },
  credentials ++= {
    List(bintrayCredentialsFile.value)
      .filter(_.exists())
      .map(f ⇒ Credentials.toDirect(Credentials(f)))
      .map(c ⇒ Credentials("Artifactory Realm", "oss.jfrog.org", c.userName, c.passwd))
  },
  bintrayReleaseOnPublish := {
    if (isSnapshot.value) false
    else bintrayReleaseOnPublish.value
  }
)

val noPublishing = Seq(
  publish := {},
  publishLocal := {},
  publishArtifact := false
)

lazy val BufferedConfigTest = config("buffered-config-test").extend(Test)
lazy val InvalidConfigTest = config("invalid-config-test").extend(Test)
val testConfigs = "test, buffered-config-test, invalid-config-test"

lazy val library = (project in file("library"))
  .configs(BufferedConfigTest, InvalidConfigTest)
  .disablePlugins(sbtassembly.AssemblyPlugin)
  .settings(
    commonSettings,
    bintrayPublishing,
    ProtobufPlugin.protobufSettings,
    inConfig(BufferedConfigTest)(Defaults.testSettings),
    inConfig(InvalidConfigTest)(Defaults.testSettings),
    name := "kamon-prometheus",
    description := "Kamon module to export metrics to Prometheus",
    libraryDependencies ++= Seq(
      "io.kamon"               %% "kamon-core"               % kamonVersion,
      "com.typesafe.akka"      %% "akka-actor"               % akkaVersion,
      "com.typesafe.akka"      %% "akka-http"                % akkaHttpVersion  % "provided",
      "com.typesafe"            % "config"                   % "1.3.1",
      "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.5"          % "provided",
      // -- testing --
      "ch.qos.logback"     % "logback-classic"   % "1.1.7"          % testConfigs,
      "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion  % "test",
      "com.typesafe.akka" %% "akka-slf4j"        % akkaVersion      % testConfigs,
      "com.typesafe.akka" %% "akka-testkit"      % akkaVersion      % "test",
      "org.scalatest"     %% "scalatest"         % "3.0.1"          % testConfigs,
      "io.kamon"          %% "kamon-akka-2.4"    % kamonVersion     % "test",
      "org.scalacheck"    %% "scalacheck"        % "1.13.4"         % "test"
    ),
    dependencyOverrides ++= Set(
      "com.typesafe.akka"      %% "akka-actor"    % akkaVersion,
      "org.scala-lang"          % "scala-library" % scalaVersion.value,
      "org.scala-lang"          % "scala-reflect" % scalaVersion.value,
      "org.scala-lang.modules" %% "scala-xml"     % "1.0.6"
    ),
    version in ProtobufPlugin.protobufConfig := "2.6.1",

    // We have to ensure that Kamon starts/stops serially
    parallelExecution in Test := false,
    // Don't count Protobuf-generated code in coverage
    coverageExcludedPackages := "com\\.monsanto\\.arch\\.kamon\\.prometheus\\.metric\\..*"
  )

lazy val ghPagesSettings =
  ghpages.settings ++
    Seq(
      git.remoteRepo := "git@github.com:MonsantoCo/kamon-prometheus.git"
    )

lazy val `kamon-prometheus` = (project in file("."))
  .disablePlugins(sbtassembly.AssemblyPlugin)
  .aggregate(library)
  .settings(
    commonSettings,
    noPublishing,
    ghPagesSettings,
    unidocSettings,
    site.settings,
    site.asciidoctorSupport(),
    site.includeScaladoc("api/snapshot"),
    UnidocKeys.unidocProjectFilter in (ScalaUnidoc, UnidocKeys.unidoc) := inAnyProject,
    site.addMappingsToSiteDir(mappings in (ScalaUnidoc, packageDoc), "api/snapshot")
  )
