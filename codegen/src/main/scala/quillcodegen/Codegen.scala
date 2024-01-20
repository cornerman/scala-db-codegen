package quillcodegen

import io.getquill.codegen.jdbc.ComposeableTraitsJdbcCodegen
import io.getquill.codegen.model._

import java.io.File
import java.nio.file.Path
import scala.concurrent.Future

object Codegen {
  def generate(
    outDir: File,
    jdbcUrl: String,
    username: Option[String],
    password: Option[String],
    packagePrefix: String,
    naming: NameParser,
    nestedTrait: Boolean,
    generateQuerySchema: Boolean,
  ): Future[scala.Seq[Path]] = {
    val dataSource = SqlExecutor.getDataSource(jdbcUrl, username = username, password = password)

    val gen = new ComposeableTraitsJdbcCodegen(dataSource, packagePrefix = packagePrefix, nestedTrait = nestedTrait) {
      override def nameParser: NameParser               = sanitizedNameParser(naming, shouldGenerateQuerySchema = generateQuerySchema)
      override def packagingStrategy: PackagingStrategy = PackagingStrategy.ByPackageHeader.TablePerSchema(this.packagePrefix)
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

  private def sanitizedNameParser(naming: NameParser, shouldGenerateQuerySchema: Boolean): NameParser = new LiteralNames {
    override def generateQuerySchemas: Boolean           = shouldGenerateQuerySchema
    override def parseColumn(cm: JdbcColumnMeta): String = sanitizeScalaName(naming.parseColumn(cm))
    override def parseTable(tm: JdbcTableMeta): String   = sanitizeScalaName(naming.parseTable(tm))
  }
}
