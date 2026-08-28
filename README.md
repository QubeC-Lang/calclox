# CalcLox

CalcLox is a standalone Java 21 library for embedding a small, calculator-oriented scripting
language. It supports variables, arithmetic and logical expressions, control flow, functions,
host-provided numeric values, and output callbacks.

The language is a simplified version of [Lox](ttps://craftinginterpreters.com/the-lox-language.html), without some features like classes and inheritance, and is designed to be used in a calculator application.

## Build

```shell
./gradlew build
```

The published module coordinates are `tech.ixor:calclox:<version>`.

## Use

Implement `CalculatorFrontend` to expose mutable numeric variables and collect output, then pass
it and a program to `CalcLoxRunner.run`:

```java
import tech.ixor.calclox.CalcLoxRunner;
import tech.ixor.calclox.CalculatorFrontend;

import java.util.HashMap;
import java.util.Map;

Map<String, Double> variables = new HashMap<>();
variables.put("principal", 100.0);

CalculatorFrontend frontend = new CalculatorFrontend() {
    public Map<String, Double> variables() { return variables; }
    public void output(String result) { System.out.println(result); }
};

CalcLoxRunner.run(frontend, """
    define principal;
    var doubled = principal * 2;
    output doubled;
    """);
```

`define` imports an existing host variable. Numeric declarations and assignments are written
back to the frontend map. Each call uses an isolated interpreter. Invalid source or runtime
failures throw `CalcLoxRunnerError`, whose `getLine()` method identifies the source line.

## Language example

```text
fun square(value) {
  return value * value;
}

for (var i = 1; i <= 3; i = i + 1) {
  output square(i);
}
```

CalcLox intentionally omits classes and inheritance. The built-in `clock()` returns Unix time in
seconds, and `eval(string)` evaluates a CalcLox expression in the current program environment.

## Resources and credits

- [Crafting Interpreters](https://craftinginterpreters.com/) by Bob Nystrom. This book introduces the [Lox language](https://craftinginterpreters.com/the-lox-language.html). This book also contains [a guide for building a tree-walk interpreter](https://craftinginterpreters.com/a-tree-walk-interpreter.html), which was used as a reference for building the interpreter for `calcLox`.
