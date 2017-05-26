import os
import argparse
import sys
from enum import Enum

from mx_irbuilder_util import TemporaryEnv

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

    def run(self, inputFile, flags=None):
        if flags is None:
            flags = []
        tool = mx_sulong.findLLVMProgram('llvm-as', self.supportedVersions)
        return self.runTool([tool] + flags + [inputFile], errorMsg='Cannot assemble %s with %s' % (inputFile, tool), verbose=True)

class LlvmLLI(Tool):
    def __init__(self, supportedVersions):
        self.supportedVersions = supportedVersions

    def run(self, inputFile, flags=None):
        if flags is None:
            flags = []
        tool = mx_sulong.findLLVMProgram('lli', self.supportedVersions)
        return self.runTool([tool] + flags + [inputFile], nonZeroIsFatal=False, timeout=30, errorMsg='Cannot run %s with %s' % (inputFile, tool))

LlvmAS_32 = LlvmAS(['3.2', '3.3'])
LlvmAS_38 = LlvmAS(['3.8', '3.9', '4.0'])

LlvmLLI_32 = LlvmLLI(['3.2', '3.3'])
LlvmLLI_38 = LlvmLLI(['3.8', '3.9', '4.0'])

def getIRWriterClasspathOptions():
    """gets the classpath of the IRWRITER distributions"""
    return mx.get_runtime_jvm_args('IRWRITER')

def runIRBuilderOut(args=None, out=None):
    return mx.run_java(getIRWriterClasspathOptions() + ["at.pointhi.irbuilder.irwriter.SourceParser"] + args)

irBuilderTests32 = {
    'sulong' : ['sulong', "at.pointhi.irbuilder.test.SulongGeneratorSuite", os.path.join(mx_testsuites._cacheDir, 'sulong')],
    'llvm' : ['llvm', "at.pointhi.irbuilder.test.LLVMGeneratorSuite", os.path.join(mx_testsuites._cacheDir, 'llvm')],
    'gcc' : ['gcc', "at.pointhi.irbuilder.test.GCCGeneratorSuite", os.path.join(mx_testsuites._cacheDir, 'gcc')],
    'nwcc' : ['nwcc', "at.pointhi.irbuilder.test.NWCCGeneratorSuite", os.path.join(mx_testsuites._cacheDir, 'nwcc')],
    'assembly' : ['assembly', "at.pointhi.irbuilder.test.InlineAssemblyGeneratorSuite", os.path.join(mx_testsuites._cacheDir, 'inlineassemblytests')],
}

irBuilderTests38 = {
    'sulong' : ['sulong38', "at.pointhi.irbuilder.test.SulongGeneratorSuite", os.path.join(mx_testsuites._cacheDir, 'sulong')],
    'sulongcpp' : ['sulongcpp38', "at.pointhi.irbuilder.test.SulongCPPGeneratorSuite", os.path.join(mx_testsuites._cacheDir, 'sulongcpp')],
    'llvm' : ['llvm38', "at.pointhi.irbuilder.test.LLVMGeneratorSuite", os.path.join(mx_testsuites._cacheDir, 'llvm')],
    'gcc' : ['gcc38', "at.pointhi.irbuilder.test.GCCGeneratorSuite", os.path.join(mx_testsuites._cacheDir, 'gcc')],
    'nwcc' : ['nwcc38', "at.pointhi.irbuilder.test.NWCCGeneratorSuite", os.path.join(mx_testsuites._cacheDir, 'nwcc')],
}

irBuilderTestsGen38 = {
    'binary_vector' : ["at.pointhi.irbuilder.testgenerator.BinaryVectorOperatorTest", os.path.join(mx_testsuites._cacheDir, 'irbuilder/vector')],
    'binary_i1' : ["at.pointhi.irbuilder.testgenerator.BinaryI1Operations", os.path.join(mx_testsuites._cacheDir, 'irbuilder/binaryI1')],
    'binary_i1_vector' : ["at.pointhi.irbuilder.testgenerator.BinaryI1VectorOperations", os.path.join(mx_testsuites._cacheDir, 'irbuilder/binaryI1Vector')],
}

def runIRBuilderTest32(vmArgs):
    vmArgs, otherArgs = mx_sulong.truffle_extract_VM_args(vmArgs)
    parser = argparse.ArgumentParser(description="Compiles all or selected test suites.")
    parser.add_argument('suite', nargs='*', help=' '.join(irBuilderTests32.keys()), default=irBuilderTests32.keys())
    parser.add_argument('--skip-compilation', help='skip suite compilation', action='store_true')  # TODO: makefile
    parsedArgs = parser.parse_args(otherArgs)

    with TemporaryEnv("LLVMIR_VERSION", "3.2"):
        returnCode = 0
        for testSuiteName in parsedArgs.suite:
            suite = irBuilderTests32[testSuiteName]
            """runs the test suite"""
            if parsedArgs.skip_compilation is False:
                mx_sulong.ensureDragonEggExists()
                mx_sulong.mx_testsuites.compileSuite([suite[0]])
            try:
                mx_sulong.mx_testsuites.run32(vmArgs, suite[1], [])
            except:
                pass
            if runIRGeneratorSuite(LlvmAS_32, LlvmLLI_32, suite[2]) != 0:
                returnCode = 1
        return returnCode

def runIRBuilderTest38(vmArgs):
    vmArgs, otherArgs = mx_sulong.truffle_extract_VM_args(vmArgs)
    parser = argparse.ArgumentParser(description="Compiles all or selected test suites.")
    parser.add_argument('suite', nargs='*', help=' '.join(irBuilderTests38.keys()), default=irBuilderTests38.keys())
    parser.add_argument('--skip-compilation', help='skip suite compilation', action='store_true')  # TODO: makefile
    parsedArgs = parser.parse_args(otherArgs)

    with TemporaryEnv("LLVMIR_VERSION", "3.8"):
        returnCode = 0
        for testSuiteName in parsedArgs.suite:
            suite = irBuilderTests38[testSuiteName]
            """runs the test suite"""
            if parsedArgs.skip_compilation is False:
                mx_sulong.ensureDragonEggExists()
                mx_sulong.mx_testsuites.compileSuite([suite[0]])
            try:
                mx_sulong.mx_testsuites.run38(vmArgs, suite[1], [])
            except:
                pass
            if runIRGeneratorSuite(LlvmAS_38, LlvmLLI_38, suite[2]) != 0:
                returnCode = 1
        return returnCode

def runIRBuilderTestGen38(vmArgs):
    vmArgs, otherArgs = mx_sulong.truffle_extract_VM_args(vmArgs)
    parser = argparse.ArgumentParser(description="Compiles all or selected test suites.")
    parser.add_argument('suite', nargs='*', help=' '.join(irBuilderTestsGen38.keys()), default=irBuilderTestsGen38.keys())
    parsedArgs = parser.parse_args(otherArgs)

    with TemporaryEnv("LLVMIR_VERSION", "3.8"):
        returnCode = 0
        for testSuiteName in parsedArgs.suite:
            suite = irBuilderTestsGen38[testSuiteName]
            """runs the test suite"""
            try:
                mx_sulong.mx_testsuites.run38(vmArgs, suite[0], [])
            except:
                pass
            if runIRGeneratorBuilderSuite(LlvmAS_38, LlvmLLI_38, suite[1]) != 0:
                returnCode = 1
        return returnCode


class CompareFileResult(Enum):
    PASSED = 0
    FAILED = 1
    FAILED_REFERENCE = 2


def testFiles(assembler, lli, lliReference, lliFiles, sulongFiles, expectedExitVal=None):
    # test Files which need to be run with lli
    for file in lliReference:
        # run file using lli
        exitVal = lli.run(file)

        # test for errrors
        if expectedExitVal is None:
            if exitVal == -6 or exitVal == -11:
                # there was either a segfault or a abort
                return CompareFileResult.FAILED_REFERENCE
            expectedExitVal = exitVal
        elif expectedExitVal is not None and exitVal != expectedExitVal:
            return CompareFileResult.FAILED_REFERENCE

    for file in lliFiles:
        # run file using lli
        exitVal = lli.run(file)

        # test for errrors
        if expectedExitVal is None:
            expectedExitVal = exitVal
        elif expectedExitVal is not None and exitVal != expectedExitVal:
            return CompareFileResult.FAILED

    # test Files which need to be run with sulong
    for file in sulongFiles:
        # assemble file if required
        if file.endswith('.ll'):
            if assembler.run(file) == 0:
                file = file[:-3] + ".bc"
            else:
                return CompareFileResult.FAILED

        # run file using sulong
        exitVal = mx_sulong.runLLVM([file])

        # test for errrors
        if expectedExitVal is None:
            expectedExitVal = exitVal
        elif expectedExitVal is not None and exitVal != expectedExitVal:
            return CompareFileResult.FAILED

    return CompareFileResult.PASSED


def runIRGeneratorSuite(assembler, lli, cacheDir):
    mx.log('Testing Generated LLVM IR Files')
    mx.log(cacheDir)

    passed = []
    failed = []
    failed_references = []

    for root, _, files in os.walk(cacheDir):
        for fileName in files:
            inputFile = os.path.join(cacheDir, root, fileName)
            if inputFile.endswith('.out.ll'):
                ref_file = inputFile[:-7] + ".bc"

                ret = testFiles(assembler, lli, [ref_file], [inputFile], [])

                if ret is CompareFileResult.PASSED:
                    sys.stdout.write('.')
                    sys.stdout.flush()
                    passed.append(inputFile)
                elif ret is CompareFileResult.FAILED:
                    sys.stdout.write('E')
                    sys.stdout.flush()
                    failed.append(inputFile)
                elif ret is CompareFileResult.FAILED_REFERENCE:
                    sys.stdout.write('W')
                    sys.stdout.flush()
                    failed_references.append(inputFile)

    total = len(failed) + len(passed)
    mx.log()

    if len(failed_references):
        mx.log_error(str(len(failed_references)) + ' compiled reference Tests failed!')
        for x in range(0, len(failed_references)):
            mx.log_error(str(x) + ') ' + failed_references[x])
        mx.log()

    if len(failed) != 0:
        mx.log_error('Failed ' + str(len(failed)) + ' of ' + str(total) + ' Tests!')
        for x in range(0, len(failed)):
            mx.log_error(str(x) + ') ' + failed[x])
        return 1
    elif total == 0:
        mx.log_error('There is something odd with the testsuite, ' + str(total) + ' Tests executed!')
        return 1
    else:
        mx.log('Passed all ' + str(total) + ' Tests!')
        return 0


def runIRGeneratorBuilderSuite(assembler, lli, sulongSuiteCacheDir):
    mx.log('Testing Reassembly')
    mx.log(sulongSuiteCacheDir)

    passed = []
    failed = []
    failed_references = []

    for root, _, files in os.walk(sulongSuiteCacheDir):
        for fileName in files:
            inputFile = os.path.join(sulongSuiteCacheDir, root, fileName)
            if inputFile.endswith('.ll'):
                ret = testFiles(assembler, lli, [inputFile], [], [inputFile], 0)

                if ret is CompareFileResult.PASSED:
                    sys.stdout.write('.')
                    sys.stdout.flush()
                    passed.append(inputFile)
                elif ret is CompareFileResult.FAILED:
                    sys.stdout.write('E')
                    sys.stdout.flush()
                    failed.append(inputFile)
                elif ret is CompareFileResult.FAILED_REFERENCE:
                    sys.stdout.write('W')
                    sys.stdout.flush()
                    failed_references.append(inputFile)

    total = len(failed) + len(passed)
    mx.log()

    if len(failed_references):
        mx.log_error(str(len(failed_references)) + ' compiled reference Tests failed!')
        for x in range(0, len(failed_references)):
            mx.log_error(str(x) + ') ' + failed_references[x])
        mx.log()

    if len(failed) != 0:
        mx.log_error('Failed ' + str(len(failed)) + ' of ' + str(total) + ' Tests!')
        for x in range(0, len(failed)):
            mx.log_error(str(x) + ') ' + failed[x])
        return 1
    elif total == 0:
        mx.log_error('There is something odd with the testsuite, ' + str(total) + ' Tests executed!')
        return 1
    else:
        mx.log('Passed all ' + str(total) + ' Tests!')
        return 0


mx.update_commands(_suite, {
    'irbuilder-out' : [runIRBuilderOut, ''],
    'irbuilder-test32' : [runIRBuilderTest32, ''],
    'irbuilder-test38' : [runIRBuilderTest38, ''],
    'irbuilder-testgen38' : [runIRBuilderTestGen38, ''],
})

