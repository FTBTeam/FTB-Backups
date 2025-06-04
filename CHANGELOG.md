# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [21.0.1]

### Added
* Added "Notify Admins Only" server config setting
  * When true, only players with permission level >= 2, and SSP server owners, will be notified about in-progress backups

### Fixed
* Fixed issue where players on SMP could get a stuck backup progress notification on the client
* Fixed backup interval being ignored on server startup and always treated as 120 minutes, even if changed in config
* Better command output for `/ftbbackups3 time` is a backup is overdue (due to no players being online)

## [21.0.0]

### Added
* Initial release of FTB Backups 3, a major rewrite and update for Minecraft 1.21.1 of the original FTB Backups mod
  * Note that this mod is not related to FTB Backups 2 in any way
