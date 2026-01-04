package backend.mips;

import backend.mips.assembly.*;
import backend.mips.assembly.fake.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.ListIterator;
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
        ListIterator<MipsAssembly> it = text.listIterator();

        while (it.hasNext()) {
            MipsAssembly instr = it.next();

            // 1. move $r, $r
            if (instr instanceof MarsMove move) {
                if (move.getDst() == move.getSrc()) {
                    it.remove();
                    changed = true;
                    continue;
                }
            }

            // 2. addi $r, $r, 0
            if (instr instanceof MipsAlu alu) {
                if (alu.getImmediate() != null && alu.getImmediate() == 0) {
                    if (alu.getAluType() == MipsAlu.AluType.ADDI || alu.getAluType() == MipsAlu.AluType.ADDIU) {
                        if (alu.getRd() == alu.getRs()) {
                            it.remove();
                            changed = true;
                            continue;
                        } else {
                            it.set(new MarsMove(alu.getRd(), alu.getRs()));
                            changed = true;
                        }
                    }
                }
                // add $r, $r, $zero
                if (alu.getRt() == Register.ZERO
                        && (alu.getAluType() == MipsAlu.AluType.ADD || alu.getAluType() == MipsAlu.AluType.ADDU)) {
                    if (alu.getRd() == alu.getRs()) {
                        it.remove();
                        changed = true;
                        continue;
                    } else {
                        it.set(new MarsMove(alu.getRd(), alu.getRs()));
                        changed = true;
                    }
                }
            }

            // 3. li $r, 0 -> move $r, $zero
            if (instr instanceof MarsLi li) {
                if (li.getNumber() == 0) {
                    it.set(new MarsMove(li.getRd(), Register.ZERO));
                    changed = true;
                }
            }
        }
        return changed;
    }

    private boolean simplifyControlFlow() {
        ArrayList<MipsAssembly> text = module.getTextSegment();
        boolean changed = false;

        // 1. Jump to next instruction
        int i = 0;
        while (i < text.size()) {
            MipsAssembly instr = text.get(i);
            boolean removed = false;
            if (instr instanceof MipsJump jump && jump.getJumpType() == MipsJump.JumpType.J) {
                int nextIdx = next(text, i);
                if (nextIdx < text.size() && text.get(nextIdx) instanceof MipsLabel label) {
                    if (label.getLabel().equals(jump.getTargetLabel())) {
                        text.remove(i);
                        changed = true;
                        removed = true;
                    }
                }
            }
            if (!removed) {
                i++;
            }
        }

        // 2. Jump to jump
        Map<String, String> jumpMap = new HashMap<>();
        for (int j = 0; j < text.size(); j++) {
            if (text.get(j) instanceof MipsLabel label) {
                int nextIdx = j + 1;
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
            for (int k = 0; k < text.size(); k++) {
                MipsAssembly instr = text.get(k);
                if (instr instanceof MipsJump jump && jump.getJumpType() == MipsJump.JumpType.J) {
                    String target = jump.getTargetLabel();
                    if (jumpMap.containsKey(target)) {
                        String newTarget = jumpMap.get(target);
                        if (!newTarget.equals(target)) {
                            text.set(k, new MipsJump(MipsJump.JumpType.J, newTarget));
                            changed = true;
                        }
                    }
                } else if (instr instanceof MipsBranch branch) {
                    String target = branch.getLabel();
                    if (jumpMap.containsKey(target)) {
                        String newTarget = jumpMap.get(target);
                        if (!newTarget.equals(target)) {
                            if (branch.getRt() != null) {
                                text.set(k, new MipsBranch(branch.getBranchType(), branch.getRs(), branch.getRt(),
                                        newTarget));
                            } else {
                                text.set(k, new MipsBranch(branch.getBranchType(), branch.getRs(), newTarget));
                            }
                            changed = true;
                        }
                    }
                }
            }
        }

        // 3. Jump over jump
        // beq $r1, $r2, L1; j L2; L1: -> bne $r1, $r2, L2; L1:
        i = 0;
        while (i < text.size()) {
            MipsAssembly i1 = text.get(i);
            int idx2 = next(text, i);
            if (idx2 >= text.size())
                break;
            MipsAssembly i2 = text.get(idx2);
            int idx3 = next(text, idx2);
            if (idx3 >= text.size()) {
                i++;
                continue;
            }
            MipsAssembly i3 = text.get(idx3);

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
                        text.remove(idx2);
                        changed = true;
                    }
                }
            }
            i++;
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
        int i = 0;
        while (i < text.size()) {
            MipsAssembly instr1 = text.get(i);
            int idx2 = next(text, i);
            if (idx2 >= text.size())
                break;
            MipsAssembly instr2 = text.get(idx2);

            boolean removed = false;
            if (instr1 instanceof MipsLsu lsu1 && lsu1.getLsuType() == MipsLsu.LsuType.SW) {
                if (instr2 instanceof MipsLsu lsu2 && lsu2.getLsuType() == MipsLsu.LsuType.LW) {
                    if (lsu1.getBase() == lsu2.getBase() &&
                            lsu1.getOffset().equals(lsu2.getOffset()) &&
                            lsu1.getLabel() == null && lsu2.getLabel() == null) {

                        if (lsu1.getRd() == lsu2.getRd()) {
                            text.remove(idx2);
                            changed = true;
                            removed = true;
                        } else {
                            text.set(idx2, new MarsMove(lsu2.getRd(), lsu1.getRd()));
                            changed = true;
                        }
                    }
                }
            }
            if (!removed) {
                i++;
            }
        }
        return changed;
    }

    private boolean mergeCompareAndBranch() {
        ArrayList<MipsAssembly> text = module.getTextSegment();
        boolean changed = false;
        int i = 0;
        while (i < text.size()) {
            MipsAssembly i1 = text.get(i);
            int idx2 = next(text, i);
            if (idx2 >= text.size())
                break;
            MipsAssembly i2 = text.get(idx2);

            boolean removed = false;
            if (i1 instanceof MipsCompare cmp && i2 instanceof MipsBranch br) {
                if (br.getRt() == Register.ZERO && br.getRs() == cmp.getRd()) {
                    // Check if cmp.getRd() is used later before being redefined or before a label
                    boolean usedLater = false;
                    for (int j = idx2 + 1; j < text.size(); j++) {
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
                            text.set(idx2, new MipsBranch(newBrType, cmp.getRs(), cmp.getRt(), br.getLabel()));
                            text.remove(i);
                            changed = true;
                            removed = true;
                        }
                    }
                }
            }
            if (!removed) {
                i++;
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
        } else if (instr instanceof MipsMdu mdu) {
            return mdu.getRs() == reg || mdu.getRt() == reg || (mdu.getMduType() != MipsMdu.MduType.MFHI
                    && mdu.getMduType() != MipsMdu.MduType.MFLO && mdu.getRd() == reg);
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
        } else if (instr instanceof MipsMdu mdu) {
            return (mdu.getMduType() == MipsMdu.MduType.MFHI || mdu.getMduType() == MipsMdu.MduType.MFLO)
                    && mdu.getRd() == reg;
        } else if (instr instanceof MipsJump jump) {
            return jump.getJumpType() == MipsJump.JumpType.JAL && reg == Register.RA;
        }
        return false;
    }

    private int next(ArrayList<MipsAssembly> text, int i) {
        int next = i + 1;
        while (next < text.size() && text.get(next) instanceof MipsAnnotation) {
            next++;
        }
        return next;
    }
}
