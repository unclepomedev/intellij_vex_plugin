<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# intellij_vex_plugin Changelog

## [Unreleased]

### Added

* The formatter adds a newline at the end of the file in compliance with POSIX.

## [0.4.0] - 2026-03-31

### Added

* Support for resolving constant and function-like #define macros.
* Support for Houdini installation path ($HFS) to resolve standard include files.
* Support for parsing dictionary literals (`{key: value}` syntax).
* Support for context definitions.

## [0.3.0] - 2026-03-30

### Added

* Introduced type inference for VEX built-in attributes and explicit cast prefixes.

### Fixed

* Fixed parsing of function-style type casts.

## [0.2.1] - 2026-03-29

### Added

* Fixed false error highlight when a variable and function call share the same name on the same line (e.g., `float dot = dot()`).

## [0.2.0] - 2026-03-26

### Added

* Autocompletion, Code Navigation, Rename for struct
* Include directory (HOUDINI_VEX_PATH) settings and features that span multiple files
* Create a new .vfl file

## [0.1.0] - 2026-03-23

### Added

* Code Formatting
* Autocomplete
* Error and Warnings (Syntax, Type, etc.)
* Quick Documentation
* Code Navigation / Rename
* Inlay Hints
* Create a new .vex file (Custom templates can be set.)
