Global / onChangedBuildSource := ReloadOnSourceChanges

inThisBuild(
  Seq(
    organization := "com.github.cornerman",
    licenses     := Seq("MIT License" -> url("https://opensource.org/licenses/MIT")),
    homepage     := Some(url("https://github.com/cornerman/scala-db-codegen")),
    scmInfo := Some(
      ScmInfo(
        url("https://github.com/cornerman/scala-db-codegen"),
        "scm:git:git@github.com:cornerman/scala-db-codegen.git",
        Some("scm:git:git@github.com:cornerman/scala-db-codegen.git"),
      )
    ),
    pomExtra :=
      <developers>
      <developer>
        <id>jkaroff</id>
        <name>Johannes Karoff</name>
        <url>https://github.com/cornerman</url>
      </developer>
    </developers>,
  )
)

// TODO: Use sbt-cross to workaround: https://github.com/sbt/sbt/issues/5586
lazy val codegen = project
  .settings(
    name := "scala-db-codegen",
    libraryDependencies ++= Seq(
      "org.scala-lang"        % "scala-reflect"            % scalaVersion.value,
      "org.scalatra.scalate" %% "scalate-core"             % "1.10.1",
      "org.scalameta"        %% "scalafmt-core"            % "3.8.1",
      "org.flywaydb"          % "flyway-core"              % "10.6.0",
      "org.xerial"            % "sqlite-jdbc"              % "3.44.1.0",
      "org.postgresql"        % "postgresql"               % "42.7.1",
      "mysql"                 % "mysql-connector-java"     % "8.0.33",
      "org.mariadb.jdbc"      % "mariadb-java-client"      % "3.1.2",
      "us.fatehi"             % "schemacrawler-tools"      % "16.21.1",
      "us.fatehi"             % "schemacrawler-postgresql" % "16.21.1",
      "us.fatehi"             % "schemacrawler-sqlite"     % "16.21.1",
      "us.fatehi"             % "schemacrawler-mysql"      % "16.21.1",
    ),
  )
  .cross

lazy val codegen212 = codegen("2.12.19")
lazy val codegen213 = codegen("2.13.13")

lazy val pluginSbt = project
  .settings(
    name               := "sbt-db-codegen",
    scalaVersion       := "2.12.19",
    crossScalaVersions := Seq("2.12.19"),
    sbtPlugin          := true,
    publishMavenStyle  := true,
  )
  .dependsOn(codegen212)

lazy val pluginMill = project
  .settings(
    name               := "mill-db-codegen",
    scalaVersion       := "2.13.13",
    crossScalaVersions := Seq("2.13.13"),
    libraryDependencies ++= Seq(
      "com.lihaoyi" %% "mill-main"     % "0.11.7" % Provided,
      "com.lihaoyi" %% "mill-main-api" % "0.11.7" % Provided,
      "com.lihaoyi" %% "mill-scalalib" % "0.11.7" % Provided,
    ),
  )
  .dependsOn(codegen213)
