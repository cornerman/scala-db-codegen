package schemacrawler.crawl

import schemacrawler.schema.{ColumnDataType, DataTypeType, Schema}

object SchemaCrawlerExt {
  def newColumnDataType(schema: Schema, name: String, tpe: DataTypeType): ColumnDataType =
    new MutableColumnDataType(schema, name, tpe)
}
