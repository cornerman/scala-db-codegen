package dbcodegen.plugin

import mill._
import scalalib._
import dbcodegen._

import java.sql.SQLType

trait DbCodegenModule extends ScalaModule {

  // The jdbc URL for the database
  def dbcodegenJdbcUrl: String
  // The template file for the code generator
  def dbcodegenTemplateFiles: Seq[PathRef] = Seq.empty
  // Output path for the generated code
  def dbcodegenOutPath: T[PathRef] = T { PathRef(T.ctx().dest / "scala") }
  // Setup task to be executed before the code generation runs against the database
  def dbcodegenSetupTask: Task[Unit] = T.task { () }
  // Map jdbc types to java/scala types
  def dbcodegenTypeMapping: (SQLType, Option[String]) => Option[String] = (_, tpe) => tpe
  // Filter which schema and table should be processed
  def dbcodegenSchemaTableFilter: (String, String) => Boolean = (_, _) => true
  // Whether to run scalafmt on the generated code
  def dbcodegenScalafmt: Boolean = true
  // Optional database username
  def dbcodegenUsername: Option[String] = None
  // Optional database password
  def dbcodegenPassword: Option[String] = None

  def dbcodegen: Task[Seq[PathRef]] = T.task {

    val _ = dbcodegenSetupTask()

    val generatedFiles = CodeGenerator.generate(
      DbConfig(
        jdbcUrl = dbcodegenJdbcUrl,
        username = dbcodegenUsername,
        password = dbcodegenPassword,
      ),
      CodeGeneratorConfig(
        templateFiles = dbcodegenTemplateFiles.map(_.path.toIO),
        outDir = dbcodegenOutPath().path.toIO,
        typeMapping = dbcodegenTypeMapping,
        schemaTableFilter = dbcodegenSchemaTableFilter,
        scalafmt = dbcodegenScalafmt,
      ),
    )

    generatedFiles.map(f => PathRef(os.Path(f.toFile)))
  }

  override def generatedSources: T[Seq[PathRef]] = T {
    val scalaOutput = dbcodegen()
    scalaOutput ++ super.generatedSources()
  }

  def executeSql(sql: String): Unit = {
    val dataSource =
      SqlExecutor.getDataSource(dbcodegenJdbcUrl, username = dbcodegenUsername, password = dbcodegenPassword)
    SqlExecutor.executeSql(dataSource, sql)
  }

  def executeSqlFile(file: PathRef): Unit = {
    val dataSource =
      SqlExecutor.getDataSource(dbcodegenJdbcUrl, username = dbcodegenUsername, password = dbcodegenPassword)
    SqlExecutor.executeSqlFile(dataSource, file.path.toIO)
  }
}
