suite = {
    "mxversion" : "5.70.2",
    "name" : "java-llvm-ir-builder",
    "versionConflictResolution" : "latest",

    "imports" : {
        "suites" : [
            {
                "name" : "sulong",
                "version" : "38a5bad302f48d676f15a0b3fd9b02f6f3a8abdd",
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
        "at.pointhi.irbuilder.irwriter" : {
            "subDir" : "projects",
            "sourceDirs" : ["src"],
            "dependencies" : [
                "sulong:SULONG",
            ],
            "checkstyle" : "at.pointhi.irbuilder.irwriter",
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
                "at.pointhi.irbuilder.test"
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
