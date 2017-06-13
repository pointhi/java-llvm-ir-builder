/*
 * BSD 3-Clause License
 *
 * Copyright (c) 2017, Thomas Pointhuber
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this
 *  list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 *  this list of conditions and the following disclaimer in the documentation
 *  and/or other materials provided with the distribution.
 *
 * Neither the name of the copyright holder nor the names of its
 *  contributors may be used to endorse or promote products derived from
 *  this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package at.pointhi.irbuilder.irbuilder.helper;

import java.util.Optional;

import com.oracle.truffle.llvm.parser.model.functions.FunctionDeclaration;
import com.oracle.truffle.llvm.runtime.types.ArrayType;
import com.oracle.truffle.llvm.runtime.types.FunctionType;
import com.oracle.truffle.llvm.runtime.types.PointerType;
import com.oracle.truffle.llvm.runtime.types.PrimitiveType;
import com.oracle.truffle.llvm.runtime.types.StructureType;
import com.oracle.truffle.llvm.runtime.types.Type;
import com.oracle.truffle.llvm.runtime.types.VoidType;

import at.pointhi.irbuilder.irbuilder.ModelModuleBuilder;
import at.pointhi.irbuilder.irbuilder.util.ModelExtractor;

public final class LLVMIntrinsics {

    private static final FunctionType vaStartType = new FunctionType(VoidType.INSTANCE, new Type[]{new PointerType(PrimitiveType.I8)}, false);
    private static final String vaStartName = "llvm.va_start";

    public static FunctionDeclaration getLlvmVaStart(ModelModuleBuilder builder) {
        return getFunctionDeclaration(builder, vaStartName, vaStartType);
    }

    private static final FunctionType vaEndType = new FunctionType(VoidType.INSTANCE, new Type[]{new PointerType(PrimitiveType.I8)}, false);
    private static final String vaEndName = "llvm.va_end";

    public static FunctionDeclaration getLlvmVaEnd(ModelModuleBuilder builder) {
        return getFunctionDeclaration(builder, vaEndName, vaEndType);
    }

    private static final FunctionType memcpyP0i8P0i8i64Type = new FunctionType(VoidType.INSTANCE,
                    new Type[]{new PointerType(PrimitiveType.I8), new PointerType(PrimitiveType.I8), PrimitiveType.I64, PrimitiveType.I32, PrimitiveType.I32, PrimitiveType.I1}, false);
    private static final String memcpyP0i8P0i8i64Name = "llvm.memcpy.p0i8.p0i8.i64";

    public static FunctionDeclaration getLlvmMemcpyP0i8P0i8i64(ModelModuleBuilder builder) {
        return getFunctionDeclaration(builder, memcpyP0i8P0i8i64Name, memcpyP0i8P0i8i64Type);
    }

    private static FunctionDeclaration getFunctionDeclaration(ModelModuleBuilder builder, String name, FunctionType type) {
        final ModelExtractor<FunctionDeclaration> extractor = new ModelExtractor.FunctionDeclarationExtractor(f -> f.getName().equals(name) && f.getType().equals(type));

        builder.getModelModule().accept(extractor);

        // only create function declaration if it does not exist yet
        final Optional<FunctionDeclaration> match = extractor.getMatch();
        if (match.isPresent()) {
            return match.get();
        } else {
            return builder.createFunctionDeclaration(name, type);
        }
    }

    public static StructureType registerVaListTagType(ModelModuleBuilder builder) {
        // This in system specific
        // @see http://llvm.org/docs/LangRef.html#variable-argument-handling-intrinsics

        /*
         * %struct.__va_list_tag = type { i32, i32, i8*, i8* }
         */
        Type[] vaListTagTypes = new Type[4];

        int i = 0;
        vaListTagTypes[i++] = PrimitiveType.I32;
        vaListTagTypes[i++] = PrimitiveType.I32;
        vaListTagTypes[i++] = new PointerType(PrimitiveType.I8);
        vaListTagTypes[i++] = new PointerType(PrimitiveType.I8);

        StructureType vaListTag = new StructureType(false, vaListTagTypes);
        vaListTag.setName("struct.__va_list_tag");

        // register our new type
        builder.createType(vaListTag);

        return vaListTag;
    }

    public static StructureType registerIOFileType(ModelModuleBuilder builder) {
        /*
         * %struct._IO_FILE = type { i32, i8*, i8*, i8*, i8*, i8*, i8*, i8*, i8*, i8*, i8*, i8*,
         * %struct._IO_marker*, %struct._IO_FILE*, i32, i32, i64, i16, i8, [1 x i8], i8*, i64, i8*,
         * i8*, i8*, i8*, i64, i32, [20 x i8] }
         */
        Type[] ioFileTypes = new Type[29];

        int i = 0;
        ioFileTypes[i++] = PrimitiveType.I32;
        ioFileTypes[i++] = new PointerType(PrimitiveType.I8);
        ioFileTypes[i++] = new PointerType(PrimitiveType.I8);
        ioFileTypes[i++] = new PointerType(PrimitiveType.I8);
        ioFileTypes[i++] = new PointerType(PrimitiveType.I8);
        ioFileTypes[i++] = new PointerType(PrimitiveType.I8);
        ioFileTypes[i++] = new PointerType(PrimitiveType.I8);
        ioFileTypes[i++] = new PointerType(PrimitiveType.I8);
        ioFileTypes[i++] = new PointerType(PrimitiveType.I8);
        ioFileTypes[i++] = new PointerType(PrimitiveType.I8);
        ioFileTypes[i++] = new PointerType(PrimitiveType.I8);
        ioFileTypes[i++] = new PointerType(PrimitiveType.I8);
        ioFileTypes[i++] = null; // %struct._IO_marker*
        ioFileTypes[i++] = null; // %struct._IO_FILE*
        ioFileTypes[i++] = PrimitiveType.I32;
        ioFileTypes[i++] = PrimitiveType.I32;
        ioFileTypes[i++] = PrimitiveType.I64;
        ioFileTypes[i++] = PrimitiveType.I16;
        ioFileTypes[i++] = PrimitiveType.I8;
        ioFileTypes[i++] = new ArrayType(PrimitiveType.I8, 1);
        ioFileTypes[i++] = new PointerType(PrimitiveType.I8);
        ioFileTypes[i++] = PrimitiveType.I64;
        ioFileTypes[i++] = new PointerType(PrimitiveType.I8);
        ioFileTypes[i++] = new PointerType(PrimitiveType.I8);
        ioFileTypes[i++] = new PointerType(PrimitiveType.I8);
        ioFileTypes[i++] = new PointerType(PrimitiveType.I8);
        ioFileTypes[i++] = PrimitiveType.I64;
        ioFileTypes[i++] = PrimitiveType.I32;
        ioFileTypes[i++] = new ArrayType(PrimitiveType.I8, 20);

        StructureType ioFile = new StructureType(false, ioFileTypes);
        ioFile.setName("struct._IO_FILE");

        /*
         * %struct._IO_marker = type { %struct._IO_marker*, %struct._IO_FILE*, i32 }
         */
        Type[] ioMarkerTypes = new Type[3];

        i = 0;
        ioMarkerTypes[i++] = null; // %struct._IO_marker*
        ioMarkerTypes[i++] = null; // %struct._IO_FILE*
        ioMarkerTypes[i++] = PrimitiveType.I32;

        StructureType ioMarker = new StructureType(false, ioMarkerTypes);
        ioMarker.setName("struct._IO_marker");

        // Patch Structure Types which couldn't be resolved earlier
        ioFileTypes[12] = new PointerType(ioMarker);
        ioFileTypes[13] = new PointerType(ioFile);

        ioMarkerTypes[0] = new PointerType(ioMarker);
        ioMarkerTypes[1] = new PointerType(ioFile);

        // register our new types
        builder.createType(ioFile);
        builder.createType(ioMarker);

        return ioFile;
    }
}
