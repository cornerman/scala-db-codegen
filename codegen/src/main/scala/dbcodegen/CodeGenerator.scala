package dbcodegen

import schemacrawler.schemacrawler._
import schemacrawler.tools.databaseconnector.{DatabaseConnectorRegistry, DatabaseUrlConnectionOptions}
import schemacrawler.tools.utility.SchemaCrawlerUtility
import us.fatehi.utility.datasource.MultiUseUserCredentials
import org.fusesource.scalate.{TemplateEngine, TemplateSource}

import java.io.File
import java.nio.file.{Files, Path, Paths}
import java.sql.SQLType
import scala.jdk.CollectionConverters._

case class DbConfig(
  jdbcUrl: String,
  username: Option[String],
  password: Option[String],
)

case class CodeGeneratorConfig(
  templateFiles: Seq[File],
  outDir: File,
  typeMapping: (SQLType, Option[String]) => Option[String],
  schemaTableFilter: (String, String) => Boolean,
)

object CodeGenerator {
  def generate(db: DbConfig, config: CodeGeneratorConfig): Seq[Path] = {
    // schema crawler options

    val credentials = new MultiUseUserCredentials(db.username.orNull, db.password.orNull)
    val connection =
      DatabaseConnectorRegistry
        .getDatabaseConnectorRegistry()
        .findDatabaseConnectorFromUrl(db.jdbcUrl)
        .newDatabaseConnectionSource(new DatabaseUrlConnectionOptions(db.jdbcUrl), credentials)

    val schemaCrawlerOptions = SchemaCrawlerOptionsBuilder
      .newSchemaCrawlerOptions()
      .withLoadOptions(LoadOptionsBuilder.builder().withInfoLevel(InfoLevel.maximum).toOptions)
      .withLimitOptions(LimitOptionsBuilder.builder().toOptions)

    val catalog = SchemaCrawlerUtility.getCatalog(connection, schemaCrawlerOptions)

    // scalate

    val templateEngine  = new TemplateEngine()
    val templateSources = config.templateFiles.map(TemplateSource.fromFile)

    // run template on schemas

    val dataSchemas = catalog.getSchemas.asScala.map { schema =>
      SchemaConverter.toDataSchema(schema, connection, catalog.getTables(schema).asScala.toSeq, config)
    }

    dataSchemas.flatMap { dataSchema =>
      val data = Map(
        "schema" -> dataSchema
      )

      templateSources.map { templateSource =>
        val output     = templateEngine.layout(templateSource, data)
        val outputPath = Paths.get(config.outDir.getPath, templateSource.file.getPath, s"${dataSchema.name}.scala")

        Files.createDirectories(outputPath.getParent)
        Files.write(outputPath, output.getBytes)

        outputPath
      }
    }.toSeq
  }

}
