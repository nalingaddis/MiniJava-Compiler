# MiniJava-Compiler
The final assignment for my Compilers course, COMP 520. This compiler for a subset of the Java language includes the core aspects of any compiler: scanning/parsing, abstract syntax analysis, identification/type checking, and assembly code generation (generated for mJAM).

## MiniJava Capabilities
* Class Declaration
* Field/Method Declaration
  * Visibility & Access Modifiers
  * `int`, `bool`, and class types
* Object references
* Complex statement and expression declarations

## Testing It Out
1. Compile and run `src/miniJava/Compiler.java`
2. Provide the file `Demo.java` as a command line argument
3. Change the boolean flags in `src/miniJava/Compiler.java` to see debugging capabilities
  * `DEBUG_TOOLS` compiler with debug symbols
  * `DISPLAY_AST` with print the abstract syntax tree to the console
  * `DEBUG` will execute `Demo.java` in debug mode
