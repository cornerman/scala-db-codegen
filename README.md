# scala-db-codegen

A sbt-plugin and mill-plugin to generate boilerplate code from a database schema. Tested with SQLite and Postgresql. Should work for all databases supported by jdbc.

> DbSchema + Template => generate scala code

The plugin can be configured to crawl a database schema to extract all tables/columns/enums. It matches the occurring jdbc/sql types to scala types.
You can provide a [scalate](https://scalate.github.io/scalate/) template to generate scala code out of this information like this:
```scala
<%@ val schema: dbcodegen.DataSchema %>

package kicks.db.${schema.name}

#for (table <- schema.tables)
case class ${table.scalaName}(
  #for (column <- table.columns)
  ${column.scalaName}: ${column.scalaType},
  #end
)
#end
```


## Usage

### sbt

In `project/plugins.sbt`:
```sbt
addSbtPlugin("com.github.cornerman" % "sbt-db-codegen" % "0.2.0")
```

In `build.sbt`:
```sbt
lazy val db = project
  .enablePlugins(dbcodegen.plugin.DbCodegenPlugin)
  .settings(
    // The jdbc URL for the database
    dbcodegenJdbcUrl := "jdbc:...",
    // The template file for the code generator
    dbcodegenTemplateFiles := Seq(file("schema.scala.ssp"))

    // Optional database username
    // dbcodegenUsername          := None,
    // Optional database password
    // dbcodegenPassword          := None,
    // Map sql types to java/scala types
    // dbcodegenTypeMapping       := (sqlType: SQLType, scalaType: Option[String]) => scalaType,
    // Filter which schema and table should be processed
    // dbcodegenSchemaTableFilter := (schema: String, table: String) => true
    // Setup task to be executed before the code generation runs against the database
    // dbcodegenSetupTask         := {},
  )
```

#### Setup database before codegen

An example for using the `dbcodegenSetupTask` to setup an sqlite database with a `schema.sql` file before the code generation runs:
```sbt
dbcodegenSetupTask := Def.taskDyn {
    IO.delete(file(dbcodegenJdbcUrl.value.stripPrefix("jdbc:sqlite:")))
    executeSqlFile(file("./schema.sql"))
}
```

The functions `executeSql` and `executeSqlFile` are provided for these kind of use-cases and use the provided jdbcUrl, username, and password.


### mill

In `build.sc`:
```scala
import mill._, scalalib._
import $ivy.`com.github.cornerman::mill-db-codegen:0.2.0`, dbcodegen.plugin.DbCodegenModule

object backend extends ScalaModule with DbCodegenModule {
  // The jdbc URL for the database
  def dbcodegenJdbcUrl       = "jdbc:sqlite:..."
  // The template file for the code generator
  def dbcodegenTemplateFiles = Seq(PathRef(os.pwd / "schema.scala.ssp"))
  // Setup task to be executed before the code generation runs against the database
  def dbcodegenSetupTask = T.task {
    val dbpath = dbcodegenJdbcUrl.stripPrefix("jdbc:sqlite:")
    os.remove(os.pwd / dbpath)
    executeSqlFile(PathRef(os.pwd / "schema.sql"))
  }
  // Optional database username
  // def dbcodegenUsername = None
  // Optional database password
  // def dbcodegenPassword = None
  // Map sql types to java/scala types
  // def dbcodegenTypeMapping = (sqlType: SQLType, scalaType: Option[String]) => scalaType
  // Filter which schema and table should be processed
  // def dbcodegenSchemaTableFilter = (schema: String, table: String) => true
}
```

## Template Examples

Template can be configured by setting `dbcodegenTemplateFiles`.

We are using [scalate](https://scalate.github.io/scalate/) for templates, so you can use anything that is supported there (e.g. `mustache` or `ssp`) - the converter will be picked according to the file extension of the provided template file. Check the [scalate user guide](https://scalate.github.io/scalate/documentation/user-guide.html) for more details.

The template is called on each database schema, and is passed an instance of [`dbcodegen.DataSchema`](codegen/src/main/scala/dbcodegen/DataSchema.scala) (variable name `schema`) which contains all the extracted information.
You can see the declaration in the first line of each `ssp` template.

### Simple

case-classes.scala.ssp:
```scala
<%@ val schema: dbcodegen.DataSchema %>

package kicks.db.${schema.name}

#for (enum <- schema.enums)
type ${enum.scalaName} = ${enum.values.map(v => "\"" + v.name + "\"").mkString(" | ")}
#end

#for (table <- schema.tables)

case class ${table.scalaName}(
  #for (column <- table.columns)
  ${column.scalaName}: ${column.scalaType},
  #end
)

#end
```

#### with scala 3 enums

```scala
#for (enum <- schema.enums)

enum ${enum.scalaName}(val sqlValue: String) {
  #for (enumValue <- enum.values)
  case ${enumValue.scalaName} extends ${enum.scalaName}("${enumValue.name}")
  #end
}
object ${enum.scalaName} {
  def bySqlValue(searchValue: String): Option[${enum.scalaName}] = values.find(_.sqlValue == searchValue)
}

#end
```

### Library: quill

quill-case-classes.scala.ssp:
```scala
<%@ val schema: dbcodegen.DataSchema %>

package kicks.db.${schema.scalaName}

import io.getquill.*

#for (enum <- schema.enums)
type ${enum.scalaName} = ${enum.values.map(v => "\"" + v.name + "\"").mkString(" | ")}
#end

#for (table <- schema.tables)

case class ${table.scalaName}(
  #for (column <- table.columns)
  ${column.scalaName}: ${column.scalaType},
  #end
)
object ${table.scalaName} {
  inline def query = querySchema[Person](
    "${table.name}",
    #for (column <- table.columns)
    _.${column.scalaName} -> "${column.name}",
    #end
  )
}

#end
```

### Library: magnum

magnum-case-classes.scala.ssp:
```scala
<%@ val schema: dbcodegen.DataSchema %>

package kicks.db.${schema.scalaName}

import com.augustnagro.magnum.*

#for (enum <- schema.enums)
type ${enum.scalaName} = ${enum.values.map(v => "\"" + v.name + "\"").mkString(" | ")}
#end

#for (table <- schema.tables)

@Table(SqliteDbType)
case class ${table.scalaName}(
  #for (column <- table.columns)
  @SqlName("${column.name}") ${column.scalaName}: ${column.scalaType},
  #end
) derives DbCodec
object ${table.scalaName} {
  #{ val primaryKeyColumns = table.columns.filter(_.isPartOfPrimaryKey)}#
  type Id = ${if (primaryKeyColumns.isEmpty) "Null" else primaryKeyColumns.map(_.scalaType).mkString("(", ", ", ")")}

  case class Creator(
    #for (column <- table.columns if !column.isAutoGenerated)
    ${column.scalaName}: ${column.scalaType},
    #end
  )
}

val ${table.scalaName}Repo = Repo[${table.scalaName}.Creator, ${table.scalaName}, ${table.scalaName}.Id]

#end
```
