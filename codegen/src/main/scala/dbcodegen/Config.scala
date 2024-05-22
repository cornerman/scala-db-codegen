package dbcodegen

import java.io.File
import java.sql.SQLType

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
  scalafmt: Boolean,
)
