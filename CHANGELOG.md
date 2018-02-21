## TBD
### Changed
* Default Housekeeping configuration is now provided via `@Bean` `housekeepingEnvironment` only if the bean is not already provided.

## [1.0.2] 2018-02-15
### Added
* Added new housekeeping configuration parameters to set the schema name and the location of the DB initialization script.

## [1.0.1] 2018-01-15
### Changed
* schema.sql file moved into test resources so that it is not added to classpath of projects that depend upon this project.
* PostConstruct Bean made conditional if Bean implementation is missing

## [1.0.0] 2018-01-05
### Added
* Configurable housekeeping entities and repositories.
* Default housekeeping entities and repository.

## [0.0.2] 2017-12-20
### Changed
* Changed HikariCP version to 2.4.13 Java 7 version for backward compatibility with older JVMs.

## [0.0.1] 2017-12-12
### Added
* Housekeeping module migrated from [circus-train](https://github.com/HotelsDotCom/circus-train) to this project.
