package dbcodegen

import schemacrawler.schema.{Column, Index, Schema, Table, View}

case class DataColumn(
  name: String,
  scalaType: String,
  db: Column,
) {
  def scalaName = NameFormat.sanitizeScalaName(NameFormat.toCamelCase(name))
}

case class DataIndex(
  name: String,
  columns: Seq[DataColumn],
  db: Index,
) {
  def scalaName = NameFormat.sanitizeScalaName(NameFormat.toPascalCase(name))
}

case class DataTable(
  name: String,
  columns: Seq[DataColumn],
  indices: Seq[DataIndex],
  db: Table,
) {
  def isView: Boolean = db.isInstanceOf[View]
  def scalaName       = NameFormat.sanitizeScalaName(NameFormat.toPascalCase(name))
}

case class DataEnumValue(
  name: String
) {
  def scalaName = NameFormat.sanitizeScalaName(name)
}

case class DataEnum(
  name: String,
  values: Seq[DataEnumValue],
) {
  def scalaName = NameFormat.sanitizeScalaName(NameFormat.toPascalCase(name))
}

case class DataStructMember(
  name: String,
  scalaType: String,
) {
  def scalaName = NameFormat.sanitizeScalaName(NameFormat.toCamelCase(name))
}

case class DataStruct(
  name: String,
  member: Seq[DataStructMember],
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
