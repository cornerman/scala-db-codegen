package dbcodegen.plugin

import sbt.{io as _, *}
import sbt.Keys.*
import dbcodegen.*

import java.io.File
import java.sql.SQLType

object DbCodegenPlugin extends AutoPlugin {
  override def trigger = noTrigger

  object autoImport {
    val dbcodegenSetupTask =
      taskKey[Unit]("Setup task to be executed before the code generation runs against the database")
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

    def executeSql(sql: String): Def.Initialize[Task[Unit]] = Def.task {
      val connectionSource = DbConnection.getSource(dbConfig.value)
      SqlExecutor.executeSql(connectionSource.get(), sql)
    }

    def executeSqlFile(file: File): Def.Initialize[Task[Unit]] = Def.task {
      val connectionSource = DbConnection.getSource(dbConfig.value)
      SqlExecutor.executeSqlFile(connectionSource.get(), file)
    }
  }
  import autoImport._

  override lazy val projectSettings: Seq[Def.Setting[_]] = Seq(
    dbcodegenSetupTask         := {},
    dbcodegenTemplateFiles     := Seq.empty,
    dbcodegenTypeMapping       := ((_, tpe) => tpe),
    dbcodegenSchemaTableFilter := ((_, _) => true),
    dbcodegenScalafmt          := true,
    dbcodegenUsername          := None,
    dbcodegenPassword          := None,
    (Compile / sourceGenerators) += Def.task {
      val outDir = (Compile / sourceManaged).value / "scala" / "dbcodegen"

      val _ = dbcodegenSetupTask.value

      // TODO: caching?
      val generatedFiles = CodeGenerator.generate(
        dbConfig.value,
        CodeGeneratorConfig(
          templateFiles = dbcodegenTemplateFiles.value,
          outDir = outDir,
          typeMapping = dbcodegenTypeMapping.value,
          schemaTableFilter = dbcodegenSchemaTableFilter.value,
          scalafmt = dbcodegenScalafmt.value,
          scalaVersion = scalaVersion.value,
        ),
      )

      generatedFiles.map(_.toFile)
    }.taskValue,
  )

  private val dbConfig = Def.task {
    DbConfig(
      jdbcUrl = dbcodegenJdbcUrl.value,
      username = dbcodegenUsername.value,
      password = dbcodegenPassword.value,
    )
  }
}