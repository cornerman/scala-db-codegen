package quillcodegen.plugin

import io.getquill.codegen.jdbc.model.JdbcTypeInfo
import mill._
import scalalib._
import io.getquill.codegen.model.{JdbcColumnMeta, JdbcTableMeta, NameParser, NumericPreference, RawSchema, SkipColumn, SnakeCaseNames, UnrecognizedTypeStrategy, UseDefaults}
import quillcodegen.{Codegen, SqlExecutor}

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.reflect.ClassTag

trait CodegenPlugin extends ScalaModule {

  // The jdbc URL for the database
  def quillcodegenJdbcUrl: String
  // Setup task to be executed before the code generation runs against the database
  def quillcodegenSetupTask: Task[Unit] = T.task {()}
  // The package prefix for the generated code
  def quillcodegenPackagePrefix: String
  // Whether to generate nested traits, default is false
  def quillcodegenNestedTrait: Boolean = false
  // Whether to generate query schemas, default is true
  def quillcodegenGenerateQuerySchema: Boolean = true
  // Specify which tables to process, default is all
  def quillcodegenTableFilter: RawSchema[JdbcTableMeta, JdbcColumnMeta] => Boolean = _ => true
  // Strategy for unrecognized types
  def quillcodegenUnrecognizedType: UnrecognizedTypeStrategy = SkipColumn
  // Map jdbc types to java/scala types
  def quillcodegenTypeMapping: (JdbcTypeInfo, Option[ClassTag[_]]) => Option[ClassTag[_]] = (_, classTag) => classTag
  // Which numeric type preference for numeric types
  def quillcodegenNumericType: NumericPreference = UseDefaults
  // The naming parser to use, default is SnakeCaseNames
  def quillcodegenNaming: NameParser = SnakeCaseNames
  // Optional database username
  def quillcodegenUsername: Option[String] = None
  // Optional database password
  def quillcodegenPassword: Option[String] = None
  // Timeout for the generate task
  def quillcodegenTimeout: Duration = Duration.Inf
  // Version used for quill-core
  def quillVersion: String = "4.8.1"

  def quillcodegenOutPath: T[PathRef] = T { PathRef(T.ctx().dest / "scala") }

  def quillcodegen: Task[Seq[PathRef]] = T.task {

    val _ = quillcodegenSetupTask()

    val generation = Codegen.generate(
      outDir = quillcodegenOutPath().path.toIO,
      packagePrefix = quillcodegenPackagePrefix,
      jdbcUrl = quillcodegenJdbcUrl,
      username = quillcodegenUsername,
      password = quillcodegenPassword,
      naming = quillcodegenNaming,
      generateQuerySchema = quillcodegenGenerateQuerySchema,
      nestedTrait = quillcodegenNestedTrait,
      tableFilter = quillcodegenTableFilter,
      unrecognizedType = quillcodegenUnrecognizedType,
      typeMapping = quillcodegenTypeMapping,
      numericType = quillcodegenNumericType,
      isScala3 = isScala3(),
    )

    val generatedFiles = Await.result(generation, quillcodegenTimeout)

    generatedFiles.map(f => PathRef(os.Path(f.toFile)))
  }

  override def ivyDeps: T[Agg[Dep]] = T {
    val deps =
      if (isScala3()) Agg(ivy"io.getquill::quill-sql::${quillVersion}")
      else Agg(ivy"io.getquill::quill-core::${quillVersion}")

    super.ivyDeps() ++ deps
  }

  override def generatedSources: T[Seq[PathRef]] = T {
    val scalaOutput = quillcodegen()
    scalaOutput ++ super.generatedSources()
  }

  def executeSql(sql: String): Task[Unit] = T.task {
    val dataSource =
      SqlExecutor.getDataSource(quillcodegenJdbcUrl, username = quillcodegenUsername, password = quillcodegenPassword)
    SqlExecutor.executeSql(dataSource, sql)
  }

  def executeSqlFile(file: PathRef): Task[Unit] = T.task {
    val dataSource =
      SqlExecutor.getDataSource(quillcodegenJdbcUrl, username = quillcodegenUsername, password = quillcodegenPassword)
    SqlExecutor.executeSqlFile(dataSource, file.path.toIO)
  }

  private def isScala3 = T{ scalaVersion().startsWith("3.") }
}
