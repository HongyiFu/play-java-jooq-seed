name := """play-java-jooq-seed"""
version := "1.0"
scalaVersion := "2.13.2"

lazy val root = (project in file("."))
  .enablePlugins(PlayJava)
  .enablePlugins(JooqCodegenPlugin)
  // LauncherJarPlugin is a solution to a Windows-only issue. mac and Linux users can comment away this
  // See https://github.com/playframework/playframework/issues/2854
  // Solution from https://www.scala-sbt.org/sbt-native-packager/recipes/longclasspath.html
  .enablePlugins(LauncherJarPlugin)

libraryDependencies ++= Seq(
  guice,
  javaJdbc,
  javaWs,
  "org.postgresql" % "postgresql" % "42.2.13",
  "org.postgresql" % "postgresql" % "42.2.13" % JooqCodegen,
  // If you have configured jOOQ to display JPA annotations in jOOQ record classes, you would need JPA.
  // Personally I prefer to have it because it shows nullable columns.
  "javax.persistence" % "javax.persistence-api" % "2.2",
)

Test / testOptions += Tests.Argument(TestFrameworks.JUnit, "-a", "-v")

ThisBuild / scalacOptions ++= Seq("-encoding", "utf8", "-deprecation", "-feature", "-unchecked")
ThisBuild / javacOptions ++= Seq("-Xlint:unchecked", "-Xlint:deprecation")

// suppress API doc generation
Compile / doc / sources := Seq.empty
// avoid to publish the documentation artifact
Compile / packageDoc / publishArtifact := false

// ----- jOOQ settings -----

jooqVersion := "3.13.2"
jooqCodegenConfig := baseDirectory.value / "conf" / "jooq-codegen.xml"
jooqCodegenStrategy := CodegenStrategy.Never // using manual codegen

// If you want to use a custom generator, you need to specify the full name of the class in jooq-codegen.xml.
// You could hardcode this classname in the xml or use the text substitution feature.
// See https://github.com/kxbmap/sbt-jooq#jooqcodegenkeys
lazy val customJooqGeneratorClass = settingKey[String]("Full classname of custom jOOQ Generator class.")
customJooqGeneratorClass := "play.java.jooq.seed.codegen.PostfixPojoClassGeneratorStrategy"

// Then add our new settingKey
Compile / jooqCodegenKeys += customJooqGeneratorClass



