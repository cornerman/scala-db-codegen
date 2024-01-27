# sbt-quillcodegen

This is an sbt-plugin that uses the [quill-codegen-jdbc](https://zio.dev/zio-quill/code-generation/) to generate case classes and query schemas from a database schema.

Works with scala 2 and 3.

## Usage

In `project/plugins.sbt`:
```sbt
addSbtPlugin("com.github.cornerman" % "sbt-quillcodegen" % "0.1.5")
```

In `build.sbt`:
```sbt
lazy val db = project
  .enablePlugins(quillcodegen.plugin.CodegenPlugin)
  .settings(
    // The package prefix for the generated code
    quillcodegenPackagePrefix := "com.example.db",
    // The jdbc URL for the database
    quillcodegenJdbcUrl := "jdbc:...",

    // Optional database username
    // quillcodegenUsername            := None,
    // Optional database password
    // quillcodegenPassword            := None,
    // The naming parser to use, default is SnakeCaseNames
    // quillcodegenNaming              := SnakeCaseNames,
    // Whether to generate a nested extensions trait, default is false
    // quillcodegenNestedTrait         := false,
    // Whether to generate query schemas, default is true
    // quillcodegenGenerateQuerySchema := true,
    // Specify which tables to process, default is all
    // quillcodegenTableFilter         := (_ => true),
    // Strategy for unrecognized types
    // quillcodegenUnrecognizedType    := SkipColumn,
    // Map jdbc types to java/scala types
    // quillcodegenTypeMapping         := ((_, classTag) => classTag),
    // Which numeric type preference for numeric types
    // quillcodegenNumericType         := UseDefaults,
    // Timeout for the generate task
    // quillcodegenTimeout             := Duration.Inf,
    // Setup task to be executed before the code generation runs against the database
    // quillcodegenSetupTask           := {},
  )
```

### Setup database before codegen

An example for using the `quillcodegenSetupTask` to setup an sqlite database with a `schema.sql` file before the code generation runs:
```sbt
quillcodegenSetupTask := Def.taskDyn {
    IO.delete(file(quillcodegenJdbcUrl.value.stripPrefix("jdbc:sqlite:")))
    executeSqlFile(file("./schema.sql"))
}
```

The functions `executeSql` and `executeSqlFile` are provided for these kind of use-cases and use the provided jdbcUrl, username, and password.

