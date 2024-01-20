package quillcodegen

import com.mysql.cj.jdbc.MysqlDataSource
import org.mariadb.jdbc.MariaDbDataSource
import org.postgresql.ds.PGSimpleDataSource
import org.sqlite.SQLiteDataSource

import java.io.File
import javax.sql.DataSource
import scala.io.Source
import scala.util.chaining._

object SqlExecutor {
  def executeSqlFile(dataSource: DataSource, file: File): Unit = {
    executeSql(dataSource, Source.fromFile(file).mkString)
  }

  def executeSql(dataSource: DataSource, sql: String): Unit = {
    val connection = dataSource.getConnection
    try {
      val _ = connection.createStatement().execute(sql)
    } finally {
      connection.close()
    }
  }

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
