name := "enumtest"

version := "1.0"

scalaVersion := "2.12.4"

libraryDependencies  ++=  Seq(
  "org.squeryl" %% "squeryl" % "0.9.9",
  "com.h2database" % "h2" % "1.4.196"
)
