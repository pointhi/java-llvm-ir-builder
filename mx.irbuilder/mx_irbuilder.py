import os

import mx
import mx_sulong
import mx_testsuites
from mx_irbuilder_util import TemporaryEnv

#import multiprocessing
import argparse
import sys

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
            if _runIRGeneratorSuite(LlvmAS_32, LlvmLLI_32, suite[2]) != 0:
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
            if _runIRGeneratorSuite(LlvmAS_38, LlvmLLI_38, suite[2]) != 0:
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
            if _runIRGeneratorBuilderSuite(LlvmAS_38, LlvmLLI_38, suite[1]) != 0:
                returnCode = 1
        return returnCode

def _testFile(param):
    inputFile = param[0]
    assembler = param[1]
    lli = param[2]

    failed = []
    segfaulted = []
    passed = []

    if inputFile.endswith('.out.ll'):
        if assembler.run(inputFile) == 0:
            exit_code_ref = lli.run(inputFile[:-7] + ".bc")
            exit_code_out = lli.run(inputFile[:-7] + ".out.bc")
            if exit_code_ref == exit_code_out:
                sys.stdout.write('.')
                passed.append(inputFile)
                sys.stdout.flush()
            else:
                if exit_code_ref == -6 or exit_code_ref == -11:
                    sys.stdout.write('S')  # reference code had a segfault or aborted, don't count
                    segfaulted.append(inputFile[:-7] + ".bc")
                else:
                    sys.stdout.write('E')
                    failed.append(inputFile)
                sys.stdout.flush()
        else:
            sys.stdout.write('E')
            failed.append(inputFile)
            sys.stdout.flush()

    return passed, failed, segfaulted

def _runIRGeneratorSuite(assembler, lli, sulongSuiteCacheDir):
    mx.log('Testing Reassembly')
    mx.log(sulongSuiteCacheDir)

    passed = []
    failed = []
    segfaulted = []

    #processes = multiprocessing.cpu_count() * 2
    #processes = 1
    #pool = multiprocessing.Pool(processes)
    #inputFiles = []

    results = []

    for root, _, files in os.walk(sulongSuiteCacheDir):
        for fileName in files:
            inputFile = os.path.join(sulongSuiteCacheDir, root, fileName)
            results += [_testFile([inputFile, assembler, lli])]
            #inputFiles.append([inputFile, assembler, lli])

    #results = pool.map(_testFile, inputFiles)

    for result in results:
        passed += result[0]
        failed += result[1]
        segfaulted += result[2]


    total = len(failed) + len(passed)
    mx.log()

    if len(segfaulted):
        mx.log_error(str(len(segfaulted)) + ' compiled Tests segfaulted in lli!')
        for x in range(0, len(segfaulted)):
            mx.log_error(str(x) + ') ' + segfaulted[x])
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

def _testGeneratedFile(param):
    inputFile = param[0]
    assembler = param[1]
    lli = param[2]

    failed = []
    wrong = []
    passed = []

    if inputFile.endswith('.ll'):
        if assembler.run(inputFile) == 0:
            exit_code_ref = lli.run(inputFile[:-3] + ".bc")
            try:
                exit_code_out = mx_sulong.runLLVM([inputFile[:-3] + ".bc"])
            except:
                # why is there sometimes a exceptions.SystemExit thrown? Arithmetic overflows?
                exit_code_out = -1

            if exit_code_ref == 0 and exit_code_out == 0:
                sys.stdout.write('.')
                passed.append(inputFile)
                sys.stdout.flush()
            elif exit_code_ref != 0:
                sys.stdout.write('W')  # reference code returned with a non-null
                wrong.append(inputFile[:-3] + ".bc")
            else:
                sys.stdout.write('E')
                failed.append(inputFile)
            sys.stdout.flush()
        else:
            sys.stdout.write('E')
            failed.append(inputFile)
            sys.stdout.flush()

    return passed, failed, wrong

def _runIRGeneratorBuilderSuite(assembler, lli, sulongSuiteCacheDir):
    mx.log('Testing Reassembly')
    mx.log(sulongSuiteCacheDir)

    passed = []
    failed = []
    wrong = []

    results = []

    for root, _, files in os.walk(sulongSuiteCacheDir):
        for fileName in files:
            inputFile = os.path.join(sulongSuiteCacheDir, root, fileName)
            results += [_testGeneratedFile([inputFile, assembler, lli])]

    for result in results:
        passed += result[0]
        failed += result[1]
        wrong += result[2]

    total = len(failed) + len(passed)
    mx.log()

    if len(wrong):
        mx.log_error(str(len(wrong)) + ' compiled Tests returned not 0 in lli!')
        for x in range(0, len(wrong)):
            mx.log_error(str(x) + ') ' + wrong[x])
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

