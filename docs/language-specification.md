# CalcLox language specification

This document specifies the language accepted by the current CalcLox implementation. CalcLox is
case-sensitive and derived from Lox, but this document takes precedence over general Lox material.

## Lexical grammar

Source is a sequence of Unicode Java `char` values, but identifiers and digits use ASCII ranges.
Spaces, tabs, carriage returns, newlines, and line comments are insignificant except for error line
tracking. A line comment begins with `//` and ends before the next newline or at end of source.

```ebnf
identifier  = ( "A" … "Z" | "a" … "z" | "_" ),
              { "A" … "Z" | "a" … "z" | "_" | "0" … "9" } ;
number      = digit, { digit }, [ ".", digit, { digit } ] ;
string      = '"', { string-character | escape-pair }, '"' ;
escape-pair = "\\", any-character ;
digit       = "0" … "9" ;
```

A number must start with a digit. Thus `.5` is not a number, `1.` is tokenized as `1` followed by
`.`, and exponent notation is unsupported. Number values are parsed as Java `Double`.

Strings may span lines. The scanner preserves backslash pairs literally; it does not generally
decode escapes such as `\n` or `\\`. A backslash merely prevents the following character—especially
a quote—from ending the token. The surrounding quotes are not part of the runtime string value.

Reserved words are:

```text
and break continue define else false for fun if nil or output return true var while
```

`break` and `continue` are reserved for future use and currently have no valid grammatical
production. Punctuation and operators are:

```text
( ) { } , . - + ; / * ! != = == > >= < <=
```

The `.` token is recognized but has no valid expression syntax. Unknown characters and unterminated
strings are lexical errors.

## Syntactic grammar

The following EBNF describes valid programs. `EOF` denotes the end of the source.

```ebnf
program         = { declaration }, EOF ;

declaration     = define-declaration
                | function-declaration
                | variable-declaration
                | statement ;

define-declaration
                = "define", identifier, ";" ;
variable-declaration
                = "var", identifier, [ "=", expression ], ";" ;
function-declaration
                = "fun", identifier, "(", parameters, ")", block ;
parameters      = [ identifier, { ",", identifier } ] ;

statement       = expression-statement
                | output-statement
                | return-statement
                | if-statement
                | while-statement
                | for-statement
                | block ;
expression-statement
                = expression, ";" ;
output-statement
                = "output", expression, ";" ;
return-statement
                = "return", [ expression ], ";" ;
if-statement    = "if", "(", expression, ")", statement,
                  [ "else", statement ] ;
while-statement = "while", "(", expression, ")", statement ;
for-statement   = "for", "(",
                  ( ";" | variable-declaration | expression-statement ),
                  [ expression ], ";", [ expression ], ")", statement ;
block           = "{", { declaration }, "}" ;

expression      = assignment ;
assignment      = logic-or, [ "=", assignment ] ;
logic-or        = logic-and, { "or", logic-and } ;
logic-and       = equality, { "and", equality } ;
equality        = comparison, { ( "!=" | "==" ), comparison } ;
comparison      = term, { ( ">" | ">=" | "<" | "<=" ), term } ;
term            = factor, { ( "-" | "+" ), factor } ;
factor          = unary, { ( "/" | "*" ), unary } ;
unary           = ( "!" | "-" ), unary | call ;
call            = primary, { "(", arguments, ")" } ;
arguments       = [ expression, { ",", expression } ] ;
primary         = "true" | "false" | "nil" | number | string
                | identifier | "(", expression, ")" ;
```

An assignment's left operand must be an identifier expression. Assignment associates right-to-left;
binary and logical operators associate left-to-right. Function declarations require a block body.
The parser limits parameter and argument lists to 255 entries.

The parser currently also accepts a standalone `else statement` and executes its statement. This is
a compatibility quirk, not normative syntax; programs must attach `else` to an `if`.

## Precedence

From lowest to highest precedence:

| Level | Operators/forms | Associativity |
|---|---|---|
| Assignment | `=` | right |
| Logical OR | `or` | left, short-circuiting |
| Logical AND | `and` | left, short-circuiting |
| Equality | `==`, `!=` | left |
| Comparison | `<`, `<=`, `>`, `>=` | left |
| Additive | `+`, `-` | left |
| Multiplicative | `*`, `/` | left |
| Unary | `!`, unary `-` | right |
| Call | `(...)` | left |
| Primary | literals, identifiers, grouping | — |

## Values and operators

Runtime values are Java `Double`, `String`, `Boolean`, `null` (CalcLox `nil`), and callable values.

| Operation | Required operands | Result |
|---|---|---|
| unary `-` | number | negated number |
| `!` | any | boolean negation of truthiness |
| `-`, `*`, `/` | two numbers | number |
| `+` | two numbers or two strings | sum or concatenation |
| `<`, `<=`, `>`, `>=` | two numbers | boolean |
| `==`, `!=` | any | boolean equality result |
| `and`, `or` | any | one of the operands |

Only `nil` and `false` are falsey. All other values are truthy. Logical operators evaluate their left
operand first and skip the right operand when its result cannot affect selection. Other binary
operators evaluate left then right. Function arguments evaluate left-to-right.

Equality considers two `nil` values equal, one `nil` unequal to a non-`nil` value, and otherwise uses
same-type Java value equality. There is no implicit numeric/string conversion. Division by positive
or negative floating-point zero is an error.

## Declarations, scope, and assignment

CalcLox uses lexical scope. A block creates a child scope. A function call creates a scope for its
parameters whose enclosing scope is the function's declaration environment; functions are closures.
Lookup selects the nearest lexically visible declaration.

`var name;` initializes `name` to `nil`. A local variable cannot be read in its own initializer, and
a name cannot be declared twice in the same local scope. Shadowing a declaration in an outer scope is
valid. At global scope, redeclaration replaces the existing binding in the current implementation.
Assignment updates the resolved declaration and produces the assigned value. Reading or assigning an
undefined name is a runtime error.

`define name;` obtains a `Double` from the host frontend map and binds it in the current scope. The key
must exist; its value may be Java `null`, which becomes CalcLox `nil`. Numeric `var` initialization and
numeric assignment also put the value into the host map under the declared or assigned identifier.
This host synchronization is an embedding side effect, not a separate CalcLox value type.

## Statement execution

Statements execute in source order. `output` evaluates its expression, converts the value with
`String.valueOf`, and synchronously invokes the host callback.

`if` executes one selected branch. `while` reevaluates its condition before every iteration. `for` is
equivalent to an optional initializer followed by a `while`; its increment expression runs after the
body. An omitted condition means `true`. If present, the initializer and loop are enclosed in a new
scope. There are currently no valid `break` or `continue` statements.

A function declaration creates a callable closure at the point the declaration executes. A call
requires a callable and exactly its declared number of arguments. `return` immediately exits the
innermost function and optionally supplies a value; omitted returns and reaching the end both yield
`nil`. Returning from top-level code is a resolution error.

## Native functions

```text
clock()       -> number
eval(string)  -> any
```

`clock` returns `System.currentTimeMillis() / 1000.0`.

`eval` replaces each textual `\"` pair with `"`, scans and parses its argument as one expression,
requires the entire string to be consumed, resolves it, and evaluates it in the current interpreter.
Declarations and statements are invalid. Current resolution treats names in evaluated text as global
references, so access to locals or parameters is not specified and currently fails unless a global
binding with the same name exists. Errors originating from native `eval` type checking may report
line `-1`; parse errors inside its string use lines relative to that string.

## Host execution and errors

Every `CalcLoxRunner.run` invocation uses a fresh global environment containing `clock` and `eval`.
No language state persists between invocations. Host-map mutations and callback effects occur as the
program executes and are not rolled back if a later error occurs.

Language failures stop execution and surface as `CalcLoxRunnerError`: lexical errors, syntax errors,
resolution errors, undefined names, invalid operand types, division by zero, calls of non-functions,
wrong arity, and missing host variables. Source lines are one-based where available.

## Deliberate omissions

The current language has no classes, objects, properties, inheritance, methods, arrays, modules,
anonymous functions, modulo/exponent operators, compound assignments, increment/decrement operators,
block comments, or general-purpose string escape decoding. `break`, `continue`, and property-access
`.` are lexically reserved/recognized but unsupported syntactically.

