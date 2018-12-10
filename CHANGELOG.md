## TBD
### Added
* vacuum-tool: Added validation of configured tables, see vacuum-tool/README.md: tables-validation. 
### Changed
* Added db name and table name columns to housekeeping tables. See [#30](https://github.com/HotelsDotCom/housekeeping/issues/30).
* Changed default max-active connection pool size from 50 to 2 as it was unnecessarily large and having multiple housekeeping jobs was causing RDS issues (too many open connections).

## [2.1.0] - 2018-10-05
### Changed
* Refactored general metastore tunnelling code to leverage hcommon-hive-metastore libraries. See [#23](https://github.com/HotelsDotCom/housekeeping/issues/23).
* Upgraded hotels-oss-parent to 2.3.3 (was 2.0.6).
* Packaging vaccuum-tool results in a jar with a new name: housekeeping-vacuum-tool-all-latest.jar.
* Removed the hcommon-hive-metastore jar from the lib folder and shaded it into housekeeping-vacuum-tool.

## [2.0.0] - 2018-06-22
### Changed
* Refactored module to include the Vacuum Tool (was previously part of [Circus Train](https://github.com/HotelsDotCom/circus-train)). See [#17](https://github.com/HotelsDotCom/housekeeping/issues/17).

## [1.0.4] - 2018-04-25
### Changed
* Default Housekeeping configuration is now provided via `@Bean` `housekeepingEnvironment` only if the bean is not already provided.
### Fixed
* Fixed issue where Housekeeping was failing when database contained a path which no longer exists. See [#18](https://github.com/HotelsDotCom/housekeeping/issues/18).

## [1.0.2] - 2018-02-15
### Added
* Added new housekeeping configuration parameters to set the schema name and the location of the DB initialization script.

## [1.0.1] - 2018-01-15
### Changed
* schema.sql file moved into test resources so that it is not added to classpath of projects that depend upon this project.
* PostConstruct Bean made conditional if Bean implementation is missing.

## [1.0.0] - 2018-01-05
### Added
* Configurable housekeeping entities and repositories.
* Default housekeeping entities and repository.

## [0.0.2] - 2017-12-20
### Changed
* Changed HikariCP version to 2.4.13 Java 7 version for backward compatibility with older JVMs.

## [0.0.1] - 2017-12-12
### Added
* Housekeeping module migrated from [circus-train](https://github.com/HotelsDotCom/circus-train) to this project.
