package backend.mips.assembly.data;

public class MipsSpace extends MipsDataAssembly {
    private final String name;
    private final int size;

    public MipsSpace(String name, int size) {
        super(name);
        this.name = name;
        this.size = size;
    }

    @Override
    public String toString() {
        return this.name + ": .space " + this.size;
    }
}
