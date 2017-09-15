# CHANGELOG
Currently on [version 1.4.0](https://github.com/sumeet-bansal/lighthouse/releases/tag/v1.4).

## [Unreleased](https://github.com/sumeet-bansal/lighthouse/compare/v1.4...master)
### Added or Changed
+ any requested features

## [Lighthouse v1.4.0](https://github.com/sumeet-bansal/lighthouse/compare/v1.3...v1.4)
Complete database rehaul and [full feature release](https://github.com/sumeet-bansal/lighthouse/releases/tag/v1.4).
### Added
+ support for SQLite

### Changed
+ complete migration from MongoDB to SQLite
+ significant performance improvements to batch operations within the database module
+ significant performance improvements to CSV write speeds
+ improved application structure and organization
+ improved error handling for incompatible directory structures
+ updated and improved unit tests
+ updated outdated dependencies

### Fixed
+ incorrect CSV formatting for rows with key discrepancies (i.e. missing properties on either side)

### Removed
- support for MongoDB

## [Lighthouse v1.3.0](https://github.com/sumeet-bansal/lighthouse/compare/95e1f2a564d074e742faaf6ed5794dcbb5e0b570...v1.3) (Sept 06, 2017)
[Full feature release](https://github.com/sumeet-bansal/lighthouse/releases/tag/v1.3).
### Added
+ added ability to mark properties to be ignored during queries
+ `ignore` command to edit the database properties directly
+ support for files with non-standard paths (e.g. `.ignore` files)
+ added automatic yes flags (`-y`/`--yes`) for database clearing
+ support for quotation marks and statement separators in commands
+ [a guide to adding parsers](https://github.com/sumeet-bansal/lighthouse/blob/master/src/main/java/parser/ParserGuide.md)

### Changed
+ minor aesthetic changes to the splash page and main prompt
+ bug fixes for untested infinite loops
+ better internal statistics tracking
+ restructured packages to match modular architecture

## [Lighthouse v1.2.3](https://github.com/sumeet-bansal/lighthouse/compare/baba13511abcfadf8fa5339bf978c00bab3cb01b...95e1f2a564d074e742faaf6ed5794dcbb5e0b570) (Aug 29, 2017)
### Changed
+ significant performance boosts to the query module
+ more versatile `find` and `grep` tools

### Fixed
+ patched duplicating property issue

## [Lighthouse v1.2.2](https://github.com/sumeet-bansal/lighthouse/compare/1bb5752131993f20ab999853c4535b59decddfae...baba13511abcfadf8fa5339bf978c00bab3cb01b) (Aug 25, 2017)
### Changed
+ significant performance improvements to the database module
+ a more versatile `list` tool with specific support for viewing specific branches at various depths

## [Lighthouse v1.2.1](https://github.com/sumeet-bansal/lighthouse/compare/v1.2...1bb5752131993f20ab999853c4535b59decddfae) (Aug 18, 2017)
### Changed
+ improved performance when switching modules
+ improved application structure and organization

## [Lighthouse v1.2.0](https://github.com/sumeet-bansal/lighthouse/compare/v1.1...v1.2) (Aug 15, 2017)
Rebrand and [full feature release](https://github.com/sumeet-bansal/lighthouse/releases/tag/v1.2).
### Added
+ greater support for more file types
	+ .INFO
	+ .whitelist
	+ .blacklist
	+ .keyring
	+ .gateway
+ full extraction tool spin-off ([crawler](https://github.com/sumeet-bansal/crawler))
	+ greater ZooKeeper suppport
	+ VM dependency extraction
+ query support for extension wildcards
+ full \*.extension support

### Changed
+ updated as a fully-fledged command-line application
+ rebranded Alcatraz Diagnostic Suite (ADS) as Lighthouse

## [Alcatraz Diagnostic Suite v1.1](https://github.com/sumeet-bansal/lighthouse/compare/v1.0...v1.1) (Aug 03, 2017)
[Full feature release](https://github.com/sumeet-bansal/lighthouse/releases/tag/v1.1).
### Added
+ XML parsing (beta)
+ internal queries (e.g. all nodes within a fabric)
+ `exclude` command
+ `grep` and `find` commands

## [Alcatraz Diagnostic Suite v1.0](https://github.com/sumeet-bansal/lighthouse/compare/v0.9...v1.0) (Jul 28, 2017)
[Official stable release](https://github.com/sumeet-bansal/lighthouse/releases/tag/v1.0), as presented to Kailash.

## [Alcatraz Diagnostic Suite v0.9](https://github.com/sumeet-bansal/lighthouse/compare/bb5afa82be4c205f298cd6d9f9223d74ac5a13e7...v0.9) (Jul 25, 2017)
[Rough demo version](https://github.com/sumeet-bansal/lighthouse/releases/tag/v0.9), as presented to Herb and Alan.