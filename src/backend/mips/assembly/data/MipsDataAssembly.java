package backend.mips.assembly.data;

import backend.mips.assembly.MipsAssembly;
import backend.mips.assembly.MipsType;

public abstract class MipsDataAssembly extends MipsAssembly {
    private final String label;

    public MipsDataAssembly(String label) {
        super(MipsType.DATA);
        this.label = label;
    }

    @Override
    public String toString() {
        return this.label + ":";
    }
}
