# BSD 3-Clause License
#
# Copyright (c) 2017, Thomas Pointhuber
# All rights reserved.
#
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions are met:
#
# Redistributions of source code must retain the above copyright notice, this
#  list of conditions and the following disclaimer.
#
# Redistributions in binary form must reproduce the above copyright notice,
#  this list of conditions and the following disclaimer in the documentation
#  and/or other materials provided with the distribution.
#
# Neither the name of the copyright holder nor the names of its
#  contributors may be used to endorse or promote products derived from
#  this software without specific prior written permission.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
# AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
# IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
# DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
# FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
# DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
# SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
# CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
# OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
# OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

import os
import argparse
import sys

import mx
import mx_sulong
import mx_testsuites

_suite = mx.suite('irbuilder')

class Tool(object):
    def supports(self, language):
        return language in self.supportedLanguages

    def runTool(self, args, errorMsg=None, verbose=None, **kwargs):
        try:
            if not mx.get_opts().verbose and not verbose:
                f = open(os.devnull, 'w')
                ret = mx.run(args, out=f, err=f, **kwargs)
            else:
                f = None
                ret = mx.run(args, **kwargs)
        except SystemExit:
            ret = -1
            if errorMsg is None:
                mx.log_error()
                mx.log_error('Error: Cannot run {}'.format(args))
            else:
                mx.log_error()
                mx.log_error('Error: {}'.format(errorMsg))
                mx.log_error(' '.join(args))
        if f is not None:
            f.close()
        return ret

class LlvmAS(Tool):
    def __init__(self, supportedVersions):
        self.supportedVersions = supportedVersions

    def find_tool(self):
        return mx_sulong.findLLVMProgram('llvm-as', self.supportedVersions)

    def run(self, inputFile, flags=None):
        if flags is None:
            flags = []
        tool = self.find_tool()
        return self.runTool([tool] + flags + [inputFile], errorMsg='Cannot assemble %s with %s' % (inputFile, tool), verbose=True)

class LlvmLLI(Tool):
    def __init__(self, supportedVersions):
        self.supportedVersions = supportedVersions

    def find_tool(self):
        return mx_sulong.findLLVMProgram('lli', self.supportedVersions)

    def run(self, inputFile, flags=None):
        if flags is None:
            flags = []
        tool = self.find_tool()
        return self.runTool([tool] + flags + [inputFile], nonZeroIsFatal=False, timeout=30, errorMsg='Cannot run %s with %s' % (inputFile, tool))

LlvmAS_32 = LlvmAS(['3.2', '3.3'])
LlvmAS_38 = LlvmAS(['3.8', '3.9', '4.0', '5.0'])

LlvmLLI_32 = LlvmLLI(['3.2', '3.3'])
LlvmLLI_38 = LlvmLLI(['3.8', '3.9', '4.0', '5.0'])

def getIRWriterClasspathOptions():
    """gets the classpath of the IRWRITER distributions"""
    return mx.get_runtime_jvm_args('IRWRITER')

def runIRBuilderOut(args=None, out=None):
    """uses java-llvm-ir-builder to parse a LLVM Bitcode file and outputs it's LLVM IR"""
    vmArgs, irbuilderArgs = mx_sulong.truffle_extract_VM_args(args)
    return mx.run_java(mx_sulong.getCommonOptions(False) + vmArgs + getIRWriterClasspathOptions() + ["at.pointhi.irbuilder.irwriter.SourceParser"] + irbuilderArgs)

irBuilderTests32 = {
    'gcc_c' : ['gcc_c', "at.pointhi.irbuilder.test.GCCGeneratorSuite", os.path.join(mx_testsuites._cacheDir, 'gcc'), ['-Dsulongtest.fileExtensionFilter=.c']],
}

irBuilderTests38 = {
    'llvm' : ['llvm', "at.pointhi.irbuilder.test.LLVMGeneratorSuite", os.path.join(mx_testsuites._cacheDir, 'llvm'), []],
    'gcc_c' : ['gcc_c', "at.pointhi.irbuilder.test.GCCGeneratorSuite", os.path.join(mx_testsuites._cacheDir, 'gcc'), ['-Dsulongtest.fileExtensionFilter=.c']],
    'gcc_cpp' : ['gcc_cpp', "at.pointhi.irbuilder.test.GCCGeneratorSuite", os.path.join(mx_testsuites._cacheDir, 'gcc'), ['-Dsulongtest.fileExtensionFilter=.cpp:.C:.cc']],
    'nwcc' : ['nwcc', "at.pointhi.irbuilder.test.NWCCGeneratorSuite", os.path.join(mx_testsuites._cacheDir, 'nwcc'), []],
    'assembly' : ['assembly', "at.pointhi.irbuilder.test.InlineAssemblyGeneratorSuite", os.path.join(mx_testsuites._cacheDir, 'inlineassemblytests'), []],
}

irBuilderTestsGen38 = {
    'binary_vector' : ["at.pointhi.irbuilder.testgenerator.BinaryVectorOperatorTest", os.path.join(mx_testsuites._cacheDir, 'irbuilder', 'vector')],
    'binary_i1' : ["at.pointhi.irbuilder.testgenerator.BinaryI1Operations", os.path.join(mx_testsuites._cacheDir, 'irbuilder', 'binaryI1')],
    'binary_i1_vector' : ["at.pointhi.irbuilder.testgenerator.BinaryI1VectorOperations", os.path.join(mx_testsuites._cacheDir, 'irbuilder', 'binaryI1Vector')],
    'cast_vector' : ["at.pointhi.irbuilder.testgenerator.VectorBitcastTest", os.path.join(mx_testsuites._cacheDir, 'irbuilder', 'castVector')],
    'fibonacci' : ["at.pointhi.irbuilder.testgenerator.FibonacciFunctionCallTest", os.path.join(mx_testsuites._cacheDir, 'irbuilder', 'fibonacci')],
    'cast_integer' : ["at.pointhi.irbuilder.testgenerator.VarICasts", os.path.join(mx_testsuites._cacheDir, 'irbuilder', 'VarICasts')],
    'float_compare' : ["at.pointhi.irbuilder.testgenerator.FloatCompareOperators", os.path.join(mx_testsuites._cacheDir, 'irbuilder', 'FloatCompareOperator')],
}

def runIRBuilderTest32(vmArgs):
    """test ir-writer with llvm 3.2 bitcode files (see -h or --help)"""
    vmArgs, otherArgs = mx_sulong.truffle_extract_VM_args(vmArgs)
    parser = argparse.ArgumentParser(description="Compiles all or selected test suites.")
    parser.add_argument('suite', nargs='*', help=' '.join(irBuilderTests32.keys()), default=irBuilderTests32.keys())
    parser.add_argument('--skip-compilation', help='skip suite compilation', action='store_true')  # TODO: makefile
    parsedArgs = parser.parse_args(otherArgs)

    # test if we have the required tools installed
    LlvmAS_32.find_tool()
    LlvmLLI_32.find_tool()

    returnCode = 0
    for testSuiteName in parsedArgs.suite:
        suite = irBuilderTests32[testSuiteName]
        """runs the test suite"""
        if parsedArgs.skip_compilation is False:
            mx_sulong.ensureDragonEggExists()
            mx_sulong.mx_testsuites.compileSuite([suite[0]])
        try:
            mx_sulong.mx_testsuites.run(vmArgs + suite[3] + ['-Dpolyglot.irwriter.LLVMVersion=3.2'], suite[1], [])
        except KeyboardInterrupt:
            sys.exit(-1)
        except:
            mx.log_error("unexpected exception thrown, continue...")

        testSuite = IRGeneratorSuite(LlvmAS_32, LlvmLLI_32)
        testSuite.run(suite[2])
        if not testSuite.wasSuccessfull():
            returnCode = 1

    return returnCode

def runIRBuilderTest38(vmArgs):
    """test ir-writer with llvm 3.8 bitcode files (see -h or --help)"""
    vmArgs, otherArgs = mx_sulong.truffle_extract_VM_args(vmArgs)
    parser = argparse.ArgumentParser(description="Compiles all or selected test suites.")
    parser.add_argument('suite', nargs='*', help=' '.join(irBuilderTests38.keys()), default=irBuilderTests38.keys())
    parser.add_argument('--skip-compilation', help='skip suite compilation', action='store_true')  # TODO: makefile
    parsedArgs = parser.parse_args(otherArgs)

    # test if we have the required tools installed
    LlvmAS_38.find_tool()
    LlvmLLI_38.find_tool()

    returnCode = 0
    for testSuiteName in parsedArgs.suite:
        suite = irBuilderTests38[testSuiteName]
        """runs the test suite"""
        if parsedArgs.skip_compilation is False:
            mx_sulong.ensureDragonEggExists()
            mx_sulong.mx_testsuites.compileSuite([suite[0]])
        try:
            mx_sulong.mx_testsuites.run(vmArgs + suite[3] + ['-Dpolyglot.irwriter.LLVMVersion=3.8'], suite[1], [])
        except KeyboardInterrupt:
            sys.exit(-1)
        except:
            mx.log_error("unexpected exception thrown, continue...")

        testSuite = IRGeneratorSuite(LlvmAS_38, LlvmLLI_38)
        testSuite.run(suite[2])
        if not testSuite.wasSuccessfull():
            returnCode = 1

    return returnCode

def runIRBuilderTestGen38(vmArgs):
    """create llvm-ir testcases which are then run against llvm as well as Sulong (see -h or --help)"""
    vmArgs, otherArgs = mx_sulong.truffle_extract_VM_args(vmArgs)
    parser = argparse.ArgumentParser(description="Compiles all or selected test suites.")
    parser.add_argument('suite', nargs='*', help=' '.join(irBuilderTestsGen38.keys()), default=irBuilderTestsGen38.keys())
    parsedArgs = parser.parse_args(otherArgs)

    returnCode = 0
    for testSuiteName in parsedArgs.suite:
        suite = irBuilderTestsGen38[testSuiteName]
        """runs the test suite"""
        try:
            mx_sulong.mx_testsuites.run(vmArgs + ['-Dirwriter.LLVMVersion=3.8'], suite[0], [])
        except KeyboardInterrupt:
            sys.exit(-1)
        except:
            mx.log_error("unexpected exception thrown, continue...")

        testSuite = IRGeneratorBuilderSuite(LlvmAS_38, LlvmLLI_38)
        testSuite.run(suite[1])
        if not testSuite.wasSuccessfull():
            returnCode = 1

    return returnCode


class CompareFileResult(object):
    PASSED = 0
    FAILED = 1
    FAILED_REFERENCE = 2


def testFiles(assembler, lli, lliReference, lliFiles, sulongFiles, expectedExitVal=None):
    # test Files which need to be run with lli
    for srcFile in lliReference:
        # run file using lli
        exitVal = lli.run(srcFile)

        # test for errrors
        if expectedExitVal is None:
            if exitVal == -6 or exitVal == -11:
                # there was either a segfault or a abort
                return CompareFileResult.FAILED_REFERENCE
            expectedExitVal = exitVal
        elif expectedExitVal is not None and exitVal != expectedExitVal:
            return CompareFileResult.FAILED_REFERENCE

    for srcFile in lliFiles:
        # run file using lli
        exitVal = lli.run(srcFile)

        # test for errrors
        if expectedExitVal is None:
            expectedExitVal = exitVal
        elif expectedExitVal is not None and exitVal != expectedExitVal:
            return CompareFileResult.FAILED

    # test Files which need to be run with sulong
    for srcFile in sulongFiles:
        # assemble file if required
        if srcFile.endswith('.ll'):
            if assembler.run(srcFile) == 0:
                srcFile = srcFile[:-3] + ".bc"
            else:
                return CompareFileResult.FAILED

        # run file using sulong
        exitVal = mx_sulong.runLLVM([srcFile])

        # test for errrors
        if expectedExitVal is None:
            expectedExitVal = exitVal
        elif expectedExitVal is not None and exitVal != expectedExitVal:
            return CompareFileResult.FAILED

    return CompareFileResult.PASSED


class IRTestSuite(object):
    def __init__(self, assembler, lli):
        self.assembler = assembler
        self.lli = lli

        self.passed = []
        self.failed = []
        self.failed_references = []

    def run(self, cacheDir):
        mx.log('Testing Reassembly')
        mx.log(cacheDir)

        for root, _, files in os.walk(cacheDir):
            for fileName in files:
                inputFile = os.path.join(cacheDir, root, fileName)
                if self.isTestFile(inputFile):
                    try:
                        ret = self.invoke(inputFile)
                    except SystemExit:
                        sys.stdout.write('E')
                        sys.stdout.flush()
                        self.failed.append(inputFile)
                    else:
                        self.handleInvokeResult(inputFile, ret)

        self.printStats()

    def isTestFile(self, inputFile):
        raise NotImplementedError("this method requires to be overloaded by the child class")

    def invoke(self, inputFile):
        raise NotImplementedError("his method requires to be overloaded by the child class")

    def handleInvokeResult(self, inputFile, ret):
        if ret is CompareFileResult.PASSED:
            sys.stdout.write('.')
            sys.stdout.flush()
            self.passed.append(inputFile)
        elif ret is CompareFileResult.FAILED:
            sys.stdout.write('E')
            sys.stdout.flush()
            self.failed.append(inputFile)
        elif ret is CompareFileResult.FAILED_REFERENCE:
            sys.stdout.write('W')
            sys.stdout.flush()
            self.failed_references.append(inputFile)

    def printStats(self):
        passed_len = len(self.passed)
        failed_len = len(self.failed)
        failed_references_len = len(self.failed_references)
        total_len = failed_len + passed_len

        mx.log()

        if len(self.failed_references):
            mx.log_error('{0} compiled reference Tests failed!'.format(failed_references_len))
            for x in range(0, failed_references_len):
                mx.log_error(str(x) + ') ' + self.failed_references[x])
            mx.log()

        if failed_len != 0:
            mx.log_error('Failed {0} of {1} Tests!'.format(failed_len, total_len))
            for x in range(0, len(self.failed)):
                mx.log_error('{0}) {1}'.format(x, self.failed[x]))
        elif total_len == 0:
            mx.log_error('There is something odd with the testsuite, {0} Tests executed!'.format(total_len))
        else:
            mx.log('Passed all {0} Tests!'.format(total_len))

    def wasSuccessfull(self):
        if len(self.failed) != 0:
            return False
        elif len(self.passed) == 0:
            return False
        else:
            return True


class IRGeneratorSuite(IRTestSuite):
    def __init__(self, assembler, lli):
        IRTestSuite.__init__(self, assembler, lli)

    def isTestFile(self, inputFile):
        return inputFile.endswith('.out.ll')

    def invoke(self, inputFile):
        ref_file = inputFile[:-7] + ".bc"

        return testFiles(self.assembler, self.lli, [ref_file], [inputFile], [])


class IRGeneratorBuilderSuite(IRTestSuite):
    def __init__(self, assembler, lli):
        IRTestSuite.__init__(self, assembler, lli)

    def isTestFile(self, inputFile):
        return inputFile.endswith('.ll')

    def invoke(self, inputFile):
        return testFiles(self.assembler, self.lli, [inputFile], [], [inputFile], 0)


mx.update_commands(_suite, {
    'irbuilder-out' : [runIRBuilderOut, ''],
    'irbuilder-test32' : [runIRBuilderTest32, ''],
    'irbuilder-test38' : [runIRBuilderTest38, ''],
    'irbuilder-testgen38' : [runIRBuilderTestGen38, ''],
})

