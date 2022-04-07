name := "omen"

version := "0.1"

scalaVersion := "2.12.8"

resolvers += "Artima Maven Repository" at "https://repo.artima.com/releases"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.5.21"
libraryDependencies += "com.typesafe.akka" %% "akka-http" % "10.1.10"
libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.5.19"
libraryDependencies += "com.typesafe.akka" %% "akka-http-xml" % "10.1.10"
libraryDependencies += "com.typesafe.akka" %% "akka-http-spray-json" % "10.1.10"
libraryDependencies += "org.apache.logging.log4j" % "log4j-api" % "2.11.2"
libraryDependencies += "org.apache.logging.log4j" % "log4j-core" % "2.11.2"
libraryDependencies += "org.apache.logging.log4j" %% "log4j-api-scala" % "11.0"
libraryDependencies += "org.scala-graph" % "graph-core_2.12" % "1.13.1"
libraryDependencies += "com.hazelcast" % "hazelcast" % "4.2"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream-testkit" % "2.5.23",
  "com.typesafe.akka" %% "akka-http-testkit" % "10.1.10",
  "org.slf4j" % "slf4j-nop" % "1.6.4",
  "mysql" % "mysql-connector-java" % "5.1.24",
  "org.apache.commons" % "commons-dbcp2" % "2.0.1",

)
libraryDependencies += "jep" % "jep" % "2.24"
libraryDependencies += "com.github.t3hnar" %% "scala-bcrypt" % "4.1"

libraryDependencies += "com.lightbend.akka" %% "akka-stream-alpakka-slick" % "1.1.2"
libraryDependencies += "com.h2database" % "h2" % "1.3.148"
libraryDependencies += "org.mariuszgromada.math" % "MathParser.org-mXparser" % "4.3.3"

libraryDependencies += "org.scalactic" %% "scalactic" % "3.0.8"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.8" % "test"
libraryDependencies += "org.neo4j" % "neo4j-community" % "4.4.5"
libraryDependencies += "net.jcazevedo" %% "moultingyaml" % "0.4.0"
libraryDependencies += "commons-io" % "commons-io" % "2.6"
libraryDependencies += "org.mongodb" % "mongodb-driver-sync" % "4.6.0-alpha0"

 