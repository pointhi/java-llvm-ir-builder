import mx
import mx_sulong

_suite = mx.suite('irbuilder')


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
        mx_sulong.mx_testsuites.run32(vmArgs, "at.pointhi.irbuilder.test.IRGeneratorSuite",[])
    except:
        pass
    #return _runIRGeneratorSuite(mx_tools.Tool.LLVM_AS_32, mx_tools.Tool.LLVM_LLI_32)

def runIRBuilderTest38(vmArgs):
    """runs the Sulong test suite"""
    mx_sulong.ensureDragonEggExists()
    mx_sulong.mx_testsuites.compileSuite(['sulong38'])
    try:
        mx_sulong.mx_testsuites.run38(vmArgs, "at.pointhi.irbuilder.test.IRGeneratorSuite",[])
    except:
        pass
    #return _runIRGeneratorSuite(mx_tools.Tool.LLVM_AS_38, mx_tools.Tool.LLVM_LLI_38)

mx.update_commands(_suite, {
    'irbuilder-out' : [runIRBuilderOut, ''],
    'irbuilder-test32' : [runIRBuilderTest32, ''],
    'irbuilder-test38' : [runIRBuilderTest38, ''],
})

