# Changelog
All notable changes to this project will be documented in this file.

This project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## Unreleased [1.0] - 2023-02-01
### Added
- minimal implementation of [Mustache specs](https://github.com/mustache/spec)
  - compilation of template from **String**
  - complete implementation of core modules
  - complete implementation of **lambdas** optional module
  - complete implementation of **dynamic-names** optional module
  - complete implementation of **inheritance** optional module
- rendering to **String** using various data sources
  - JSON text using kotlinx.serialization
  - descent in Kotlin **Map** and **List** instances
  - reflection on Kotlin classes
  - user-defined sources
- management of named templates
  - templates retrieved from a directory
  - templates compiled from strings
  - ad-hoc resolution with *(String) -> Template* function
- kotlin multiplatform build
  - jvm
  - osxArm
