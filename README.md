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

You can use the `codegen` project as a library or use the sbt and mill plugin.
For ease of use, jdbc drivers as well as schemacrawler plugins for postgresql, mysql and sqlite are included.

### sbt

In `project/plugins.sbt`:
```sbt
addSbtPlugin("com.github.cornerman" % "sbt-db-codegen" % "0.5.2")
```

In `build.sbt`:
```sbt
lazy val db = project
  .enablePlugins(dbcodegen.plugin.DbCodegenPlugin)
  .settings(
    // The template file for the code generator
    dbcodegenTemplateFiles := Seq(file("schema.scala.ssp"))
    // The jdbc URL for the database
    dbcodegenJdbcUrl := "jdbc:...",

    // Optional database username
    // dbcodegenUsername          := None,
    // Optional database password
    // dbcodegenPassword          := None,
    // Map sql types to java/scala types
    // dbcodegenTypeMapping       := (sqlType: SQLType, scalaType: Option[String]) => scalaType,
    // Filter which schema and table should be processed
    // dbcodegenSchemaTableFilter := (schema: String, table: String) => true
    // Whether to run scalafmt on the generated code
    // dbcodegenScalafmt := true
    // Setup task to be executed before the code generation runs against the database
    // dbcodegenSetupTask         := { _ => () },
  )
```

#### Setup database before codegen

An example for using the `dbcodegenSetupTask` to setup an sqlite database with a `schema.sql` file before the code generation runs:
```sbt
dbcodegenJdbcUrl := "jdbc:sqlite:file::memory:?cache=shared",
dbcodegenSetupTask := { db =>
  db.executeSqlFile(file("./schema.sql"))
}
```

The functions `executeSql` and `executeSqlFile` are provided for these kind of use-cases and use the provided jdbcUrl, username, and password.


### mill

In `build.sc`:
```scala
import mill._, scalalib._
import $ivy.`com.github.cornerman::mill-db-codegen:0.5.2`, dbcodegen.plugin._

object backend extends ScalaModule with DbCodegenModule {
  // sources to trigger reloads when a file changes
  def dbTemplateFile         = T.source(os.pwd / "schema.scala.ssp")
  def dbSchemaFile           = T.source(os.pwd / "schema.sql")

  // The template file for the code generator
  def dbcodegenTemplateFiles = T { Seq(dbTemplateFile()) }
  // The jdbc URL for the database
  def dbcodegenJdbcUrl       = "jdbc:sqlite:file::memory:?cache=shared"
  // Setup task to be executed before the code generation runs against the database
  def dbcodegenSetupTask = T.task { (db: Db) =>
    db.executeSqlFile(dbSchemaFile())
  }

  // Optional database username
  // def dbcodegenUsername = None
  // Optional database password
  // def dbcodegenPassword = None
  // Map sql types to java/scala types
  // def dbcodegenTypeMapping = (sqlType: SQLType, scalaType: Option[String]) => scalaType
  // Filter which schema and table should be processed
  // def dbcodegenSchemaTableFilter = (schema: String, table: String) => true
  // Whether to run scalafmt on the generated code
  // def dbcodegenScalafmt = true
}
```

### CLI

Use coursier to launch the CLI for generating code:
```bash
cs launch com.github.cornerman:scala-db-codegen-cli_2.13:0.5.2 -- --jdbc-url jdbc:postgresql://localhost:5432/postgres --username postgres --password password --out-dir generated-code --template-file schema.scala.ssp --scala-version "3.0.0"
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
  inline def query = querySchema[${table.scalaName}](
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
  #if (column.db.isPartOfPrimaryKey)
  @Id
  #end
  @SqlName("${column.name}")
  ${column.scalaName}: ${column.scalaType},
  #end
) derives DbCodec
object ${table.scalaName} {
  #{ val primaryKeyColumns = table.columns.filter(_.db.isPartOfPrimaryKey)}#
  type Id = ${if (primaryKeyColumns.isEmpty) "Null" else primaryKeyColumns.map(_.scalaType).mkString("(", ", ", ")")}

  #if (!table.isView)
  case class Creator(
    #for (column <- table.columns if !column.db.isGenerated && !column.db.hasDefaultValue && !column.db.isAutoIncremented)
    ${column.scalaName}: ${column.scalaType},
    #end
  )
  #end
}

#if (table.isView)
val ${table.scalaName}Repo = ImmutableRepo[${table.scalaName}, ${table.scalaName}.Id]
#else
val ${table.scalaName}Repo = Repo[${table.scalaName}.Creator, ${table.scalaName}, ${table.scalaName}.Id]
#end

#end
```
