lazy val akkaHttpVersion      = "10.1.7"
lazy val akkaVersion          = "2.5.19"
lazy val reactiveMongoVersion = "0.13.0"

organization := "de.kddc"
scalaVersion := "2.12.7"
name         := "mybench"

libraryDependencies ++= Seq(
  "com.typesafe.akka"        %% "akka-http"                % akkaHttpVersion,
  "com.typesafe.akka"        %% "akka-http-spray-json"     % akkaHttpVersion,
  "com.typesafe.akka"        %% "akka-stream"              % akkaVersion,
  "com.typesafe.akka"        %% "akka-http-testkit"        % akkaHttpVersion % Test,
  "com.typesafe.akka"        %% "akka-testkit"             % akkaVersion     % Test,
  "com.typesafe.akka"        %% "akka-stream-testkit"      % akkaVersion     % Test,
  "org.scalatest"            %% "scalatest"                % "3.0.5"         % Test,
  "com.softwaremill.macwire" %% "macros"                   % "2.3.1"         % Provided,
  "org.reactivemongo"        %% "reactivemongo"            % reactiveMongoVersion,
  "org.reactivemongo"        %% "reactivemongo-akkastream" % reactiveMongoVersion
)
