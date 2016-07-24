# asdl-tools
This project is a compiler and code generator for a dialect of Abstract Syntax Description Language (ASDL).
The compiler reads ASDL and writes Java class definitions that support processing of data using the Visitor pattern.

## Motivation
The motivation for the work is to explore (in Java) the abstract syntax tree (AST) of Python.
An AST is used as an intermediate representation of the input program inside a compiler.
Python is unusual in that the mechanism is made available to Python programs through the `ast` module in the standard library.
Any string of Python source code can be converted to its AST.
The set of data types used in the Python AST is specified using ASDL.
This ASDL is compiled by a Python program `asdl-c.py`, during the building of CPython, and used to generate the C data structures that become the implementation of the `ast`modules.
In Jython (the Java implementation of Python), a modified version of the CPython `asdl-c.py`is used to generate Java.
The aim here is to use Java and modern compiler and code generation tools (ANTLR and StringTemplate) to accomplish something similar.

## About ASDL
ASDL is described in "The Zephyr Abstract Syntax Description Language" [TR-554-97](https://www.cs.princeton.edu/research/techreps/TR-554-97) Wang, et al..
There are tools in ML at [SourceForge ASDL](http://asdl.sourceforge.net/), but at the time of this writing, the project has seen no changes since August 2002.

## Dialect of ASDL
In applying ASDL to describing Python, the Python project made some additions to ASDL, and also omitted some features.
Since the primary aim is to generate the Python AST, this project follows suit.
These differences are:
 * Code generation is not controlled through the ASDL source files (`view` keyword). We'll be using StringTemplate.
 * The `attributes` keyword is allowed for "product" types, not just for "sum" types.
 * Importing of one module by another is not supported.

