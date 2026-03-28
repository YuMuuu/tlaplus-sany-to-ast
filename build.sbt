import sbt.Keys.libraryDependencies

import scala.collection.Seq

val scala3Version = "3.8.2"

lazy val root = project
  .in(file("."))
  .settings(
    name := "tlaplus-sany-to-ast",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := scala3Version,
    libraryDependencies ++= Seq(
      ("org.lamport" % "tla2tools" % "1.8.0")
        .intransitive()
        .from(
          "https://github.com/tlaplus/tlaplus/releases/download/v1.8.0/tla2tools.jar"
        ),
      "org.scala-lang.modules" %% "scala-xml" % "2.2.0"
    )
  )

lazy val downloadSanyXsd = taskKey[Unit]("sany.xsdをdownloadする")

downloadSanyXsd := {
  import scala.sys.process.*
  val out = baseDirectory.value.absolutePath + "/src/main/resources/sany.xsd"
  println(s"output path: ${out}")
  val cmd = Seq(
    "curl",
    "-Lo",
    out,
    "https://raw.githubusercontent.com/tlaplus/tlaplus/refs/tags/v1.8.0/tlatools/org.lamport.tlatools/src/tla2sany/xml/sany.xsd"
  )
  Process(cmd).!
}
