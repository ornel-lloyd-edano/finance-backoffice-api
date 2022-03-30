import scalariform.formatter.preferences._
import com.typesafe.sbt.packager.docker.Cmd

resolvers ++= Seq(
  Resolver.bintrayRepo("cakesolutions", "maven")/*,
  "confluent" at "https://packages.confluent.io/maven/"*/
)

enablePlugins(DockerPlugin, JavaAppPackaging)

name := """backoffice_api"""
organization := "tech.pegb"

version := "0.0.85"


val BackOfficeIntegrationTest = IntegrationTest.extend(Test)

lazy val backoffice_api = (project in file("."))
  .enablePlugins(PlayScala, SbtWeb, BuildInfoPlugin)
  .configs(BackOfficeIntegrationTest)
  .settings(
    Defaults.itSettings,

    buildInfoKeys := Seq[BuildInfoKey](version),
    buildInfoPackage := "tech.pegb.backoffice",

    Test / fork := true,
    Test / parallelExecution := false,
    Test / javaOptions += "-Dconfig.file=conf/application.test.conf",

    BackOfficeIntegrationTest / fork := true,
    BackOfficeIntegrationTest / parallelExecution := false,
    BackOfficeIntegrationTest / javaOptions += "-Dconfig.file=conf/application.it.conf")

scalaVersion := "2.12.8"

val jacksonVersion = "2.9.6"
val avro4sVersion = "2.0.4"
val csKafkaVersion = "2.1.0"
val silencerVersion = "1.4.3"
val scalaCacheVersion = "0.27.0"

libraryDependencies ++= Seq(
  ehcache,
  evolutions,
  guice,
  jdbc,
  ws,
  "ai.x" %% "play-json-extensions" % "0.30.1",
  "com.pauldijou" %% "jwt-play-json" % "2.1.0",
  "com.typesafe.play" %% "play-json" % "2.7.1",
  "org.mariadb.jdbc" % "mariadb-java-client" % "1.6.5",
  "com.couchbase.client" % "java-client" % "2.7.3",
  "org.apache.commons" % "commons-email" % "1.5",
  "org.apache.commons" % "commons-text" % "1.6",
  "org.cvogt" %% "scala-extensions" % "0.5.3",
  "org.playframework.anorm" %% "anorm" % "2.6.2",
  "org.typelevel" %% "cats-core" % "1.6.0",
  "com.sauldhernandez" %% "autoschema" % "1.0.4",
  "org.julienrf" %% "play-json-derived-codecs" % "6.0.0",

// For greenplum
  "postgresql" % "postgresql" % "8.3-603.jdbc4",

  "net.cakesolutions" %% "scala-kafka-client-akka" % csKafkaVersion,
  //"io.confluent" % "kafka-avro-serializer" % "5.1.0",
  "com.sksamuel.avro4s" %% "avro4s-kafka" % avro4sVersion,

  "io.swagger" %% "swagger-play2" % "1.6.0",
  "io.swagger" % "swagger-core" % "1.5.21", // explicit forcing of the most version
  "io.swagger" %% "swagger-scala-module" % "1.0.4", // explicit forcing of the most version
  "org.webjars" %% "webjars-play" % "2.7.0-1",
  "org.webjars" % "swagger-ui" % "3.19.0",
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % jacksonVersion, // tmp forcing of the most version
  "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310" % jacksonVersion,

  "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % "it,test",
  "org.scalamock" %% "scalamock" % "4.1.0" % Test,
  "com.h2database" % "h2" % "1.4.197" % Test,
  "net.cakesolutions" %% "scala-kafka-client-testkit" % csKafkaVersion % "it,test",
  "io.github.azhur" %% "kafka-serde-play-json" % "0.4.0",
  "io.github.embeddedkafka" %% "embedded-kafka" % "2.3.0" % "test",
  // hadoop dependencies
  "org.apache.hadoop" % "hadoop-client" % "2.7.0" exclude("org.slf4j", "slf4j-log4j12"),
 
  //"org.slf4j" % "log4j-over-slf4j" % "1.7.21" % Test
  // sangria + graphql
  "org.sangria-graphql" %% "sangria" % "1.4.2",
  "org.sangria-graphql" %% "sangria-relay" % "1.4.2",
  "org.sangria-graphql" %% "sangria-circe" % "1.2.1",
  "org.sangria-graphql" %% "sangria-play-json" % "1.0.5",
  "org.sangria-graphql" %% "sangria-slowlog" % "0.1.8",
  "io.circe" %% "circe-core" % "0.10.0",
  "io.circe" %% "circe-parser" % "0.10.0",

  "commons-codec" % "commons-codec" % "1.11",
  "com.github.cb372" %% "scalacache-caffeine" % scalaCacheVersion,
  "com.github.cb372" %% "scalacache-cats-effect" % scalaCacheVersion,

  compilerPlugin("com.github.ghik" % "silencer-plugin" % silencerVersion cross CrossVersion.full),
  "com.github.ghik" % "silencer-lib" % silencerVersion % Provided cross CrossVersion.full
)

dependencyOverrides += "commons-codec" % "commons-codec" % "1.11"

scalacOptions in ThisBuild ++= Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-explaintypes",
  "-feature",
  "-language:higherKinds",
  "-unchecked",
  "-Xlint",
  "-Ypartial-unification")

dockerBaseImage := "openjdk:11-jre-slim"
dockerExposedPorts := Seq(9000)
Docker / daemonUser := "nobody"
Docker / daemonGroup := "root"
Docker / packageName := "192.168.36.102:5000/backoffice_api"
Docker / dockerCommands ++= Seq(Cmd("RUN", "chmod -R 777 /opt/docker"))

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "tech.pegb.controllers._"

// Adds additional packages into conf/routes
play.sbt.routes.RoutesKeys.routesImport ++= Seq(
  //"tech.pegb.backoffice.api.auth.dto.implicits.PathBinders._",
  "tech.pegb.backoffice.api.QueryBinders._"
)

// silence all warnings on autogenerated files
scalacOptions += "-P:silencer:pathFilters=target/.*"
// Make sure you only exclude warnings for the project directories, i.e. make builds reproducible
scalacOptions += s"-P:silencer:sourceRoots=${baseDirectory.value.getCanonicalPath}"

scalariformPreferences := scalariformPreferences.value
  .setPreference(DanglingCloseParenthesis, Prevent)
  .setPreference(RewriteArrowSymbols, true) // allows not to force this option in IDE
  .setPreference(SpacesAroundMultiImports, false)
  .setPreference(DoubleIndentConstructorArguments, true) // http://docs.scala-lang.org/style/declarations.html#classes
  .setPreference(NewlineAtEndOfFile, true)

coverageExcludedPackages := "<empty>;Reverse.*;router\\.*"
coverageMinimum := 86
coverageFailOnMinimum := false

PlayKeys.devSettings := Seq(
  "config.resource" -> "development.conf"
)
