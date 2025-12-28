package midend.llvm.instr.io;

import midend.llvm.type.IrBaseType;

import backend.mips.MipsBuilder;
import backend.mips.Register;
import backend.mips.assembly.MipsAlu;
import backend.mips.assembly.MipsSyscall;
import backend.mips.assembly.fake.MarsMove;
import backend.mips.assembly.MipsLsu;

public class GetCharInstr extends IOInstr {
    public GetCharInstr() {
        super(IrBaseType.INT32);
    }

    public static String GetDeclare() {
        return "declare i32 @getchar() ";
    }

    @Override
    public String toString() {
        return this.irName + " = call i32 @getchar()";
    }

    @Override
    public void toMips() {
        super.toMips();
        new MipsAlu(MipsAlu.AluType.ADDI, Register.V0, Register.ZERO, 12);
        new MipsSyscall();

        Register rd = MipsBuilder.allocateStackForValue(this) == null ? MipsBuilder.getValueToRegister(this)
                : Register.K0;

        if (rd != Register.V0) {
            new MarsMove(rd, Register.V0);
        }

        if (rd == Register.K0) {
            Integer offset = MipsBuilder.getStackValueOffset(this);
            if (offset != null) {
                new MipsLsu(MipsLsu.LsuType.SW, Register.K0, Register.SP, offset);
            }
        }
    }
}
