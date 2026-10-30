lazy val root = (project in file("."))
    .enablePlugins(ScalusBlueprintPlugin)
    .settings(
      name := "vesting-validator",
      scalaVersion := "3.9.0",
      scalacOptions ++= Seq("-deprecation", "-feature"),
      libraryDependencies ++= Seq(
        "org.scalus" %% "scalus" % "1.4.0",
        "org.scalus" %% "scalus-cardano-ledger" % "1.4.0",
        "org.scalus" %% "scalus-testkit" % "1.4.0" % Test,
        "org.scalatest" %% "scalatest" % "3.2.20" % Test
      ),
      addCompilerPlugin(
        ("org.scalus" % "scalus-plugin" % "1.4.0").cross(CrossVersion.full)
      )
    )
