package dbcodegen.plugin

import sbt.{io as _, *}
import sbt.Keys.*
import dbcodegen.*
import us.fatehi.utility.datasource.DatabaseConnectionSource

import java.io.File
import java.sql.{Connection, SQLType}
import scala.util.Using

object DbCodegenPlugin extends AutoPlugin {
  override def trigger = noTrigger

  object autoImport {
    val dbcodegenSetupTask =
      taskKey[Db => Unit]("Setup task to be executed before the code generation runs against the database")
    val dbcodegenJdbcUrl =
      settingKey[String]("The jdbc URL for the database")
    val dbcodegenTemplateFiles =
      settingKey[Seq[File]]("The file path to the schema code generation template")
    val dbcodegenTypeMapping =
      settingKey[(SQLType, Option[String]) => Option[String]]("Map jdbc types to java/scala types")
    val dbcodegenSchemaTableFilter =
      settingKey[(String, String) => Boolean]("Filter which schema and table should be processed")
    val dbcodegenScalafmt =
      settingKey[Boolean]("Whether to run scalafmt on the generated code")
    val dbcodegenUsername =
      settingKey[Option[String]]("Optional database username")
    val dbcodegenPassword =
      settingKey[Option[String]]("Optional database password")
  }
  import autoImport._

  override lazy val projectSettings: Seq[Def.Setting[_]] = Seq(
    dbcodegenSetupTask         := { _ => () },
    dbcodegenTemplateFiles     := Seq.empty,
    dbcodegenTypeMapping       := ((_, tpe) => tpe),
    dbcodegenSchemaTableFilter := ((_, _) => true),
    dbcodegenScalafmt          := true,
    dbcodegenUsername          := None,
    dbcodegenPassword          := None,
    (Compile / sourceGenerators) += Def.task {
      val outDir = (Compile / sourceManaged).value / "scala" / "dbcodegen"

      val dbConfig = DbConfig(
        jdbcUrl = dbcodegenJdbcUrl.value,
        username = dbcodegenUsername.value,
        password = dbcodegenPassword.value,
      )

      val codeGeneratorConfig = CodeGeneratorConfig(
        templateFiles = dbcodegenTemplateFiles.value,
        outDir = outDir,
        typeMapping = dbcodegenTypeMapping.value,
        schemaTableFilter = dbcodegenSchemaTableFilter.value,
        scalafmt = dbcodegenScalafmt.value,
        scalaVersion = scalaVersion.value,
      )

      val setupTask = dbcodegenSetupTask.value
      Using.resource(DbConnection.getSource(dbConfig)) { connectionSource =>
        setupTask(Db(connectionSource))

        val generatedFiles = CodeGenerator.generate(connectionSource, codeGeneratorConfig)

        generatedFiles.map(_.toFile)
      }
    }.taskValue,
  )
}

trait Db {
  def connection: Connection
  def executeSql(sql: String): Unit
  def executeSqlFile(file: File): Unit
}
object Db {
  def apply(source: DatabaseConnectionSource): Db = new Db {
    lazy val connection                  = source.get()
    def executeSql(sql: String): Unit    = SqlExecutor.executeSql(connection, sql)
    def executeSqlFile(file: File): Unit = SqlExecutor.executeSqlFile(connection, file)
  }
}
