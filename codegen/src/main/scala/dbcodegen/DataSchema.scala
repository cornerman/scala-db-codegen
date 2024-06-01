package dbcodegen

import schemacrawler.schema.{Column, Schema, Table}

case class DataColumn(
  name: String,
  scalaType: String,
  db: Column,
) {
  def scalaName = NameFormat.sanitizeScalaName(NameFormat.toCamelCase(name))
}

case class DataTable(
  name: String,
  columns: Seq[DataColumn],
  isView: Boolean,
  db: Table,
) {
  def scalaName = NameFormat.sanitizeScalaName(NameFormat.toPascalCase(name))
}

case class DataEnumValue(
  name: String
) {
  def scalaName = NameFormat.sanitizeScalaName(NameFormat.toPascalCase(name))
}

case class DataEnum(
  name: String,
  values: Seq[DataEnumValue],
) {
  def scalaName = NameFormat.sanitizeScalaName(NameFormat.toPascalCase(name))
}

case class DataSchema(
  name: String,
  tables: Seq[DataTable],
  enums: Seq[DataEnum],
  db: Schema,
) {
  def scalaName = NameFormat.sanitizeScalaName(NameFormat.toCamelCase(name))
}
