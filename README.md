# intellij_vex_plugin

[![Version](https://img.shields.io/jetbrains/plugin/v/30846-houdinivexassist)](https://plugins.jetbrains.com/plugin/30846-houdinivexassist)

<!-- Plugin description -->
Houdini VEX assist plugin for JetBrains IDEs (IntelliJ IDEA, PyCharm, WebStorm, etc.)

## Features

* Code Formatting
* Autocomplete
* Error and Warnings (Syntax, Type, etc.)
* Quick Documentation
* Code Navigation / Rename
* Inlay Hints
* Create new VEX files (.vfl and .vex) (Custom templates can be set.)

source code and issue tracker: https://github.com/unclepomedev/intellij_vex_plugin

<!-- Plugin description end -->

### Quick Usage

To enable multi-file features (like resolving `#include`), set your **Include path** in `Settings/Preferences > Tools > VEX`. This setting is similar to `HOUDINI_VEX_PATH` and can use the same path values.

Usage and shortcuts conform to standard JetBrains IDE behavior.

### images

* Code Formatting

![fmt_vex.gif](https://raw.githubusercontent.com/unclepomedev/intellij_vex_plugin/main/fig/fmt_vex.gif)

* Autocomplete

![fmt_vex.gif](https://raw.githubusercontent.com/unclepomedev/intellij_vex_plugin/main/fig/complete.gif)

* Error and Warnings (Syntax, Type, etc.)

![highlight.png](https://raw.githubusercontent.com/unclepomedev/intellij_vex_plugin/main/fig/highlight.png)

* Quick Documentation

![doc.png](https://raw.githubusercontent.com/unclepomedev/intellij_vex_plugin/main/fig/doc.png)

* Code Navigation / Rename

![rename.gif](https://raw.githubusercontent.com/unclepomedev/intellij_vex_plugin/main/fig/rename.gif)

* Inlay Hints

![inlay_hint.png](https://raw.githubusercontent.com/unclepomedev/intellij_vex_plugin/main/fig/inlay_hint.png)

* Create new VEX files (.vfl and .vex) (Custom templates can be set.)

![new.png](https://raw.githubusercontent.com/unclepomedev/intellij_vex_plugin/main/fig/new.png)

### Known Limitations regarding Preprocessor Directives

> - Recursive macro expansion is not performed.
> - Complex `#if` and `#elif` conditions containing mathematical or logical operators (e.g., `#if VERSION > 19`) are not fully evaluated and will default to being active (`true`).
> - However, common patterns such as `#if 0`, `#if 1`, `#if defined(MACRO)`, and `#if !defined(MACRO)` are properly evaluated to support standard dead-code blocks and macro checks.
