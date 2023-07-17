# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [Unreleased]

### Fixed

- Fix favicons transparency, cache and loading
- Fix dimensions and attributes autocompletion

### Changed

- Bump sdmx-dl from 3.0.0-beta.11 to 3.0.0-beta.12

## [2.2.6] - 2023-06-05

This is a feature release of **jdemetra-dotstat**.  
[JDemetra+ 2.2.4 or later version](https://github.com/jdemetra/jdemetra-app/releases) is required to run it.

### Added

- Add source from Statistics Canada
- Add source from El Salvador Labour Market Information System
- Add source from Statistics Luxembourg
- Add source from National Statistical Office of Thailand
- Add source favicon and default SDMX icon
- Add source from UN International Children's Emergency Fund
- Add sources from Directorates General of the European Commission
- Add source from Eurostat International trade in goods
- Add support of multi-language descriptions in sources
- Add curl backend option

### Fixed

- Migrate ABS source to the new server
- Fix obs gathering on single obs

### Changed

- Improve browsing by preloading resources
- Modify ISTAT endpoint
- Improve Eurostat driver

## [2.2.5] - 2021-09-14

This is a feature release of **jdemetra-dotstat**.  
[JDemetra+ 2.2.3 or later version](https://github.com/jdemetra/jdemetra-app/releases) is required to run it.

### Added

- Add default user-agent `sdmx-dl/version`
- Add source from Bundesbank 
- Add source from Economic and Social Commission for Asia and the Pacific 
- Add source from National Statistical Institute of Cambodia 
- Add source from SDMX Global Registry 
- Add source from Pacific Data Hub 
- Add source from UK Data Service 

## [2.2.4] - 2021-04-03

This is a feature release of **jdemetra-dotstat**.  
[JDemetra+ 2.2.3 or later version](https://github.com/jdemetra/jdemetra-app/releases) is required to run it.

### Added

- Add checksum and GPG signature on binaries
- Add persistent cache
- Add events notification
- Improve config UI
- Add support of OS truststore (SSL certificates)
- Add basic authentication
- Add storing of credentials in Windows PasswordVault
- Add source aliases
- Add support of SDMX attributes at series level
- Improve security by enforcing use of SSL when possible
- Add source from Bank for International Settlements (BIS)
- Add source from Norges Bank (NB)
- Add possibility to use custom sources

### Changed

- Minimum JDemetra+ version is now v2.2.3
- Move SDMX code to its own repository [sdmx-dl](https://github.com/nbbrd/sdmx-dl)

## [2.2.3] - 2019-01-14

This is a feature release of **jdemetra-dotstat**.  
[JDemetra+ 2.2.0 or later version](https://github.com/jdemetra/jdemetra-app/releases) is required to run it.

### Added

- Add support of system proxy selector under Windows
- Improve error reporting
- Improve logging of web queries

### Fixed

- Fix typo in NBB source description
- Fix jdk11 issues
- Fix parsing of dotstat dimension id

## [2.2.2] - 2018-04-20

This is a feature release of **jdemetra-dotstat**.  
[JDemetra+ 2.2.0 or later version](https://github.com/jdemetra/jdemetra-app/releases) is required to run it.

### Added

- Add Statistics Estonia endpoint (beta)

## [2.2.1] - 2018-03-01

This is a feature release of **jdemetra-dotstat**.  
[JDemetra+ 2.2.0 or later version](https://github.com/jdemetra/jdemetra-app/releases) is required to run it.

### Added

- Improve WB endpoint to https
- Add support of compact/generic 2.0/2.1 sdmx files

### Fixed

- Fix missing codes and dimensions in Insee driver
- Fix missing metadata when getting single ts
- Fix XXE vulnerability
- Fix IMF endpoint
- Fix ILO driver
- Fix UIS driver

## [2.2.0] - 2017-07-14

This is a feature release of **jdemetra-dotstat**.  
[JDemetra+ 2.2.0 or later version](https://github.com/jdemetra/jdemetra-app/releases) is required to run it.

### Added

- Add support of series attributes
- Add optional series label from attribute
- Add support of language priority list
- Modify definition of dimensions is now optional
- Improve error reporting
- Improve responsiveness through smarter caching

### Fixed

- Fix INSEE entry point

## [2.1.1] - 2017-03-22

This is a feature release of **jdemetra-dotstat**.  
[JDemetra+ 2.1.0 or later version](https://github.com/jdemetra/jdemetra-app/releases) is required to run it.

### Added

- Improve display name of data sources
- Add IMF SDMX Central entry point
- Update ABS entry point

### Fixed

- Fix INEGI endpoint

## [2.1.0] - 2016-08-29

This is a feature release of **jdemetra-dotstat**.  
[JDemetra+ 2.1.0 or later version](https://github.com/jdemetra/jdemetra-app/releases) is required to run it.

### Added

- Add WITS entry point
- Add use of https when available

### Fixed

- Fix names of some times series

## [2.0.3] - 2016-03-18

This is a feature release of **jdemetra-dotstat**.  
[JDemetra+ 2.0.0 or later version](https://github.com/jdemetra/jdemetra-app/releases) is required to run it.

### Added

- Add support of ESTAT large response
- Add UNDATA entry point

### Fixed

- Fix infinite timeouts
- Fix ESTAT web service update
- Fix two dots string keys

### Changed

- v2.0.2 or previous releases must be uninstalled before installing the new one

## [2.0.2] - 2016-01-15

This is a bugfix release of **jdemetra-dotstat**.  
[JDemetra+ 2.0.0 or later version](https://github.com/jdemetra/jdemetra-app/releases) is required to run it.

### Fixed

- Fix some issues with sdmx servers

## [2.0.1] - 2015-09-08

This is a feature release of **jdemetra-dotstat**.  
[JDemetra+ 2.0.0 or later version](https://github.com/jdemetra/jdemetra-app/releases) is required to run it.

### Added

- Add INSEE entry point

## [2.0.0] - 2015-07-28

This is the initial release of **jdemetra-dotstat**.  
[JDemetra+ 2.0.0 or later version](https://github.com/jdemetra/jdemetra-app/releases) is required to run it.

[Unreleased]: https://github.com/nbbrd/jdemetra-dotstat/compare/v2.2.6...HEAD
[2.2.6]: https://github.com/nbbrd/jdemetra-dotstat/compare/v2.2.5...v2.2.6
[2.2.5]: https://github.com/nbbrd/jdemetra-dotstat/compare/v2.2.4...v2.2.5
[2.2.4]: https://github.com/nbbrd/jdemetra-dotstat/compare/v2.2.3...v2.2.4
[2.2.3]: https://github.com/nbbrd/jdemetra-dotstat/compare/v2.2.2...v2.2.3
[2.2.2]: https://github.com/nbbrd/jdemetra-dotstat/compare/v2.2.1...v2.2.2
[2.2.1]: https://github.com/nbbrd/jdemetra-dotstat/compare/v2.2.0...v2.2.1
[2.2.0]: https://github.com/nbbrd/jdemetra-dotstat/compare/v2.1.1...v2.2.0
[2.1.1]: https://github.com/nbbrd/jdemetra-dotstat/compare/v2.1.0...v2.1.1
[2.1.0]: https://github.com/nbbrd/jdemetra-dotstat/compare/v2.0.3...v2.1.0
[2.0.3]: https://github.com/nbbrd/jdemetra-dotstat/compare/v2.0.2...v2.0.3
[2.0.2]: https://github.com/nbbrd/jdemetra-dotstat/compare/v2.0.1...v2.0.2
[2.0.1]: https://github.com/nbbrd/jdemetra-dotstat/compare/v2.0.0...v2.0.1
[2.0.0]: https://github.com/nbbrd/jdemetra-dotstat/releases/tag/v2.0.0
