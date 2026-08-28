# CalcLox language guide

CalcLox uses semicolon-terminated statements and brace-delimited blocks. Comments start with `//`
and continue to the end of the line.

```text
// Values supplied by the Java host must be imported explicitly.
define price;
define quantity;

var subtotal = price * quantity;
output subtotal;
```

## Values and expressions

CalcLox values are numbers, strings, booleans, `nil`, and functions. All numbers are double-precision
floating-point values, so output normally includes a decimal suffix such as `3.0`.

```text
output 2 + 3 * 4;          // 14.0
output (2 + 3) * 4;        // 20.0
output "calc" + "lox";    // calclox
output 5 >= 3;             // true
output nil == nil;         // true
```

`+` adds two numbers or concatenates two strings; it does not convert between types. Arithmetic and
ordering operators require numbers. Equality works for every value type. Division by zero is a
runtime error.

Only `false` and `nil` are falsey; numbers (including zero), strings (including the empty string),
and functions are truthy. `!` negates truthiness. `and` and `or` short-circuit and return an operand,
not necessarily a boolean:

```text
output nil or "fallback";  // fallback
output true and 42;        // 42.0
```

## Variables and host inputs

`var` declares a language variable. Without an initializer its value is `nil`.

```text
var count = 1;
count = count + 1;
var pending;
output pending;            // null
```

`define` declares a variable whose numeric value is read from the Java frontend map:

```text
define taxRate;
var total = 100 * (1 + taxRate);
```

Blocks introduce lexical scope. Inner variables may shadow outer ones, and functions capture their
declaration environment. Declaring the same name twice in one local scope is an error.

## Conditional execution

Parentheses around conditions are required. The branches may be a single statement or a block.

```text
if (score >= 50) {
  output "pass";
} else {
  output "retry";
}
```

## Loops

`while` repeats while its condition is truthy:

```text
var n = 3;
while (n > 0) {
  output n;
  n = n - 1;
}
```

`for` has initializer, condition, and increment clauses. Each clause is optional. A `var`
initializer is scoped to the loop.

```text
for (var i = 0; i < 3; i = i + 1) {
  output i;
}
```

Although `break` and `continue` are reserved words, loop-control statements are not implemented in
the current language and using either word is a syntax error.

## Functions and closures

Functions have fixed arity and may accept at most 255 parameters. Calls may supply at most 255
arguments, and the argument count must match exactly. Falling off the end returns `nil`.

```text
fun power2(value) {
  return value * value;
}

fun makeAdder(amount) {
  fun add(value) {
    return value + amount;
  }
  return add;
}

var addTax = makeAdder(0.13);
output power2(4);
output addTax(10);
```

`return` is valid only inside a function. Functions are values and can be stored, returned, and
called through variables. There are no anonymous function expressions.

## Native functions

`clock()` takes no arguments and returns Unix time in seconds as a number.

`eval(text)` parses and evaluates exactly one expression in the current interpreter. It cannot
evaluate declarations or statements:

```text
var amount = 10;
output eval("amount * 1.5"); // 15.0 at top level
```

The argument must be a string. In the current implementation, evaluated name references resolve as
globals; do not rely on `eval` to access function parameters or block-local variables. An escaped
quote (`\"`) in the string is converted to a quote before the expression is scanned.

## Complete example

```text
define principal;
define annualRate;
define years;

fun compound(balance, rate, periods) {
  var i = 0;
  while (i < periods) {
    balance = balance * (1 + rate);
    i = i + 1;
  }
  return balance;
}

var finalBalance = compound(principal, annualRate, years);
output finalBalance;
```

