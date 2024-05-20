package dbcodegen

import com.mysql.cj.jdbc.MysqlDataSource
import org.apache.ibatis.jdbc.ScriptRunner
import org.mariadb.jdbc.MariaDbDataSource
import org.postgresql.ds.PGSimpleDataSource
import org.sqlite.SQLiteDataSource

import java.io.{File, StringReader}
import java.sql.Connection
import javax.sql.DataSource
import scala.io.Source
import scala.util.chaining._

object SqlExecutor {
  def executeSqlFile(dataSource: DataSource, file: File): Unit = {
    executeSql(dataSource, Source.fromFile(file).mkString)
  }

  def executeSql(dataSource: DataSource, sql: String): Unit = {
    val connection = dataSource.getConnection
    val reader     = new StringReader(sql)

    try {
      createScriptRunner(connection).runScript(reader)
    } finally {
      reader.close()
      connection.close()
    }
  }

  private def createScriptRunner(connection: Connection) =
    new ScriptRunner(connection)
      .tap(_.setStopOnError(true))
      .tap(_.setSendFullScript(false))
      .tap(_.setAutoCommit(true))
      .tap(_.setRemoveCRs(true))

  def getDataSource(jdbcUrl: String, username: Option[String] = None, password: Option[String] = None): DataSource = jdbcUrl match {
    case s if s.startsWith("jdbc:sqlite:") =>
      new SQLiteDataSource().tap(_.setUrl(jdbcUrl))
    case s if s.startsWith("jdbc:postgresql:") =>
      new PGSimpleDataSource().tap(_.setURL(jdbcUrl)).tap(_.setUser(username.orNull)).tap(_.setPassword(password.orNull))
    case s if s.startsWith("jdbc:mysql:") =>
      new MysqlDataSource().tap(_.setURL(jdbcUrl)).tap(_.setUser(username.orNull)).tap(_.setPassword(password.orNull))
    case s if s.startsWith("jdbc:mariadb:") =>
      new MariaDbDataSource().tap(_.setUrl(jdbcUrl)).tap(_.setUser(username.orNull)).tap(_.setPassword(password.orNull))
    case _ => throw new IllegalArgumentException("Unexpected jdbc url")
  }
}
