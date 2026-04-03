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

### Evaluation of macros

The plugin parses preprocessor directives such as #ifdef, #ifndef, #else, and #endif as flat AST nodes rather than hierarchical blocks. This approach prevents syntax tree corruption when directives interrupt standard VEX statements. A semantic evaluation layer determines whether the subsequent code blocks are active or inactive by resolving macro definitions within the current file and its included files. Code located within inactive blocks is excluded from scope analysis, ensuring that variables and functions defined there do not trigger false duplicate definition errors or appear in code completion results.

There are known limitations regarding the evaluation of complex preprocessor conditions. The plugin does not currently implement a constant expression evaluator or support recursive macro expansion. As a result, #if and #elif directives that rely on mathematical comparisons, logical operators, or the defined() function are not evaluated dynamically. In the current implementation, #if and #elif conditions are treated as unconditionally true.
