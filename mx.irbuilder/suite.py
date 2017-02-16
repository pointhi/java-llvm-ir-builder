suite = {
    "mxversion" : "5.70.2",
    "name" : "java-llvm-ir-builder",
    "versionConflictResolution" : "latest",

    "imports" : {
        "suites" : [
            {
                "name" : "sulong",
                "version" : "6e5a4355382c4abfbe81fe692e9eef290754b79b",
                "urls" : [
                    {
                        "url" : "https://github.com/graalvm/sulong",
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
            "dependencies" : [],
            "javaCompliance" : "1.8",
            "license" : "BSD-new",
        },
    }
}
