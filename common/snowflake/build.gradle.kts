plugins {
    id("me.champeau.jmh") version "0.7.2"
}

dependencies {
    // JMH (Java Microbenchmark Harness) for performance testing
    jmh("org.openjdk.jmh:jmh-core:1.37")
    jmh("org.openjdk.jmh:jmh-generator-annprocess:1.37")
}

jmh {
    jmhVersion = "1.37"
    warmupIterations = 3
    iterations = 5
    fork = 1
    timeUnit = "s"
    resultFormat = "JSON"
    resultsFile = project.file("${project.buildDir}/reports/jmh/results.json")
}
