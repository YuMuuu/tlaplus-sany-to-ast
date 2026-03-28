import sbt.Keys.libraryDependencies
import scala.collection.Seq


val scala3Version = "3.8.2"
lazy val dispatchVersion = "2.0.0"


lazy val root = project
  .in(file("."))
  .enablePlugins(ScalaxbPlugin)
  .settings(
    name := "tlaplus-sany-to-ast",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := scala3Version,
    libraryDependencies ++= Seq(
      "org.scala-lang.modules" %% "scala-xml" % "2.4.0",
      "org.scala-lang.modules" %% "scala-parser-combinators" % "2.4.0",
      "org.dispatchhttp" %% "dispatch-core" % dispatchVersion,
      "javax.xml.bind" % "jaxb-api" % "2.3.0",
      ("org.lamport" % "tla2tools" % "1.8.0")
        .intransitive()
        .from(
          "https://github.com/tlaplus/tlaplus/releases/download/v1.8.0/tla2tools.jar"
        )
    ),
    Compile / scalaxb / scalaxbPackageName := "generated",
    Compile / scalaxb / scalaxbDispatchVersion := dispatchVersion,

    Compile / scalaxb := (Compile / scalaxb).dependsOn(downloadSanyXsd).value,

    //work around:
    // tlaplusのxsdのvalueに\が入っていてgenerate結果が不正になコードになる
    // 暫定対応として生成後 のファイルをエスケープするように書き換える
    Compile / sourceGenerators += Def.task {
      val _ = (Compile / scalaxb).value

      val dir = (Compile / sourceManaged).value / "sbt-scalaxb" / "generated"
      val files = Seq(dir / "sany.scala", dir / "xmlprotocol.scala").filter(_.exists)

      val replacements = Seq(
        "= \"\\lnot\""       -> "= \"\\\\lnot\"",
        "= \"\\in\""         -> "= \"\\\\in\"",
        "= \"\\union\""      -> "= \"\\\\union\"",
        "= \"\\intersect\""  -> "= \"\\\\intersect\"",
        "= \"\\subseteq\""   -> "= \"\\\\subseteq\"",
        "= \"\\equiv\""      -> "= \"\\\\equiv\"",
        "= \"\\land\""       -> "= \"\\\\land\"",
        "= \"\\lor\""        -> "= \"\\\\lor\"",
        "= \"\\cdot\""       -> "= \"\\\\cdot\"",
        "= \"\\\""           -> "= \"\\\\\"",
        "scala.xml.Text(\"\\lnot\")"      -> "scala.xml.Text(\"\\\\lnot\")",
        "scala.xml.Text(\"\\in\")"        -> "scala.xml.Text(\"\\\\in\")",
        "scala.xml.Text(\"\\union\")"     -> "scala.xml.Text(\"\\\\union\")",
        "scala.xml.Text(\"\\intersect\")" -> "scala.xml.Text(\"\\\\intersect\")",
        "scala.xml.Text(\"\\subseteq\")"  -> "scala.xml.Text(\"\\\\subseteq\")",
        "scala.xml.Text(\"\\equiv\")"     -> "scala.xml.Text(\"\\\\equiv\")",
        "scala.xml.Text(\"\\land\")"      -> "scala.xml.Text(\"\\\\land\")",
        "scala.xml.Text(\"\\lor\")"       -> "scala.xml.Text(\"\\\\lor\")",
        "scala.xml.Text(\"\\cdot\")"      -> "scala.xml.Text(\"\\\\cdot\")",
        "scala.xml.Text(\"\\\")"          -> "scala.xml.Text(\"\\\\\")"
      )

      val replacements2 = Seq(
        """case class Modules(RootModule: String,
          |  context: generated.Context,
          |  modules: Seq[generated.Modules] = Nil)""".stripMargin ->
          """case class Modules(RootModule: String,
            |  context: generated.Context,
            |  modules: Seq[generated.Modules with scalaxb.DataRecord[generated.ModulesOption]] = Nil
            |)""".stripMargin,
        """case class ModuleNode(nodeSequence1: generated.NodeSequence,
          |  uniquename: String,
          |  extendsValue: generated.Extends,
          |  modulenode: Seq[generated.ModuleNode] = Nil) extends ModulesOption with EntryOption""".stripMargin ->
        """case class ModuleNode(nodeSequence1: generated.NodeSequence,
          |  uniquename: String,
          |  extendsValue: generated.Extends,
          |  modulenode: Seq[scalaxb.DataRecord[ ArgumentOption with OperatorOption with ModuleNodeOption with generated.OpDefNodeRefOption] = Nil) extends ModulesOption with EntryOption""".stripMargin

      )

      val replacements3 = Seq(
        "__obj.modules flatMap { scalaxb.toXML[generated.Modules](_, None, Some(\"modules\"), __scope, false) })" ->
        "__obj.modules flatMap {\n          case x: generated.Modules => scalaxb.toXML[generated.Modules](x, None, Some(\"modules\"), __scope, false)\n          case x: scalaxb.DataRecord[generated.ModulesOption] => scalaxb.toXML[scalaxb.DataRecord[generated.ModulesOption]](x, x.namespace, x.key, __scope, false)\n        })",
        "__obj.modulenode flatMap { scalaxb.toXML[generated.ModuleNode](_, None, Some(\"modulenode\"), __scope, false) })" ->
        "__obj.modulenode flatMap { x => scalaxb.toXML[scalaxb.DataRecord[Any]](x, x.namespace, x.key, __scope, false) } )"
      )

      files.foreach { f =>
        val s0 = IO.read(f)
      val s1 = (replacements ++ replacements2 ++ replacements3).foldLeft(s0) { case (acc, (from, to)) =>
        acc.replace(from, to)
      }
        if (s0 != s1) IO.write(f, s1)
      }

      files
    }.taskValue
  )

lazy val downloadSanyXsd = taskKey[Unit]("sany.xsdをdownloadする")

downloadSanyXsd := {
  import scala.sys.process.*
  val out = baseDirectory.value.absolutePath + "/src/main/xsd/sany.xsd"
  println(s"output path: ${out}")
  val cmd = Seq(
    "curl",
    "-Lo",
    out,
    "https://raw.githubusercontent.com/tlaplus/tlaplus/refs/tags/v1.8.0/tlatools/org.lamport.tlatools/src/tla2sany/xml/sany.xsd"
  )
  Process(cmd).!
}
