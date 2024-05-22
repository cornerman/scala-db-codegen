package dbcodegen

import org.fusesource.scalate.{TemplateEngine, TemplateSource}
import org.scalafmt.Scalafmt
import org.scalafmt.config.ScalafmtConfig
import schemacrawler.schemacrawler._
import schemacrawler.tools.utility.SchemaCrawlerUtility
import us.fatehi.utility.datasource.DatabaseConnectionSource

import java.nio.file.{Files, Path, Paths}
import scala.jdk.CollectionConverters._
import scala.meta.dialects

object CodeGenerator {
  def generate(connectionSource: DatabaseConnectionSource, config: CodeGeneratorConfig): Seq[Path] = {
    // schema crawler options

    val schemaCrawlerOptions = SchemaCrawlerOptionsBuilder
      .newSchemaCrawlerOptions()
      .withLoadOptions(LoadOptionsBuilder.builder().withInfoLevel(InfoLevel.maximum).toOptions)
      .withLimitOptions(LimitOptionsBuilder.builder().toOptions)

    val catalog = SchemaCrawlerUtility.getCatalog(connectionSource, schemaCrawlerOptions)

    // scalate

    val templateEngine  = new TemplateEngine()
    val templateSources = config.templateFiles.map(TemplateSource.fromFile)

    // run template on schemas

    val dataSchemas = catalog.getSchemas.asScala.map { schema =>
      SchemaConverter.toDataSchema(schema, connectionSource, catalog.getTables(schema).asScala.toSeq, config)
    }

    dataSchemas.flatMap { dataSchema =>
      val data = Map(
        "schema" -> dataSchema
      )

      templateSources.map { templateSource =>
        val rawOutput = templateEngine.layout(templateSource, data)

        val formatted = if (config.scalafmt) {
          val scalafmtConfig = ScalafmtConfig.default.withDialect(scalaVersionToScalafmtDialect(config.scalaVersion))
          Scalafmt.format(rawOutput, scalafmtConfig).toEither.toOption
        } else None

        val output     = formatted.getOrElse(rawOutput)
        val outputPath = Paths.get(config.outDir.getPath, templateSource.file.getPath, s"${dataSchema.name}.scala")

        Files.createDirectories(outputPath.getParent)
        Files.write(outputPath, output.getBytes)

        outputPath
      }
    }.toSeq
  }

  def scalaVersionToScalafmtDialect(scalaVersion: String) = scalaVersion match {
    case v if v.startsWith("3.") => dialects.Scala3
    case _                       => dialects.Scala
  }
}
