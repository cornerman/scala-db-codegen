package dbcodegen.plugin

import mill._
import scalalib._
import dbcodegen._
import us.fatehi.utility.datasource.DatabaseConnectionSource

import java.sql.{Connection, SQLType}
import scala.util.Using

trait DbCodegenModule extends ScalaModule {

  // The jdbc URL for the database
  def dbcodegenJdbcUrl: String
  // The template file for the code generator
  def dbcodegenTemplateFiles: Seq[PathRef] = Seq.empty
  // Output path for the generated code
  def dbcodegenOutPath: T[PathRef] = T { PathRef(T.ctx().dest / "scala") }
  // Setup task to be executed before the code generation runs against the database
  def dbcodegenSetupTask: Task[Db => Unit] = T.task { (_: Db) => () }
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

    val dbConfig = DbConfig(
      jdbcUrl = dbcodegenJdbcUrl,
      username = dbcodegenUsername,
      password = dbcodegenPassword,
    )

    val codeGeneratorConfig = CodeGeneratorConfig(
      templateFiles = dbcodegenTemplateFiles.map(_.path.toIO),
      outDir = dbcodegenOutPath().path.toIO,
      typeMapping = dbcodegenTypeMapping,
      schemaTableFilter = dbcodegenSchemaTableFilter,
      scalafmt = dbcodegenScalafmt,
      scalaVersion = scalaVersion(),
    )

    val setupTask = dbcodegenSetupTask()
    Using.resource(DbConnection.getSource(dbConfig)) { connectionSource =>
      setupTask(Db(connectionSource))

      val generatedFiles = CodeGenerator.generate(connectionSource, codeGeneratorConfig)

      generatedFiles.map(f => PathRef(os.Path(f.toFile)))
    }

  }

  override def generatedSources: T[Seq[PathRef]] = T {
    val scalaOutput = dbcodegen()
    scalaOutput ++ super.generatedSources()
  }
}

trait Db {
  def connection: Connection
  def executeSql(sql: String): Unit
  def executeSqlFile(file: PathRef): Unit
}
object Db {
  def apply(source: DatabaseConnectionSource): Db = new Db {
    lazy val connection                     = source.get()
    def executeSql(sql: String): Unit       = SqlExecutor.executeSql(connection, sql)
    def executeSqlFile(file: PathRef): Unit = SqlExecutor.executeSqlFile(connection, file.path.toIO)
  }
}
