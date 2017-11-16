# java-llvm-ir-builder
Create LLVM IR using Java

[![Build Status](https://travis-ci.org/pointhi/java-llvm-ir-builder.svg?branch=master)](https://travis-ci.org/pointhi/java-llvm-ir-builder)
[![Code Climate](https://codeclimate.com/github/pointhi/java-llvm-ir-builder/badges/gpa.svg)](https://codeclimate.com/github/pointhi/java-llvm-ir-builder)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/2b33c63bf7b147ecb07e28d7a86fb574)](https://www.codacy.com/app/pointhi/java-llvm-ir-builder?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=pointhi/java-llvm-ir-builder&amp;utm_campaign=Badge_Grade)

## Setup Enviroment

Create some dir where you put the projects in

```bash
mkdir java-llvm-ir-builder-dev
cd java-llvm-ir-builder-dev
```

Download mx

```bash
git clone https://github.com/graalvm/mx.git
export PATH="mx:$PATH"
```

Use git clone to download this project

```bash
git clone https://github.com/pointhi/java-llvm-ir-builder
```

Enter directory and build project

```bash
cd java-llvm-ir-builder
mx build
```

**TODO: there are some parts missing in this setup manual, regarding dependencies**

## Features

By compiling this project, we get some new mx commands to work with:

```bash
mx irbuilder-out path_to_file.bc # output the .ll equivalent ir as it's parsed by Sulong
```

```bash
mx irbuilder-test32 # Run LLVM 3.2 compatible irwriter tests (compile testsuite and test the written output)
```

```bash
mx irbuilder-test38 # Run LLVM 3.8 compatible irwriter tests (compile testsuite and test the written output)
```

```bash
mx irbuilder-testgen38 # Create LLVM 3.8 compatible LLVM-IR Testcases from scratch and use them to test Sulong
```
