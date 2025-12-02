package utils;

import midend.llvm.constant.IrConstInt;
import midend.llvm.instr.comp.ICompInstr;
import midend.llvm.instr.convert.TruncInstr;
import midend.llvm.instr.convert.ZextInstr;
import midend.llvm.type.IrArrayType;
import midend.llvm.type.IrBaseType;
import midend.llvm.type.IrPointerType;
import midend.llvm.type.IrType;
import midend.llvm.value.IrValue;
import midend.semantic.SymbolType;

public class IrTypeConverter {
    // 符号类型转Ir类型
    public static IrType symbolType2IrType(SymbolType symbolType,int arraySize) {
        if(symbolType.isArray()) {
            if(symbolType == SymbolType.INT_ARRAY||symbolType == SymbolType.CONST_INT_ARRAY) {
                return new IrArrayType(arraySize,IrBaseType.INT32);
            }
            throw new RuntimeException("Wrong Array SymbolType: " + symbolType);
        } else {
            return switch (symbolType) {
                case VOID -> IrBaseType.VOID;
                case INT -> IrBaseType.INT32;
                case CONST_INT -> IrBaseType.INT32;
                case FUNCTION_INT -> IrBaseType.INT32;
                default -> throw new RuntimeException("Wrong SymbolType: " + symbolType);
            };
        }
    }

    // 处理函数参数时所需的转化方法
    public static IrType symbolType2IrType4Param(SymbolType symbolType) {
        if(symbolType.isArray()) {
            if(symbolType == SymbolType.INT_ARRAY||symbolType == SymbolType.CONST_INT_ARRAY) {
                return new IrPointerType(IrBaseType.INT32);
            }
            throw new RuntimeException("Wrong Array SymbolType: " + symbolType);
        } else {
            return switch (symbolType) {
                case VOID -> IrBaseType.VOID;
                case INT -> IrBaseType.INT32;
                case CONST_INT -> IrBaseType.INT32;
                case FUNCTION_INT -> IrBaseType.INT32;
                default -> throw new RuntimeException("Wrong SymbolType: " + symbolType);
            };
        }
    }

    public static IrValue convertType(IrValue value, IrType targetType) {
        IrType sourceType = value.getIrType();

        // 类型相同，无需转换
        if (sourceType.equals(targetType)) {
            return value;
        }

        // i1/i8 -> i32: 零扩展
        if (targetType.isInt32Type()) {
            if (sourceType.isInt1Type() || sourceType.isInt8Type()) {
                return new ZextInstr(targetType, value);
            }
        }

        // i32/i1 -> i8
        if (targetType.isInt8Type()) {
            if(sourceType.isInt32Type()){
                return new TruncInstr(targetType, value);
            } else if(sourceType.isInt1Type()){
                return new ZextInstr(targetType, value);
            }
        }

        // i32/i8 -> i1: 截断
        if (targetType.isInt1Type()) {
            return new ICompInstr("!=", value, new IrConstInt(0));
        }

        return value;
    }

    // 转为int，用于计算
    public static IrValue toInt32(IrValue value) {
        return convertType(value, IrBaseType.INT32);
    }

    // 转为bool，用于条件判断
    public static IrValue toInt1(IrValue value) {
        return convertType(value, IrBaseType.INT1);
    }
}
