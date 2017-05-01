lazy val dependencies = Seq(
  "org.apache.spark" %% "spark-core" % "2.1.0",
  "org.apache.spark" %% "spark-sql" % "2.1.0",
  "com.datastax.spark" %% "spark-cassandra-connector" % "2.0.1"
)

lazy val commonSettings = Seq(
  version := "0.1",
  scalaVersion := "2.11.8"
)

lazy val TSVImport = (project in file("TSVImport"))
  .settings(
    commonSettings,
    name := "TSV Import",
    libraryDependencies ++= dependencies
  )

lazy val chiSquareCategorical = (project in file("chiSquareCategorical"))
  .settings(
    commonSettings,
    name := "Chi-square Categorical",
    libraryDependencies ++= dependencies
  )
