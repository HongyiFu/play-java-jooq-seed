// The Play plugin
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.7.3")

val jooqVersion = "3.12.1"
val jdbcDriver: ModuleID = "mysql" % "mysql-connector-java" % "8.0.17"

// Settings to share the jooqVersion + jdbcDriver across project build and metabuild
// See - https://stackoverflow.com/questions/23944108
enablePlugins(BuildInfoPlugin)

buildInfoKeys := Seq[BuildInfoKey](
  "jooqVersion" -> jooqVersion,
  "jdbcDriverStr" -> jdbcDriver.toString()
)
buildInfoPackage := "metabuild"
buildInfoObject := "JooqCodegenBuildInfo"

libraryDependencies ++= Seq(
  "org.jooq" % "jooq-codegen" % jooqVersion,
  jdbcDriver
)

