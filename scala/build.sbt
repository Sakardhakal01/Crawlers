name := "scala_crawler"

version := "0.1"

scalaVersion := "2.11.11"

updateOptions := updateOptions.value.withCachedResolution(true)

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.5.21"
libraryDependencies += "com.ning" % "async-http-client" % "1.9.40"
libraryDependencies += "org.jsoup" % "jsoup" % "1.8.3"

resolvers += "Akka Snapshot Repository" at "http://repo.akka.io/snapshots/"
resolvers += "Typesafe Repository" at "https://dl.bintray.com/typesafe/maven-releases/"
resolvers += "Java.net Maven2 Repository" at "http://download.java.net/maven/2/"

