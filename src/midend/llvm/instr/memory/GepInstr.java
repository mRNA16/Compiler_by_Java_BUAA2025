package midend.llvm.instr.memory;

import midend.llvm.instr.Instr;
import midend.llvm.instr.InstrType;
import midend.llvm.type.IrArrayType;
import midend.llvm.type.IrPointerType;
import midend.llvm.type.IrType;
import midend.llvm.value.IrValue;

public class GepInstr extends Instr {
    private final IrType sourceType;
    private final IrValue pointer;
    private final IrValue offset;

    public GepInstr(IrValue pointer,IrValue offset) {
        super(new IrPointerType(getSourceType(pointer)), InstrType.GEP);
        this.pointer = pointer;
        this.addUseValue(pointer);
        this.offset = offset;
        this.addUseValue(offset);
        this.sourceType = getSourceType(pointer);
    }

    public IrType getSourceType() {
        return sourceType;
    }

    public IrValue getPointer() {
        return pointer;
    }

    public IrValue getOffset() {
        return offset;
    }

    public static IrType getSourceType(IrValue pointer) {
        IrType sourceType = ((IrPointerType)pointer.getIrType()).getTargetType();
        if(sourceType.isArrayType()) {
            return ((IrArrayType)sourceType).getElementType();
        } else if(sourceType.isPointerType()) {
            return ((IrPointerType)sourceType).getTargetType();
        } else {
            return sourceType;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(irName).append(" =  getelementptr inbounds ");
        IrType pTargetType = ((IrPointerType)pointer.getIrType()).getTargetType();
        sb.append(pTargetType).append(", ").append(pointer.getIrType()).append(" ").append(pointer.getIrName()).append(", ");
        if(pTargetType.isArrayType()) sb.append("i32 0, ");
        sb.append(offset.getIrType()).append(" ").append(offset.getIrName());
        return sb.toString();
    }
}
