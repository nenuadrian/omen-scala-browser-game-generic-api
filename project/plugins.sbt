resolvers += Resolver.url("artifactory", url("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases"))(Resolver.ivyStylePatterns).withAllowInsecureProtocol(true)

libraryDependencies += Defaults.sbtPluginExtra("com.eed3si9n" % "sbt-assembly" % "0.8.2", "0.12.0-Beta2", "2.9.2")
