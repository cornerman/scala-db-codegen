package dbcodegen

import org.flywaydb.core.api.configuration.ClassicConfiguration
import org.flywaydb.core.internal.database.DatabaseTypeRegister
import org.flywaydb.core.internal.parser.ParsingContext
import org.flywaydb.core.internal.resource.StringResource

import java.io.File
import java.sql.Connection
import scala.io.Source
import scala.jdk.CollectionConverters._
import scala.util.Using

object SqlExecutor {
  def executeSqlFile(connection: Connection, file: File): Unit =
    Using.resource(Source.fromFile(file)) { fileSource =>
      executeSql(connection, fileSource.mkString)
    }

  def executeSql(connection: Connection, sql: String): Unit = {
    val databaseType = DatabaseTypeRegister.getDatabaseTypeForConnection(connection)
    val factory = databaseType.createSqlScriptFactory(new ClassicConfiguration(), new ParsingContext())
    val resource = new StringResource(sql)
    val sqlScript = factory.createSqlScript(resource, false, null)
    val statement = connection.createStatement()
    sqlScript.getSqlStatements.asScala.foreach(sqlStatement =>
      statement.execute(sqlStatement.getSql)
    )
  }
}
