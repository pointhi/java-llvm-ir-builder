package at.pointhi.irbuilder.irbuilder;

import java.math.BigInteger;

import com.oracle.truffle.llvm.parser.model.enums.BinaryOperator;
import com.oracle.truffle.llvm.parser.model.enums.CompareOperator;
import com.oracle.truffle.llvm.parser.model.functions.FunctionDefinition;
import com.oracle.truffle.llvm.parser.model.functions.FunctionParameter;
import com.oracle.truffle.llvm.parser.model.symbols.constants.Constant;
import com.oracle.truffle.llvm.parser.model.symbols.constants.floatingpoint.FloatingPointConstant;
import com.oracle.truffle.llvm.parser.model.symbols.constants.integer.BigIntegerConstant;
import com.oracle.truffle.llvm.parser.model.symbols.constants.integer.IntegerConstant;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.Instruction;
import com.oracle.truffle.llvm.runtime.types.AggregateType;
import com.oracle.truffle.llvm.runtime.types.PrimitiveType;
import com.oracle.truffle.llvm.runtime.types.Type;
import com.oracle.truffle.llvm.runtime.types.VariableBitWidthType;
import com.oracle.truffle.llvm.runtime.types.symbols.Symbol;

public class SimpleInstrunctionBuilder {
    final InstructionBuilder builder;

    public SimpleInstrunctionBuilder(FunctionDefinition function) {
        this(new InstructionBuilder(function));
    }

    public SimpleInstrunctionBuilder(InstructionBuilder builder) {
        this.builder = builder;
    }

    private static Constant toConstant(Type type, double value) {
        if (!PrimitiveType.isFloatingpointType(type)) {
            throw new AssertionError("unexpected type: " + type);
        }
        return FloatingPointConstant.create(type, new long[]{Double.doubleToRawLongBits(value)});
    }

    private static Constant toConstant(Type type, long value) {
        if (PrimitiveType.isIntegerType(type)) {
            return new IntegerConstant(type, value);
        } else if (PrimitiveType.isFloatingpointType(type)) {
            return toConstant(type, (double) value);
        } else {
            throw new AssertionError("unexpected type: " + type);
        }
    }

    private static Constant toConstant(Type type, BigInteger value) {
        if (PrimitiveType.isIntegerType(type)) {
            if (type instanceof VariableBitWidthType) {
                return new BigIntegerConstant(type, value); // TODO
            } else {
                return new IntegerConstant(type, value.longValue());
            }
        } else if (PrimitiveType.isFloatingpointType(type)) {
            return toConstant(type, (double) value.longValue());
        } else {
            throw new AssertionError("unexpected type: " + type);
        }
    }

    public InstructionBuilder getInstructionBuilder() {
        return builder;
    }

    public FunctionParameter nextParameter() {
        final Type[] types = builder.getFunctionDefinition().getType().getArgumentTypes();
        final int id = builder.getArgCounter();
        final Type paramType;
        if (types.length <= id) {
            paramType = types[id - 1];
        } else {
            paramType = types[types.length - 1];
        }

        return builder.createParameter(paramType);
    }

    // Allocate
    public Instruction allocate(Type type) {
        return builder.createAllocate(type);
    }

    // Binary Operator
    public Instruction binaryOperator(BinaryOperator op, Symbol lhs, Symbol rhs) {
        return builder.createBinaryOperation(lhs, rhs, op);
    }

    public Instruction binaryOperator(BinaryOperator op, long lhs, Symbol rhs) {
        return builder.createBinaryOperation(toConstant(rhs.getType(), lhs), rhs, op);
    }

    public Instruction binaryOperator(BinaryOperator op, Symbol lhs, long rhs) {
        return builder.createBinaryOperation(lhs, toConstant(lhs.getType(), rhs), op);
    }

    public Instruction binaryOperator(BinaryOperator op, BigInteger lhs, Symbol rhs) {
        return builder.createBinaryOperation(toConstant(rhs.getType(), lhs), rhs, op);
    }

    public Instruction binaryOperator(BinaryOperator op, Symbol lhs, BigInteger rhs) {
        return builder.createBinaryOperation(lhs, toConstant(lhs.getType(), rhs), op);
    }

    public Instruction binaryOperator(BinaryOperator op, double lhs, Symbol rhs) {
        return builder.createBinaryOperation(toConstant(rhs.getType(), lhs), rhs, op);
    }

    public Instruction binaryOperator(BinaryOperator op, Symbol lhs, double rhs) {
        return builder.createBinaryOperation(lhs, toConstant(lhs.getType(), rhs), op);
    }

    // Compare
    public Instruction compare(CompareOperator op, Symbol lhs, Symbol rhs) {
        return builder.createCompare(op, lhs, rhs);
    }

    public Instruction compare(CompareOperator op, long lhs, Symbol rhs) {
        return builder.createCompare(op, toConstant(rhs.getType(), lhs), rhs);
    }

    public Instruction compare(CompareOperator op, Symbol lhs, long rhs) {
        return builder.createCompare(op, lhs, toConstant(lhs.getType(), rhs));
    }

    public Instruction compare(CompareOperator op, BigInteger lhs, Symbol rhs) {
        return builder.createCompare(op, toConstant(rhs.getType(), lhs), rhs);
    }

    public Instruction compare(CompareOperator op, Symbol lhs, BigInteger rhs) {
        return builder.createCompare(op, lhs, toConstant(lhs.getType(), rhs));
    }

    public Instruction compare(CompareOperator op, double lhs, Symbol rhs) {
        return builder.createCompare(op, toConstant(rhs.getType(), lhs), rhs);
    }

    public Instruction compare(CompareOperator op, Symbol lhs, double rhs) {
        return builder.createCompare(op, lhs, toConstant(lhs.getType(), rhs));
    }

    // Call
    public Instruction call(Symbol target, Symbol... arguments) {
        return builder.createCall(target, arguments);
    }

    // Extract Element
    public Instruction extractElement(Instruction vector, int index) {
        return builder.createExtractElement(vector, index);
    }

    // Insert Element
    public Instruction insertElement(Instruction vector, Constant value, int index) {
        return builder.createInsertElement(vector, value, index);
    }

    public Instruction insertElement(Instruction vector, long value, int index) {
        AggregateType type = (AggregateType) vector.getType();
        return builder.createInsertElement(vector, toConstant(type.getElementType(index), value), index);
    }

    public Instruction insertElement(Instruction vector, BigInteger value, int index) {
        AggregateType type = (AggregateType) vector.getType();
        return builder.createInsertElement(vector, toConstant(type.getElementType(index), value), index);
    }

    public Instruction insertElement(Instruction vector, double value, int index) {
        AggregateType type = (AggregateType) vector.getType();
        return builder.createInsertElement(vector, toConstant(type.getElementType(index), value), index);
    }

    // Load
    public Instruction load(Instruction source) {
        return builder.createLoad(source);
    }

    // Return
    public void return_(Symbol value) {
        builder.createReturn(value);
    }

    public void return_() {
        builder.createReturn();
    }

}
