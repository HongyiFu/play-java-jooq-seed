import metabuild.JooqCodegenBuildInfo._

name := """play-java-jooq-seed"""
version := "1.0"
scalaVersion := "2.13.0"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

lazy val jdbcDriverModuleId: ModuleID = {
  val parts = jdbcDriverStr.split(":")
  parts(0) % parts(1) % parts(2)
}

libraryDependencies ++= Seq(
  guice,
  javaJdbc,
  jdbcDriverModuleId,
  "org.jooq" % "jooq" % jooqVersion,
  // If you have configured jOOQ to display JPA annotations in jOOQ record classes, you need JPA.
  "javax.persistence" % "javax.persistence-api" % "2.2",
)

Test / testOptions += Tests.Argument(TestFrameworks.JUnit, "-a", "-v")

ThisBuild / scalacOptions ++= Seq("-encoding", "utf8", "-deprecation", "-feature", "-unchecked")
ThisBuild / javacOptions ++= Seq("-Xlint:unchecked", "-Xlint:deprecation")

// ----------  jOOQ settings  ----------

lazy val jooqConfigFile = settingKey[File]("jOOQ codegen configuration file location")
jooqConfigFile := baseDirectory.value / "conf" / "jooq-codegen.xml"

lazy val jooqConfig = taskKey[org.jooq.meta.jaxb.Configuration]("jOOQ codegen configuration class")
jooqConfig := {
  autoClose(new java.io.FileInputStream(jooqConfigFile.value)) { in =>
    org.jooq.codegen.GenerationTool.load(in)
  }
}

// You can add more custom behaviors to this task to emulate the jooqCodegenStrategy setting (ALWAYS/ IfAbsent/ Never) of sbt-jooq plugin
lazy val jooqGenFiles = taskKey[Seq[File]]("jOOQ generated files")
jooqGenFiles := {
  val config = jooqConfig.value
  val targetDirStr = config.getGenerator.getTarget.getDirectory
  var targetDir = file(targetDirStr)
  if (!targetDir.isAbsolute) {
    targetDir = baseDirectory.value / targetDirStr
  }
  val packageDir = config.getGenerator.getTarget.getPackageName.replace('.', '/')
  val finder: PathFinder = ( (targetDir / packageDir) ** "*" ).filter(_.isFile)
  finder.get
}

lazy val jooqCodegen = taskKey[Unit]("Run jOOQ code generation")
jooqCodegen := {
  val log = streams.value.log
  log.info(f"jOOQ config file is: ${jooqConfigFile.value} %nRunning jOOQ codegen..")

  // run main method of jOOQ GenerationTool
  org.jooq.codegen.GenerationTool.generate(jooqConfig.value)
}

// We need to add the jOOQ generated files to sourceGenerators.
// See - https://stackoverflow.com/a/53468356/8795412 and https://www.scala-sbt.org/release/docs/Howto-Generating-Files.html#Generate+sources
Compile / sourceGenerators += jooqGenFiles.taskValue

// Implement Java try-with-resource in Scala, from https://stackoverflow.com/a/39868021/8795412
def autoClose[A <: AutoCloseable, B](closeable: A)(fun: A => B): B = {
  var t: Throwable = null
  try {
    fun(closeable)
  } catch {
    case funT: Throwable ⇒
      t = funT
      throw t
  } finally {
    if (t != null) {
      try {
        closeable.close()
      } catch {
        case closeT: Throwable ⇒
          t.addSuppressed(closeT)
          throw t
      }
    } else {
      closeable.close()
    }
  }
}
