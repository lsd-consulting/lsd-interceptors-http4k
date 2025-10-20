# WARP.md

This file provides guidance to WARP (warp.dev) when working with code in this repository.

## Project Overview

This is a Kotlin library that provides HTTP4K filters for intercepting HTTP requests and responses and integrating with the LSD (Live Sequence Diagrams) framework. The library creates detailed sequence diagrams by capturing HTTP interactions between services.

## Build System & Commands

This project uses Gradle with Kotlin DSL.

### Development Commands
- **Build**: `./gradlew build`
- **Run tests**: `./gradlew test`
- **Run single test**: `./gradlew test --tests "ClassName.testMethodName"`
- **Run test coverage**: `./gradlew jacocoTestReport` (reports generated in `build/reports/coverage`)
- **Clean**: `./gradlew clean`

### Publishing Commands
- **Publish to local**: `./gradlew publishToMavenLocal`
- **Generate docs**: `./gradlew dokkaGeneratePublicationJavadoc`

## Architecture

### Core Components

1. **LsdFilterProvider**: The main entry point that creates HTTP4K filters for request/response interception
   - Configurable source/target name extraction from headers
   - Supports compressed body handling
   - Customizable name normalization

2. **LsdFilter**: The actual HTTP4K Filter implementation that:
   - Captures request details (method, URI, headers, body)
   - Measures response time
   - Captures response details with status code coloring
   - Integrates with LsdContext for sequence diagram generation

3. **HeaderRenderer**: Utility for formatting HTTP headers in HTML for LSD reports

### Key Features

- **Header-based Service Identification**: Uses `User-Agent` for source and `Host` for target by default
- **Body Content Handling**: Supports text-based content types (text, JSON, XML, YAML) and handles compressed content
- **Status Code Visualization**: Color-codes responses (green=2xx, purple=3xx, orange=4xx, red=5xx)
- **Duration Tracking**: Measures and displays response times

### Integration Points

- Depends on `lsd-core` library for sequence diagram generation
- Integrates with HTTP4K's Filter chain mechanism
- Uses LsdContext.instance for capturing sequence events

## Test Configuration

Tests use JUnit 5 and AssertJ. Test configuration in `src/test/resources/lsd.properties`:
- Deterministic IDs for consistent test results
- Metrics enabled for performance monitoring

## Dependencies

- **lsd-core**: Core LSD framework for sequence diagrams
- **http4k-core**: HTTP4K core library
- **JUnit 5**: Testing framework
- **AssertJ**: Assertion library

## Usage Pattern

The typical integration follows this pattern:
1. Obtain LsdContext instance
2. Create LsdFilterProvider with the context
3. Apply the filter to HTTP4K handler chain
4. Make HTTP requests through the filtered chain
5. Complete scenarios and generate reports using LsdContext