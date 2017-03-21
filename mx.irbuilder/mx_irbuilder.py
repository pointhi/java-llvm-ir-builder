import os

import mx
import mx_sulong
import mx_testsuites
import sys

_suite = mx.suite('irbuilder')

class Tool(object):
    def supports(self, language):
        return language in self.supportedLanguages

    def runTool(self, args, errorMsg=None, **kwargs):
        try:
            if not mx.get_opts().verbose:
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
        return self.runTool([tool] + flags + [inputFile], errorMsg='Cannot assemble %s with %s' % (inputFile, tool))

class LlvmLLI(Tool):
    def __init__(self, supportedVersions):
        self.supportedVersions = supportedVersions

    def run(self, inputFile, flags=None):
        if flags is None:
            flags = []
        tool = mx_sulong.findLLVMProgram('lli', self.supportedVersions)
        return self.runTool([tool] + flags + [inputFile], nonZeroIsFatal=False, errorMsg='Cannot run %s with %s' % (inputFile, tool))

def getIRWriterClasspathOptions():
    """gets the classpath of the IRWRITER distributions"""
    return mx.get_runtime_jvm_args('IRWRITER')

def runIRBuilderOut(args=None, out=None):
    return mx.run_java(getIRWriterClasspathOptions() + ["at.pointhi.irbuilder.irwriter.SourceParser"] + args)

def runIRBuilderTest32(vmArgs):
    """runs the Sulong test suite"""
    mx_sulong.ensureDragonEggExists()
    mx_sulong.mx_testsuites.compileSuite(['sulong'])
    try:
        mx_sulong.mx_testsuites.run32(vmArgs, "at.pointhi.irbuilder.test.IRGeneratorSuite", [])
    except:
        pass
    return _runIRGeneratorSuite(LlvmAS(['3.2', '3.3']), LlvmLLI(['3.2', '3.3']))

def runIRBuilderTest38(vmArgs):
    """runs the Sulong test suite"""
    mx_sulong.ensureDragonEggExists()
    mx_sulong.mx_testsuites.compileSuite(['sulong38'])
    try:
        mx_sulong.mx_testsuites.run38(vmArgs, "at.pointhi.irbuilder.test.IRGeneratorSuite", [])
    except:
        pass
    return _runIRGeneratorSuite(LlvmAS(['3.8', '3.9']), LlvmLLI(['3.8', '3.9']))

def _runIRGeneratorSuite(assembler, lli):
    sulongSuiteCacheDir = os.path.join(mx_testsuites._cacheDir, 'sulong')
    mx.log('Testing Reassembly')
    mx.log(sulongSuiteCacheDir)
    failed = []
    passed = []
    for root, _, files in os.walk(sulongSuiteCacheDir):
        for fileName in files:
            inputFile = os.path.join(sulongSuiteCacheDir, root, fileName)
            if inputFile.endswith('.out.ll'):
                if assembler.run(inputFile) == 0:
                    exit_code_ref = lli.run(inputFile[:-7] + ".bc")
                    exit_code_out = lli.run(inputFile[:-7] + ".out.bc")
                    if exit_code_ref == exit_code_out:
                        sys.stdout.write('.')
                        passed.append(inputFile)
                        sys.stdout.flush()
                    else:
                        sys.stdout.write('E')
                        failed.append(inputFile)
                        sys.stdout.flush()
                else:
                    sys.stdout.write('E')
                    failed.append(inputFile)
                    sys.stdout.flush()
    total = len(failed) + len(passed)
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
})

