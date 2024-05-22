package dbcodegen

import schemacrawler.tools.databaseconnector.{DatabaseConnectorRegistry, DatabaseUrlConnectionOptions}
import us.fatehi.utility.datasource.{DatabaseConnectionSource, MultiUseUserCredentials}

object DbConnection {
  def getSource(dbConfig: DbConfig): DatabaseConnectionSource = {
    val credentials = new MultiUseUserCredentials(dbConfig.username.orNull, dbConfig.password.orNull)
    DatabaseConnectorRegistry
      .getDatabaseConnectorRegistry()
      .findDatabaseConnectorFromUrl(dbConfig.jdbcUrl)
      .newDatabaseConnectionSource(new DatabaseUrlConnectionOptions(dbConfig.jdbcUrl), credentials)
  }
}