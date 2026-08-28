# Java library usage

## Requirements and local JAR

CalcLox requires Java 21. It is not assumed to be published to an artifact registry. Build the JAR
from a checkout:

```shell
./gradlew :calclox:jar
```

The result is `calclox/build/libs/calclox-<version>.jar`. Copy that file into your application's
repository—for example, into `libs/`—and add it as a file dependency.

Gradle Kotlin DSL:

```kotlin
dependencies {
    implementation(files("libs/calclox-<version>.jar"))
}
```

Gradle Groovy DSL:

```groovy
dependencies {
    implementation files('libs/calclox-<version>.jar')
}
```

Without a build tool, put the JAR on both the compile-time and runtime classpaths:

```shell
javac -cp libs/calclox-<version>.jar Example.java
java -cp "libs/calclox-<version>.jar:." Example
```

On Windows, replace the runtime classpath separator `:` with `;`. Use `./gradlew build` when you also
want to compile the project and run its checks.

## Minimal embedding

The host supplies a mutable `Map<String, Double>` and receives each `output` value as text.

```java
import tech.ixor.calclox.CalcLoxRunner;
import tech.ixor.calclox.CalculatorFrontend;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class Frontend implements CalculatorFrontend {
    private final Map<String, Double> variables = new HashMap<>();
    private final List<String> output = new ArrayList<>();

    @Override
    public Map<String, Double> variables() {
        return variables;
    }

    @Override
    public void output(String result) {
        output.add(result);
    }
}

Frontend frontend = new Frontend();
frontend.variables().put("principal", 1_000.0);
frontend.variables().put("rate", 0.05);

CalcLoxRunner.run(frontend, """
    define principal;
    define rate;
    var interest = principal * rate;
    output interest;
    """);
```

This emits `50.0` and adds `interest=50.0` to the host map.

## The host variable contract

`define name;` imports the current value of `name` from `variables()`. Execution fails if the map
is `null` or does not contain that key. A map entry is not automatically visible: the program must
declare it with `define` on every run in which it is used.

Numeric `var` initializers and numeric assignments are written to the host map. CalcLox values of
other types are not written. An uninitialized variable therefore creates no map entry, and assigning
a non-number does not remove a previous numeric entry. The current implementation writes any numeric
assignment by its identifier, including assignments to block locals and function parameters; avoid
reusing host-facing names for locals when that distinction matters.

The map must be mutable if a program can declare or assign numeric variables. Returning an immutable
map such as `Map.of(...)` may cause `UnsupportedOperationException`. Return `null` only when host
variables are deliberately unsupported and programs will not use `define` or numeric write-back.

## State, repeated calls, and concurrency

Each `run` creates a new interpreter. Language variables, functions, and closures do not survive the
call. The frontend map is the persistence mechanism:

```java
frontend.variables().put("balance", 100.0);

CalcLoxRunner.run(frontend,
    "define balance; balance = balance + 25;");
CalcLoxRunner.run(frontend,
    "define balance; output balance * 2;"); // emits 250.0
```

Separate calls do not share interpreter state and may use separate frontends concurrently. If calls
share a frontend, synchronization of its map and output callback is the host's responsibility.
Callbacks run synchronously on the thread that calls `run`.

## Output conversion

Every `output expression;` invokes `CalculatorFrontend.output` once with Java's
`String.valueOf(value)` representation. Common results are `12.0`, `true`, `hello`, and `null`.
CalcLox has no built-in console output; the callback decides whether to print, collect, display, or
discard the result.

## Error handling

Scanning, parsing, static resolution, and language runtime failures are reported as
`CalcLoxRunnerError`. Its line is one-based, or `-1` when no source location is available.

```java
import tech.ixor.calclox.exceptions.CalcLoxRunnerError;

try {
    CalcLoxRunner.run(frontend, source);
} catch (CalcLoxRunnerError error) {
    System.err.printf("CalcLox line %d: %s%n",
        error.getLine(), error.getMessage());
}
```

A failed run is not transactional: output already delivered and map mutations already made remain
visible. Validate or copy host state first if your application needs rollback behavior.

Passing a null frontend or source throws `IllegalArgumentException`. Exceptions thrown by host map
operations or the output callback are host exceptions and are not wrapped as `CalcLoxRunnerError`.

## Public versus implementation APIs

Most consumers need only `CalcLoxRunner`, `CalculatorFrontend`, and `CalcLoxRunnerError`. Scanner,
parser, AST, resolver, interpreter, environment, and callable classes are exposed by the Java module
but form the execution pipeline; using them directly couples an application to implementation
details and can bypass validation performed by `CalcLoxRunner`.
