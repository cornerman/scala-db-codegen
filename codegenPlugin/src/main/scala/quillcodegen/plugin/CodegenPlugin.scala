package quillcodegen.plugin

import io.getquill.codegen.model.{NameParser, SnakeCaseNames}
import quillcodegen.{Codegen, SqlExecutor}
import sbt.{io => _, _}
import sbt.Keys._

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import java.io.File

object CodegenPlugin extends AutoPlugin {
  override def trigger = noTrigger

  object autoImport {
    val quillcodegenSetupTask           = taskKey[Unit]("Setup task to be executed before the code generation runs against the database")
    val quillcodegenJdbcUrl             = settingKey[String]("The jdbc URL for the database")
    val quillcodegenPackagePrefix       = settingKey[String]("The package prefix for the generated code")
    val quillcodegenNestedTrait         = settingKey[Boolean]("Whether to generate nested traits, default is false")
    val quillcodegenGenerateQuerySchema = settingKey[Boolean]("Whether to generate query schemas, default is true")
    val quillcodegenNaming              = settingKey[NameParser]("The naming parser to use, default is SnakeCaseNames")
    val quillcodegenUsername            = settingKey[Option[String]]("Optional database username")
    val quillcodegenPassword            = settingKey[Option[String]]("Optional database password")
    val quillcodegenTimeout             = settingKey[Duration]("Timeout for the generate task")
    val quillVersion                    = settingKey[String]("Version used for quill-core")

    def executeSql(sql: String): Def.Initialize[Task[Unit]] = Def.task {
      val dataSource =
        SqlExecutor.getDataSource(quillcodegenJdbcUrl.value, username = quillcodegenUsername.value, password = quillcodegenPassword.value)
      SqlExecutor.executeSql(dataSource, sql)
    }

    def executeSqlFile(file: File): Def.Initialize[Task[Unit]] = Def.task {
      val dataSource =
        SqlExecutor.getDataSource(quillcodegenJdbcUrl.value, username = quillcodegenUsername.value, password = quillcodegenPassword.value)
      SqlExecutor.executeSqlFile(dataSource, file)
    }
  }
  import autoImport._

  override lazy val projectSettings: Seq[Def.Setting[_]] = Seq(
    quillcodegenSetupTask           := {},
    quillcodegenNestedTrait         := false,
    quillcodegenGenerateQuerySchema := true,
    quillcodegenNaming              := SnakeCaseNames,
    quillcodegenUsername            := None,
    quillcodegenPassword            := None,
    quillcodegenTimeout             := Duration.Inf,
    quillVersion                    := "4.8.1",

    // Should be same as in build.sbt for codegen module
    libraryDependencies += "io.getquill" %% "quill-core" % quillVersion.value,
    (Compile / sourceGenerators) += Def.task {
      val outDir = (Compile / sourceManaged).value / "scala" / "quillcodegen"

      val _ = quillcodegenSetupTask.value

      // TODO: caching?
      val generation = Codegen.generate(
        outDir = outDir,
        packagePrefix = quillcodegenPackagePrefix.value,
        jdbcUrl = quillcodegenJdbcUrl.value,
        username = quillcodegenUsername.value,
        password = quillcodegenPassword.value,
        naming = quillcodegenNaming.value,
        generateQuerySchema = quillcodegenGenerateQuerySchema.value,
        nestedTrait = quillcodegenNestedTrait.value,
      )

      val generatedFiles = Await.result(generation, quillcodegenTimeout.value)

      generatedFiles.map(_.toFile)
    }.taskValue,
  )
}
