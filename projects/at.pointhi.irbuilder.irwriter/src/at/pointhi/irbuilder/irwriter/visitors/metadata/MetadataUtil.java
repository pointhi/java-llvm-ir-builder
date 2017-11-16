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
package at.pointhi.irbuilder.irwriter.visitors.metadata;

import com.oracle.truffle.llvm.parser.metadata.MDBasicType.DwarfEncoding;
import com.oracle.truffle.llvm.parser.metadata.MDCompositeType;
import com.oracle.truffle.llvm.parser.metadata.MDDerivedType;
import com.oracle.truffle.llvm.runtime.types.DwLangNameRecord;

public class MetadataUtil {
    private MetadataUtil() {
    }

    public static String decode(DwarfEncoding encoding) {
        switch (encoding) {
            case DW_ATE_ADDRESS:
                return "DW_ATE_address";
            case DW_ATE_BOOLEAN:
                return "DW_ATE_boolean";
            case DW_ATE_FLOAT:
                return "DW_ATE_float";
            case DW_ATE_SIGNED:
                return "DW_ATE_signed";
            case DW_ATE_SIGNED_CHAR:
                return "DW_ATE_signed_char";
            case DW_ATE_UNSIGNED:
                return "DW_ATE_unsigned";
            case DW_ATE_UNSIGNED_CHAR:
                return "DW_ATE_unsigned_char";

            default:
                throw new RuntimeException("unexpected DwarfEncoding: " + encoding);
        }
    }

    public static String decode(DwLangNameRecord record) {
        switch (record) {
            case DW_LANG_ADA83:
                return "DW_LANG_Ada83";
            case DW_LANG_ADA95:
                return "DW_LANG_Ada95";
            case DW_LANG_C_PLUS_PLUS:
                return "DW_LANG_C_plus_plus";
            case DW_LANG_COBOL74:
                return "DW_LANG_Cobol74";
            case DW_LANG_COBOL85:
                return "DW_LANG_Cobol85";
            case DW_LANG_FORTRAN77:
                return "DW_LANG_Fortran77";
            case DW_LANG_FORTRAN90:
                return "DW_LANG_Fortran90";
            case DW_LANG_FORTRAN95:
                return "DW_LANG_Fortran95";
            case DW_LANG_JAVA:
                return "DW_LANG_Java";
            case DW_LANG_MIPS_ASSEMBLER:
                return "DW_LANG_Mips_Assembler";
            case DW_LANG_MODULA2:
                return "DW_LANG_Modula2";
            case DW_LANG_OBJC:
                return "DW_LANG_ObjC";
            case DW_LANG_OJC_PLUS_PLUS:
                return "DW_LANG_ObjC_plus_plus";
            case DW_LANG_PASCAL83:
                return "DW_LANG_Pascal83";
            case DW_LANG_PYTHON:
                return "Python";

            case DW_LANG_C:
            case DW_LANG_C89:
            case DW_LANG_C99:
            case DW_LANG_D:
            case DW_LANG_PLI:
            case DW_LANG_UPC:
                return record.toString();

            default:
                throw new RuntimeException("unexpected DwLangNameRecord: " + record);
        }

    }

    public static String decode(MDCompositeType.Tag tag) {
        switch (tag) {
            case DW_TAG_ARRAY_TYPE:
                return "DW_TAG_array_type";
            case DW_TAG_CLASS_TYPE:
                return "DW_TAG_class_type";
            case DW_TAG_ENUMERATION_TYPE:
                return "DW_TAG_enumeration_type";
            case DW_TAG_STRUCTURE_TYPE:
                return "DW_TAG_structure_type";
            case DW_TAG_SUBROUTINE_TYPE:
                return "DW_TAG_subroutine_type";
            case DW_TAG_UNION_TYPE:
                return "DW_TAG_union_type";
            case DW_TAG_VECTOR_TYPE:
                return "DW_TAG_vector_type";

            default:
                throw new RuntimeException("unexpected MDCompositeType.Tag: " + tag);
        }
    }

    public static String decode(MDDerivedType.Tag tag) {
        switch (tag) {
            case DW_TAG_CONST_TYPE:
                return "DW_TAG_const_type";
            case DW_TAG_FORMAL_PARAMETER:
                return "DW_TAG_formal_parameter";
            case DW_TAG_FRIEND:
                return "DW_TAG_friend";
            case DW_TAG_INHERITANCE:
                return "DW_TAG_inheritance";
            case DW_TAG_MEMBER:
                return "DW_TAG_member";
            case DW_TAG_POINTER_TYPE:
                return "DW_TAG_pointer_type";
            case DW_TAG_REFERENCE_TYPE:
                return "DW_TAG_reference_type";
            case DW_TAG_RESTRICT_TYPE:
                return "DW_TAG_restrict_type";
            case DW_TAG_TYPEDEF:
                return "DW_TAG_typedef";
            case DW_TAG_VOLATILE_TYPE:
                return "DW_TAG_volatile_type";

            default:
                throw new RuntimeException("unexpected MDDerivedType.Tag: " + tag);
        }
    }
}
