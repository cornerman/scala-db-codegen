package dbcodegen

import mainargs.{arg, main, ParserForMethods}

import java.io.File
import scala.util.Using

object Main {
  @main
  def run(
    @arg(doc = "The jdbc URL for the database")
    jdbcUrl: String,
    @arg(doc = "Output path for the generated code")
    outDir: String,
    @arg(doc = "The template file for the code generator")
    templateFile: Seq[String],
    @arg(doc = "Which database schemas to process")
    schema: Seq[String],
    @arg(doc = "Which database tables to process")
    table: Seq[String],
    @arg(doc = "Scala version to format the code with scalafmt")
    scalaVersion: Option[String],
    @arg(doc = "Optional database username")
    username: Option[String],
    @arg(doc = "Optional database password")
    password: Option[String],
  ) = {
    val dbConfig = DbConfig(
      jdbcUrl = jdbcUrl,
      username = username,
      password = password,
    )

    val codeGeneratorConfig = CodeGeneratorConfig(
      templateFiles = templateFile.map(s => new File(s)),
      outDir = new File(outDir),
      typeMapping = (_, tpe) => tpe,
      schemaTableFilter =
        (schemaName, tableName) => (schema.isEmpty || schema.contains(schemaName)) && (table.isEmpty || table.contains(tableName)),
      scalafmt = scalaVersion.isDefined,
      scalaVersion = scalaVersion.getOrElse("3.0.0"),
    )

    Using.resource(DbConnection.getSource(dbConfig)) { connectionSource =>
      val _ = CodeGenerator.generate(connectionSource, codeGeneratorConfig)
    }
  }

  def main(args: Array[String]): Unit = {
    val _ = ParserForMethods(this).runOrExit(args.toSeq)
  }
}
