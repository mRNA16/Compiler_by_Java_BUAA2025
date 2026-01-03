package backend.mips;

import backend.mips.assembly.*;
import backend.mips.assembly.fake.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Peephole {
    private final MipsModule module;

    public Peephole(MipsModule module) {
        this.module = module;
    }

    public void optimize() {
        MipsBuilder.setAutoAdd(false);
        boolean changed = true;
        while (changed) {
            changed = false;
            changed |= removeRedundantInstructions();
            changed |= simplifyControlFlow();
            changed |= mergeStoreLoad();
            changed |= mergeCompareAndBranch();
        }
        MipsBuilder.setAutoAdd(true);
    }

    private boolean removeRedundantInstructions() {
        ArrayList<MipsAssembly> text = module.getTextSegment();
        boolean changed = false;
        for (int i = 0; i < text.size(); i++) {
            MipsAssembly instr = text.get(i);

            // 1. move $r, $r
            if (instr instanceof MarsMove move) {
                if (move.getDst() == move.getSrc()) {
                    text.remove(i);
                    i--;
                    changed = true;
                    continue;
                }
            }

            // 2. addi $r, $r, 0
            if (instr instanceof MipsAlu alu) {
                if (alu.getImmediate() != null && alu.getImmediate() == 0) {
                    if (alu.getAluType() == MipsAlu.AluType.ADDI || alu.getAluType() == MipsAlu.AluType.ADDIU) {
                        if (alu.getRd() == alu.getRs()) {
                            text.remove(i);
                            i--;
                            changed = true;
                            continue;
                        } else {
                            text.set(i, new MarsMove(alu.getRd(), alu.getRs()));
                            changed = true;
                            continue;
                        }
                    }
                }
                // add $r, $r, $zero
                if (alu.getRt() == Register.ZERO
                        && (alu.getAluType() == MipsAlu.AluType.ADD || alu.getAluType() == MipsAlu.AluType.ADDU)) {
                    if (alu.getRd() == alu.getRs()) {
                        text.remove(i);
                        i--;
                        changed = true;
                        continue;
                    } else {
                        text.set(i, new MarsMove(alu.getRd(), alu.getRs()));
                        changed = true;
                        continue;
                    }
                }
            }

            // 3. li $r, 0 -> move $r, $zero
            if (instr instanceof MarsLi li) {
                if (li.getNumber() == 0) {
                    text.set(i, new MarsMove(li.getRd(), Register.ZERO));
                    changed = true;
                    continue;
                }
            }
        }
        return changed;
    }

    private boolean simplifyControlFlow() {
        ArrayList<MipsAssembly> text = module.getTextSegment();
        boolean changed = false;

        // 1. Jump to next instruction
        for (int i = 0; i < text.size() - 1; i++) {
            MipsAssembly instr = text.get(i);
            if (instr instanceof MipsJump jump && jump.getJumpType() == MipsJump.JumpType.J) {
                // Skip annotations
                int nextIdx = i + 1;
                while (nextIdx < text.size() && text.get(nextIdx) instanceof MipsAnnotation) {
                    nextIdx++;
                }
                if (nextIdx < text.size() && text.get(nextIdx) instanceof MipsLabel label) {
                    if (label.getLabel().equals(jump.getTargetLabel())) {
                        text.remove(i);
                        i--;
                        changed = true;
                        continue;
                    }
                }
            }
        }

        // 2. Jump to jump
        Map<String, String> jumpMap = new HashMap<>();
        for (int i = 0; i < text.size(); i++) {
            if (text.get(i) instanceof MipsLabel label) {
                int nextIdx = i + 1;
                while (nextIdx < text.size() && text.get(nextIdx) instanceof MipsAnnotation) {
                    nextIdx++;
                }
                if (nextIdx < text.size() && text.get(nextIdx) instanceof MipsJump jump
                        && jump.getJumpType() == MipsJump.JumpType.J) {
                    jumpMap.put(label.getLabel(), jump.getTargetLabel());
                }
            }
        }

        if (!jumpMap.isEmpty()) {
            for (int i = 0; i < text.size(); i++) {
                MipsAssembly instr = text.get(i);
                if (instr instanceof MipsJump jump && jump.getJumpType() == MipsJump.JumpType.J) {
                    String target = jump.getTargetLabel();
                    if (jumpMap.containsKey(target)) {
                        String newTarget = jumpMap.get(target);
                        if (!newTarget.equals(target)) {
                            text.set(i, new MipsJump(MipsJump.JumpType.J, newTarget));
                            changed = true;
                        }
                    }
                } else if (instr instanceof MipsBranch branch) {
                    String target = branch.getLabel();
                    if (jumpMap.containsKey(target)) {
                        String newTarget = jumpMap.get(target);
                        if (!newTarget.equals(target)) {
                            if (branch.getRt() != null) {
                                text.set(i, new MipsBranch(branch.getBranchType(), branch.getRs(), branch.getRt(),
                                        newTarget));
                            } else {
                                text.set(i, new MipsBranch(branch.getBranchType(), branch.getRs(), newTarget));
                            }
                            changed = true;
                        }
                    }
                }
            }
        }

        // 3. Jump over jump
        // beq $r1, $r2, L1; j L2; L1: -> bne $r1, $r2, L2; L1:
        for (int i = 0; i < text.size() - 2; i++) {
            MipsAssembly i1 = text.get(i);
            MipsAssembly i2 = text.get(i + 1);
            MipsAssembly i3 = text.get(i + 2);

            if (i1 instanceof MipsBranch branch && i2 instanceof MipsJump jump
                    && jump.getJumpType() == MipsJump.JumpType.J && i3 instanceof MipsLabel label) {
                if (label.getLabel().equals(branch.getLabel())) {
                    MipsBranch.BranchType inverted = invertBranch(branch.getBranchType());
                    if (inverted != null) {
                        if (branch.getRt() != null) {
                            text.set(i,
                                    new MipsBranch(inverted, branch.getRs(), branch.getRt(), jump.getTargetLabel()));
                        } else {
                            text.set(i, new MipsBranch(inverted, branch.getRs(), jump.getTargetLabel()));
                        }
                        text.remove(i + 1);
                        changed = true;
                    }
                }
            }
        }

        return changed;
    }

    private MipsBranch.BranchType invertBranch(MipsBranch.BranchType type) {
        return switch (type) {
            case BEQ -> MipsBranch.BranchType.BNE;
            case BNE -> MipsBranch.BranchType.BEQ;
            case BGTZ -> MipsBranch.BranchType.BLEZ;
            case BLEZ -> MipsBranch.BranchType.BGTZ;
            case BGEZ -> MipsBranch.BranchType.BLTZ;
            case BLTZ -> MipsBranch.BranchType.BGEZ;
        };
    }

    private boolean mergeStoreLoad() {
        ArrayList<MipsAssembly> text = module.getTextSegment();
        boolean changed = false;
        for (int i = 0; i < text.size() - 1; i++) {
            MipsAssembly instr1 = text.get(i);
            MipsAssembly instr2 = text.get(i + 1);

            if (instr1 instanceof MipsLsu lsu1 && lsu1.getLsuType() == MipsLsu.LsuType.SW) {
                if (instr2 instanceof MipsLsu lsu2 && lsu2.getLsuType() == MipsLsu.LsuType.LW) {
                    if (lsu1.getBase() == lsu2.getBase() &&
                            lsu1.getOffset().equals(lsu2.getOffset()) &&
                            lsu1.getLabel() == null && lsu2.getLabel() == null) {

                        if (lsu1.getRd() == lsu2.getRd()) {
                            text.remove(i + 1);
                            changed = true;
                        } else {
                            text.set(i + 1, new MarsMove(lsu2.getRd(), lsu1.getRd()));
                            changed = true;
                        }
                    }
                }
            }
        }
        return changed;
    }

    private boolean mergeCompareAndBranch() {
        ArrayList<MipsAssembly> text = module.getTextSegment();
        boolean changed = false;
        for (int i = 0; i < text.size() - 1; i++) {
            MipsAssembly i1 = text.get(i);
            MipsAssembly i2 = text.get(i + 1);

            if (i1 instanceof MipsCompare cmp && i2 instanceof MipsBranch br) {
                if (br.getRt() == Register.ZERO && br.getRs() == cmp.getRd()) {
                    // Check if cmp.getRd() is used later before being redefined or before a label
                    boolean usedLater = false;
                    for (int j = i + 2; j < text.size(); j++) {
                        MipsAssembly future = text.get(j);
                        if (future instanceof MipsLabel)
                            break;
                        if (usesRegister(future, cmp.getRd())) {
                            usedLater = true;
                            break;
                        }
                        if (definesRegister(future, cmp.getRd()))
                            break;
                    }

                    if (!usedLater) {
                        MipsBranch.BranchType newBrType = null;
                        if (br.getBranchType() == MipsBranch.BranchType.BNE) { // bnez
                            newBrType = switch (cmp.getCompareType()) {
                                case SEQ -> MipsBranch.BranchType.BEQ;
                                case SNE -> MipsBranch.BranchType.BNE;
                                default -> null;
                            };
                        } else if (br.getBranchType() == MipsBranch.BranchType.BEQ) { // beqz
                            newBrType = switch (cmp.getCompareType()) {
                                case SEQ -> MipsBranch.BranchType.BNE;
                                case SNE -> MipsBranch.BranchType.BEQ;
                                default -> null;
                            };
                        }

                        if (newBrType != null && cmp.getRt() != null) {
                            text.set(i + 1, new MipsBranch(newBrType, cmp.getRs(), cmp.getRt(), br.getLabel()));
                            text.remove(i);
                            changed = true;
                        }
                    }
                }
            }
        }
        return changed;
    }

    private boolean usesRegister(MipsAssembly instr, Register reg) {
        if (instr instanceof MipsAlu alu) {
            return alu.getRs() == reg || alu.getRt() == reg;
        } else if (instr instanceof MipsBranch br) {
            return br.getRs() == reg || br.getRt() == reg;
        } else if (instr instanceof MipsCompare cmp) {
            return cmp.getRs() == reg || cmp.getRt() == reg;
        } else if (instr instanceof MipsJump jump) {
            return jump.getRd() == reg;
        } else if (instr instanceof MipsLsu lsu) {
            if (lsu.isStoreType()) {
                return lsu.getRd() == reg || lsu.getBase() == reg;
            } else {
                return lsu.getBase() == reg;
            }
        } else if (instr instanceof MarsMove move) {
            return move.getSrc() == reg;
        }
        return false;
    }

    private boolean definesRegister(MipsAssembly instr, Register reg) {
        if (instr instanceof MipsAlu alu) {
            return alu.getRd() == reg;
        } else if (instr instanceof MipsCompare cmp) {
            return cmp.getRd() == reg;
        } else if (instr instanceof MipsLsu lsu) {
            return !lsu.isStoreType() && lsu.getRd() == reg;
        } else if (instr instanceof MarsMove move) {
            return move.getDst() == reg;
        } else if (instr instanceof MarsLi li) {
            return li.getRd() == reg;
        }
        return false;
    }
}
