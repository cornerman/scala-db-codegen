package quillcodegen

import io.getquill.codegen.gen.EmitterSettings
import io.getquill.codegen.util.StringUtil._
import io.getquill.codegen.util.StringSeqUtil._
import io.getquill.codegen.jdbc.ComposeableTraitsJdbcCodegen
import io.getquill.codegen.jdbc.model.JdbcTypeInfo
import io.getquill.codegen.model._

import java.io.File
import java.nio.file.Path
import scala.concurrent.Future
import scala.reflect.ClassTag

object Codegen {
  def generate(
    outDir: File,
    jdbcUrl: String,
    username: Option[String],
    password: Option[String],
    packagePrefix: String,
    tableFilter: RawSchema[JdbcTableMeta, JdbcColumnMeta] => Boolean,
    unrecognizedType: UnrecognizedTypeStrategy,
    typeMapping: (JdbcTypeInfo, Option[ClassTag[_]]) => Option[ClassTag[_]],
    numericType: NumericPreference,
    naming: NameParser,
    nestedTrait: Boolean,
    generateQuerySchema: Boolean,
    isScala3: Boolean,
  ): Future[scala.Seq[Path]] = {
    val dataSource = SqlExecutor.getDataSource(jdbcUrl, username = username, password = password)

    object gen extends ComposeableTraitsJdbcCodegen(dataSource, packagePrefix = packagePrefix, nestedTrait = nestedTrait) {
      override def typer: Typer                                                  = tpe => typeMapping(tpe, super.typer(tpe))
      override def unrecognizedTypeStrategy: UnrecognizedTypeStrategy            = unrecognizedType
      override def numericPreference: NumericPreference                          = numericType
      override def filter(tc: RawSchema[JdbcTableMeta, JdbcColumnMeta]): Boolean = super.filter(tc) && tableFilter(tc)
      override def nameParser: NameParser                                        = sanitizedNameParser(naming, shouldGenerateQuerySchema = generateQuerySchema)
      override def packagingStrategy: PackagingStrategy                          = PackagingStrategy.ByPackageHeader.TablePerSchema(this.packagePrefix)

      override def generatorMaker = new SingleGeneratorFactory[ContextifiedUnitGenerator] {
        override def apply(emitterSettings: EmitterSettings[JdbcTableMeta, JdbcColumnMeta]): ContextifiedUnitGenerator = {
          new ContextifiedUnitGeneratorWrap(emitterSettings)
        }
      }

      class ContextifiedUnitGeneratorWrap(emitterSettings: EmitterSettings[JdbcTableMeta, JdbcColumnMeta])
          extends ContextifiedUnitGenerator(emitterSettings) {
        private val scala3CodeEmitter = new CodeEmitter(emitterSettings) {
          override def CombinedTableSchemas = new CombinedTableSchemasGenWrap(_, _)

          class CombinedTableSchemasGenWrap(
            tableColumns: TableStereotype[TableMeta, ColumnMeta],
            querySchemaNaming: QuerySchemaNaming,
          ) extends CombinedTableSchemasGen(tableColumns, querySchemaNaming) {
            override def objectName: Option[String] = super.objectName.map(_ + "Dao")

            override def body: String = {
              val schemas = tableColumns.table.meta
                .map(schema => s"inline def ${querySchemaNaming(schema)} = " + indent(QuerySchema(tableColumns, schema).code))
                .mkString("\n\n")

              Seq(imports, schemas).pruneEmpty.mkString("\n\n")
            }

            override def QuerySchema = new QuerySchemaGenWrap(_, _)

            class QuerySchemaGenWrap(tableColumns: TableStereotype[TableMeta, ColumnMeta], schema: TableMeta)
                extends QuerySchemaGen(tableColumns, schema) {

              override def code: String = indent(querySchema)
            }
          }
        }

        override def tableSchemasCode: String =
          if (isScala3) {
            scala3CodeEmitter.tableSchemasCode.notEmpty
              .map(tsCode => s"""
                   |object ${traitName} {
                   |  import io.getquill.*
                   |
                   |  ${indent(possibleTraitNesting(indent(tsCode)))}
                   |}
                   |""".stripMargin.trimFront)
              .getOrElse("")
          } else super.tableSchemasCode
      }
    }

    gen.writeAllFiles(s"${outDir.getPath}/${packagePrefix.replace(".", "/")}")
  }

  private lazy val scalaKeywords = {
    val st = scala.reflect.runtime.universe.asInstanceOf[scala.reflect.internal.SymbolTable]
    st.nme.keywords.map(_.toString)
  }

  private def sanitizeScalaName(rawName: String): String = {
    val name = rawName.trim.replaceAll("(^[^a-zA-Z_]|[^a-zA-Z0-9_])", "_")
    if (scalaKeywords(name)) name + "_" else name
  }

  private def sanitizedNameParser(naming: NameParser, shouldGenerateQuerySchema: Boolean): NameParser = new CustomNames(
    cm => sanitizeScalaName(naming.parseColumn(cm)),
    tm => sanitizeScalaName(naming.parseTable(tm)),
  ) {
    override def generateQuerySchemas: Boolean = shouldGenerateQuerySchema
  }
}
