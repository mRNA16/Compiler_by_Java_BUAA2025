package backend.mips.assembly;

import backend.mips.MipsBuilder;

public abstract class MipsAssembly {
    private final MipsType mipsType;

    public MipsAssembly(MipsType mipsType) {
        this.mipsType = mipsType;
        MipsBuilder.addAssembly(this);
    }

    public MipsType getMipsType() {
        return this.mipsType;
    }

    @Override
    public abstract String toString();
}
