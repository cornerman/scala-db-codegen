package dbcodegen

import org.apache.ibatis.jdbc.ScriptRunner

import java.io.{File, StringReader}
import java.sql.Connection
import scala.io.Source
import scala.util.Using
import scala.util.chaining._

object SqlExecutor {
  def executeSqlFile(connection: Connection, file: File): Unit =
    Using.resource(Source.fromFile(file)) { fileSource =>
      executeSql(connection, fileSource.mkString)
    }

  def executeSql(connection: Connection, sql: String): Unit =
    Using.resource(new StringReader(sql)) { reader =>
      createScriptRunner(connection).runScript(reader)
    }

  private def createScriptRunner(connection: Connection) =
    new ScriptRunner(connection)
      .tap(_.setStopOnError(true))
      .tap(_.setSendFullScript(false))
      .tap(_.setAutoCommit(true))
      .tap(_.setRemoveCRs(true))
}
