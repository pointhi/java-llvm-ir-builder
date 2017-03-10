import mx

_suite = mx.suite('irbuilder')


def getIRWriterClasspathOptions():
    """gets the classpath of the IRWRITER distributions"""
    return mx.get_runtime_jvm_args('IRWRITER')

def runParseSource(args=None, out=None):
    return mx.run_java(getIRWriterClasspathOptions() + ["at.pointhi.irbuilder.irwriter.SourceParser"] + args)

mx.update_commands(_suite, {
    'output-ir' : [runParseSource, ''],
})

