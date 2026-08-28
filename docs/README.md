# CalcLox documentation

CalcLox is a small Lox-derived language intended to be embedded in a Java calculator or other
numeric application. Start with the document that matches what you are building:

- [Java library usage](library-usage.md) — add the dependency, implement the host interface,
  execute programs, persist numeric values, and handle failures.
- [Language guide](language-guide.md) — learn CalcLox through examples.
- [Language specification](language-specification.md) — lexical grammar, syntax, precedence,
  types, scope, runtime semantics, and implementation limits.

The public entry point is `CalcLoxRunner.run(CalculatorFrontend, String)`. A run scans, parses,
resolves, and executes one complete source string with a fresh interpreter.

