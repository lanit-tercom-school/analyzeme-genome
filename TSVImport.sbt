name := "TSV Import"
 
version := "1.0"
 
scalaVersion := "2.11.8"
 
libraryDependencies += "org.apache.spark" %% "spark-core" % "2.1.0"
libraryDependencies += "org.apache.spark" %% "spark-sql" % "2.1.0"
libraryDependencies += "com.datastax.spark" %% "spark-cassandra-connector" % "2.0.1"