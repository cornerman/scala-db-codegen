Global / onChangedBuildSource := ReloadOnSourceChanges

inThisBuild(
  Seq(
    organization       := "com.github.cornerman",
    scalaVersion       := "2.12.12",
    crossScalaVersions := Seq("2.12.12"),
    licenses           := Seq("MIT License" -> url("https://opensource.org/licenses/MIT")),
    homepage           := Some(url("https://github.com/cornerman/sbt-quillcodegen")),
    scmInfo := Some(
      ScmInfo(
        url("https://github.com/cornerman/sbt-quillcodegen"),
        "scm:git:git@github.com:cornerman/sbt-quillcodegen.git",
        Some("scm:git:git@github.com:cornerman/sbt-quillcodegen.git"),
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

lazy val codegen = project
  .settings(
    name               := "quillcodegen",
    crossScalaVersions := Seq("2.12.12", "2.13.12"),
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-reflect" % scalaVersion.value,
      // Should be same as in Codegen.scala for generated code
      "io.getquill"     %% "quill-codegen-jdbc"   % "4.8.1",
      "org.xerial"       % "sqlite-jdbc"          % "3.44.1.0",
      "org.postgresql"   % "postgresql"           % "42.7.1",
      "mysql"            % "mysql-connector-java" % "8.0.33",
      "org.mariadb.jdbc" % "mariadb-java-client"  % "3.1.2",
      "org.mybatis"      % "mybatis"              % "3.5.15",
    ),
  )

lazy val codegenPlugin = project
  .settings(
    name              := "sbt-quillcodegen",
    sbtPlugin         := true,
    publishMavenStyle := true,
  )
  .dependsOn(codegen)
