suite = {
    "mxversion" : "5.70.2",
    "name" : "java-llvm-ir-builder",
    "versionConflictResolution" : "latest",

    "imports" : {
        "suites" : [
            {
                "name" : "sulong",
                "version" : "8c3127a8ff182d8a6600a1de274aeaa5de46ee92",
                "urls" : [
                    {
                        "url" : "https://github.com/pointhi/sulong",
                        "kind" : "git"
                    },
                ]
            },
        ],
    },

    "javac.lint.overrides" : "none",

    "projects" : {
        "at.pointhi.irbuilder.irbuilder" : {
            "subDir" : "projects",
            "sourceDirs" : ["src"],
            "dependencies" : [
                "sulong:SULONG",
            ],
            "checkstyle" : "at.pointhi.irbuilder.irwriter",
            "javaCompliance" : "1.8",
            "license" : "BSD-new",
        },

        "at.pointhi.irbuilder.irwriter" : {
            "subDir" : "projects",
            "sourceDirs" : ["src"],
            "dependencies" : [
                "sulong:SULONG",
            ],
            "checkstyle" : "at.pointhi.irbuilder.irwriter",
            "annotationProcessors" : ["truffle:TRUFFLE_DSL_PROCESSOR"],
            "javaCompliance" : "1.8",
            "license" : "BSD-new",
        },

        "at.pointhi.irbuilder.test": {
            "subDir": "projects",
            "sourceDirs": ["src"],
            "dependencies": [
                "at.pointhi.irbuilder.irwriter",
                "sulong:SULONG",
                "sulong:SULONG_TEST",
                "mx:JUNIT",
            ],
            "checkstyle": "at.pointhi.irbuilder.irwriter",
            "javaCompliance": "1.8",
            "license": "BSD-new",
        },

        "at.pointhi.irbuilder.testgenerator": {
            "subDir": "projects",
            "sourceDirs": ["src"],
            "dependencies": [
                "at.pointhi.irbuilder.irbuilder",
                "at.pointhi.irbuilder.irwriter",
                "sulong:SULONG",
                "sulong:SULONG_TEST",
                "mx:JUNIT",
            ],
            "checkstyle": "at.pointhi.irbuilder.irwriter",
            "javaCompliance": "1.8",
            "license": "BSD-new",
        },
    },

    "distributions" : {
        "IRWRITER" : {
            "path" : "build/irwriter.jar",
            "subDir" : "graal",
            "sourcesPath" : "build/irbuilder.src.zip",
            "mainClass" : "at.pointhi.irbuilder.irwriter.SourceParser",
            "dependencies" : [
                "at.pointhi.irbuilder.irwriter"
            ],
            "distDependencies" : [
                "sulong:SULONG",
            ]
        },

        "IRWRITER_TEST" : {
            "path" : "build/irwriter_test.jar",
            "subDir" : "graal",
            "sourcesPath" : "build/irwriter_test.src.zip",
            "dependencies" : [
                "at.pointhi.irbuilder.test",
                "at.pointhi.irbuilder.testgenerator"
            ],
            "exclude" : [
                "mx:JUNIT"
            ],
            "distDependencies" : [
                "IRWRITER",
                "sulong:SULONG",
                "sulong:SULONG_TEST",
            ]
        },
    }
}
