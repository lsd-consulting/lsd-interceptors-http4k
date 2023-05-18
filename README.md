[![semantic-release](https://img.shields.io/badge/semantic-release-e10079.svg?logo=semantic-release)](https://github.com/semantic-release/semantic-release)

# lsd-interceptors-http4k

[![Build](https://github.com/lsd-consulting/lsd-interceptors-http4k/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/lsd-consulting/lsd-interceptors-http4k/actions/workflows/ci.yml)
[![Nightly Build](https://github.com/lsd-consulting/lsd-interceptors-http4k/actions/workflows/nightly.yml/badge.svg)](https://github.com/lsd-consulting/lsd-interceptors-http4k/actions/workflows/nightly.yml)
[![GitHub release](https://img.shields.io/github/release/lsd-consulting/lsd-interceptors-http4k)](https://github.com/lsd-consulting/lsd-interceptors-http4k/releases)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.lsd-consulting/lsd-interceptors-http4k.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22io.github.lsd-consulting%22%20AND%20a:%22lsd-interceptors-http4k%22)

Provides a Http4k Filter for intercepting http requests and responses and adding the details to the provided lsdContext instance.

----
## Usage

- Add dependencies

```groovy
    implementation 'io.github.lsd-consulting:lsd-core:<version>'
    implementation 'io.github.lsd-consulting:lsd-interceptors-http4k:<version>'
```

- Configure an interceptor with an LsdContext

```kotlin
    // Obtain an instance of an LsdContext 
    private val lsd = LsdContext.instance

    // Pass the lsdContext instance to the filter provider (along with any additional options)
    private val lsdFilter = LsdFilterProvider(lsd).filter
```

- Include the filter in the filter chain for your application handler e.g.

```kotlin
    val app = lsdFilter.then(appHandler)
```

- Invoke your application with requests, e.g. via tests
  - By default `User-Agent` header is used to determine the source participant name
  - By default `Host` header is used to determine the target participant name
  - You can override these when instantiating the LsdFilterProvider class by overriding the `sourceNameProvider` or `targetNameProvider` to use alternative headers or anything else available in the `Request` 

- Generate the report:
```kotlin
// You can capture multiple scenarios within a report
lsd.completeScenario("scenario title")

// Finally created the report
lsd.completeReport("report title")
```

For a contrived example, see file `ExampleWithReportGenerated.kt` which was used to generate the below report. Each arrow can be clicked to reveal the headers and body of each request and response:

![example.png](docs%2Fexample.png)

---
See  [lsd-core](https://github.com/lsd-consulting/lsd-core) project for further details on how the reports with sequence diagrams can be generated.
