import Dependencies._

def crossScalacOptions(scalaVersion: String): Seq[String] = CrossVersion.partialVersion(scalaVersion) match {
  case Some((2L, scalaMajor)) if scalaMajor >= 12 =>
    Seq.empty
  case Some((2L, scalaMajor)) if scalaMajor <= 11 =>
    Seq("-Yinline-warnings")
}

lazy val deploySettings = Seq(
  sonatypeProfileName := "com.github.j5ik2o",
  publishMavenStyle := true,
  Test / publishArtifact := false,
  pomIncludeRepository := { _ => false },
  pomExtra := {
    <url>https://github.com/j5ik2o/docker-controller-scala</url>
      <licenses>
        <license>
          <name>The MIT License</name>
          <url>http://opensource.org/licenses/MIT</url>
        </license>
      </licenses>
      <scm>
        <url>git@github.com:j5ik2o/docker-controller-scala.git</url>
        <connection>scm:git:github.com/j5ik2o/docker-controller-scala</connection>
        <developerConnection>scm:git:git@github.com:j5ik2o/docker-controller-scala.git</developerConnection>
      </scm>
      <developers>
        <developer>
          <id>j5ik2o</id>
          <name>Junichi Kato</name>
        </developer>
      </developers>
  },
  publishTo := sonatypePublishToBundle.value,
  credentials := {
    val ivyCredentials = (LocalRootProject / baseDirectory).value / ".credentials"
    val gpgCredentials = (LocalRootProject / baseDirectory).value / ".gpgCredentials"
    Credentials(ivyCredentials) :: Credentials(gpgCredentials) :: Nil
  }
)

lazy val baseSettings = Seq(
  organization := "com.github.j5ik2o",
  scalaVersion := Versions.scala212Version,
  crossScalaVersions := Seq(Versions.scala212Version, Versions.scala213Version),
  scalacOptions ++= (Seq(
      "-feature",
      "-deprecation",
      "-unchecked",
      "-encoding",
      "UTF-8",
      "-language:_",
      "-Ydelambdafy:method",
      "-target:jvm-1.8"
    ) ++ crossScalacOptions(scalaVersion.value)),
  resolvers ++= Seq(
      Resolver.sonatypeRepo("snapshots"),
      Resolver.sonatypeRepo("releases"),
      "Seasar Repository" at "https://maven.seasar.org/maven2/"
    ),
  libraryDependencies ++= Seq(
      scalatest.scalatest % Test
    ),
  Test / fork := true,
  Test / parallelExecution := false,
  ThisBuild / scalafmtOnCompile := true
)

val `docker-controller-scala-core` = (project in file("docker-controller-scala-core"))
  .settings(baseSettings, deploySettings)
  .settings(
    name := "docker-controller-scala-core",
    libraryDependencies ++= Seq(
        slf4j.api,
        dockerJava.dockerJava,
        dockerJava.dockerJavaTransportJersey,
        dockerJava.dockerJavaTransportHttpclient5,
        dockerJava.dockerJavaTransportOkhttp,
        tongfei.progressbar,
        seasar.s2util,
        freemarker.freemarker,
        logback.classic % Test,
        commons.io
      ),
    libraryDependencies ++= {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2L, scalaMajor)) if scalaMajor == 13 =>
          Seq.empty
        case Some((2L, scalaMajor)) if scalaMajor == 12 =>
          Seq(
            scalaLang.scalaCollectionCompat
          )
      }
    }
  )

val `docker-controller-scala-scalatest` = (project in file("docker-controller-scala-scalatest"))
  .settings(baseSettings, deploySettings)
  .settings(
    name := "docker-controller-scala-scalatest",
    libraryDependencies ++= Seq(
        scalatest.scalatest,
        logback.classic
      )
  ).dependsOn(`docker-controller-scala-core`)

val `docker-controller-scala-dynamodb-local` = (project in file("docker-controller-scala-dynamodb-local"))
  .settings(baseSettings, deploySettings)
  .settings(
    name := "docker-controller-scala-dynamodb-local",
    libraryDependencies ++= Seq(
        scalatest.scalatest % Test,
        logback.classic     % Test,
        amazonAws.dynamodb  % Test
      )
  ).dependsOn(`docker-controller-scala-core`, `docker-controller-scala-scalatest` % Test)

val `docker-controller-scala-minio` = (project in file("docker-controller-scala-minio"))
  .settings(baseSettings, deploySettings)
  .settings(
    name := "docker-controller-scala-minio",
    libraryDependencies ++= Seq(
        scalatest.scalatest % Test,
        logback.classic     % Test,
        amazonAws.s3        % Test
      )
  ).dependsOn(`docker-controller-scala-core`, `docker-controller-scala-scalatest` % Test)

val `docker-controller-scala-zookeeper` = (project in file("docker-controller-scala-zookeeper"))
  .settings(baseSettings, deploySettings)
  .settings(
    name := "docker-controller-scala-zookeeper",
    libraryDependencies ++= Seq(
        scalatest.scalatest        % Test,
        logback.classic            % Test,
        apache.zooKeeper.zooKeeper % Test
      )
  ).dependsOn(`docker-controller-scala-core`, `docker-controller-scala-scalatest` % Test)

val `docker-controller-scala-kafka` = (project in file("docker-controller-scala-kafka"))
  .settings(baseSettings, deploySettings)
  .settings(
    name := "docker-controller-scala-kafka",
    libraryDependencies ++= Seq(
        scalatest.scalatest       % Test,
        logback.classic           % Test,
        apache.kafka.kafkaClients % Test
      )
  ).dependsOn(
    `docker-controller-scala-core`,
    `docker-controller-scala-zookeeper`,
    `docker-controller-scala-scalatest` % Test
  )

val `docker-controller-scala-mysql` = (project in file("docker-controller-scala-mysql"))
  .settings(baseSettings, deploySettings)
  .settings(
    name := "docker-controller-scala-mysql",
    libraryDependencies ++= Seq(
        scalatest.scalatest % Test,
        logback.classic     % Test,
        mysql.connectorJava % Test
      )
  ).dependsOn(`docker-controller-scala-core`, `docker-controller-scala-scalatest` % Test)

val `docker-controller-scala-elasticsearch` = (project in file("docker-controller-scala-elasticsearch"))
  .settings(baseSettings, deploySettings)
  .settings(
    name := "docker-controller-scala-elasticsearch",
    libraryDependencies ++= Seq(
        scalatest.scalatest               % Test,
        logback.classic                   % Test,
        elasticsearch.restHighLevelClient % Test
      )
  ).dependsOn(`docker-controller-scala-core`, `docker-controller-scala-scalatest` % Test)

val `docker-controller-scala-root` = (project in file("."))
  .settings(baseSettings, deploySettings)
  .settings(name := "docker-controller-scala-root")
  .aggregate(
    `docker-controller-scala-core`,
    `docker-controller-scala-scalatest`,
    `docker-controller-scala-mysql`,
    `docker-controller-scala-dynamodb-local`,
    `docker-controller-scala-minio`,
    `docker-controller-scala-zookeeper`,
    `docker-controller-scala-kafka`,
    `docker-controller-scala-elasticsearch`
  )
