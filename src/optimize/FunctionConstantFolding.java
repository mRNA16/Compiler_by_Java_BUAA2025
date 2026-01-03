package optimize;

import midend.llvm.constant.IrConstInt;
import midend.llvm.instr.Instr;
import midend.llvm.instr.calc.CalculateInstr;
import midend.llvm.instr.comp.ICompInstr;
import midend.llvm.instr.ctrl.BrCondInstr;
import midend.llvm.instr.ctrl.BrInstr;
import midend.llvm.instr.ctrl.CallInstr;
import midend.llvm.instr.ctrl.ReturnInstr;
import midend.llvm.instr.phi.PhiInstr;
import midend.llvm.value.IrBasicBlock;
import midend.llvm.value.IrFunction;
import midend.llvm.value.IrValue;
import midend.llvm.value.IrParameter;

import java.util.*;

/**
 * 函数常量折叠优化器
 * 识别纯函数调用，如果参数全为常量，则在编译期求值
 */
public class FunctionConstantFolding extends Optimizer {
    private boolean changed;
    private final Map<IrFunction, Boolean> pureCache = new HashMap<>();

    @Override
    public void optimize() {
        changed = true;
        while (changed) {
            changed = false;
            pureCache.clear();
            for (IrFunction function : irModule.getFunctions()) {
                for (IrBasicBlock block : function.getBasicBlocks()) {
                    optimizeBlock(block);
                }
            }
        }
    }

    private void optimizeBlock(IrBasicBlock block) {
        Iterator<Instr> iterator = block.getInstructions().iterator();
        while (iterator.hasNext()) {
            Instr instr = iterator.next();
            if (instr instanceof CallInstr call) {
                IrFunction func = call.getFunction();
                if (isPure(func)) {
                    List<IrValue> args = call.getArgs();
                    boolean allConst = true;
                    List<Integer> constArgs = new ArrayList<>();
                    for (IrValue arg : args) {
                        if (arg instanceof IrConstInt ci) {
                            constArgs.add(ci.getValue());
                        } else {
                            allConst = false;
                            break;
                        }
                    }

                    if (allConst) {
                        Integer result = evaluate(func, constArgs, 0);
                        if (result != null) {
                            IrConstInt resConst = new IrConstInt(result);
                            call.modifyAllUsersToNewValue(resConst);
                            call.removeAllValueUse();
                            iterator.remove();
                            changed = true;
                        }
                    }
                }
            }
        }
    }

    private boolean isPure(IrFunction func) {
        if (pureCache.containsKey(func))
            return pureCache.get(func);

        // 初始假设为 true 以处理递归
        pureCache.put(func, true);

        if (func.getBasicBlocks().isEmpty()) {
            pureCache.put(func, false);
            return false;
        }

        for (IrBasicBlock block : func.getBasicBlocks()) {
            for (Instr instr : block.getInstructions()) {
                switch (instr.getInstrType()) {
                    case STORE:
                    case LOAD:
                    case IO:
                    case GEP:
                        pureCache.put(func, false);
                        return false;
                    case CALL:
                        CallInstr call = (CallInstr) instr;
                        if (!isPure(call.getFunction())) {
                            pureCache.put(func, false);
                            return false;
                        }
                        break;
                    default:
                        break;
                }
            }
        }
        return true;
    }

    private Integer evaluate(IrFunction func, List<Integer> args, int callDepth) {
        if (callDepth > 100)
            return null; // 限制递归深度

        Map<IrValue, Integer> valueMap = new HashMap<>();
        List<IrParameter> params = func.getParameters();
        for (int i = 0; i < params.size(); i++) {
            valueMap.put(params.get(i), args.get(i));
        }

        IrBasicBlock currentBlock = func.getEntryBlock();
        IrBasicBlock prevBlock = null;
        int instrCount = 0;
        int maxInstrCount = 10000; // 防止死循环

        while (instrCount < maxInstrCount) {
            for (Instr instr : currentBlock.getInstructions()) {
                instrCount++;
                if (instr instanceof CalculateInstr calc) {
                    int l = getVal(calc.getL(), valueMap);
                    int r = getVal(calc.getR(), valueMap);
                    int res = switch (calc.getCalculateType()) {
                        case ADD -> l + r;
                        case SUB -> l - r;
                        case MUL -> l * r;
                        case SDIV -> r != 0 ? l / r : 0;
                        case SREM -> r != 0 ? l % r : 0;
                        case AND -> l & r;
                        case OR -> l | r;
                    };
                    valueMap.put(calc, res);
                } else if (instr instanceof ICompInstr comp) {
                    int l = getVal(comp.getL(), valueMap);
                    int r = getVal(comp.getR(), valueMap);
                    boolean res = switch (comp.getCompType()) {
                        case EQ -> l == r;
                        case NE -> l != r;
                        case SGT -> l > r;
                        case SGE -> l >= r;
                        case SLT -> l < r;
                        case SLE -> l <= r;
                    };
                    valueMap.put(comp, res ? 1 : 0);
                } else if (instr instanceof midend.llvm.instr.convert.ZextInstr zext) {
                    valueMap.put(zext, getVal(zext.getOriginValue(), valueMap));
                } else if (instr instanceof midend.llvm.instr.convert.TruncInstr trunc) {
                    valueMap.put(trunc, getVal(trunc.getOriginValue(), valueMap));
                } else if (instr instanceof PhiInstr phi) {
                    IrValue phiVal = phi.getValueFromBlock(prevBlock);
                    valueMap.put(phi, getVal(phiVal, valueMap));
                } else if (instr instanceof BrInstr br) {
                    prevBlock = currentBlock;
                    currentBlock = br.getTargetBlock();
                    break;
                } else if (instr instanceof BrCondInstr brCond) {
                    int cond = getVal(brCond.getCond(), valueMap);
                    prevBlock = currentBlock;
                    currentBlock = (cond != 0) ? brCond.getSucBlock() : brCond.getFailBlock();
                    break;
                } else if (instr instanceof ReturnInstr ret) {
                    if (ret.getReturnValue() == null)
                        return 0;
                    return getVal(ret.getReturnValue(), valueMap);
                } else if (instr instanceof CallInstr call) {
                    List<Integer> callArgs = new ArrayList<>();
                    for (IrValue arg : call.getArgs()) {
                        callArgs.add(getVal(arg, valueMap));
                    }
                    Integer res = evaluate(call.getFunction(), callArgs, callDepth + 1);
                    if (res == null)
                        return null;
                    valueMap.put(call, res);
                }
            }
        }
        return null;
    }

    private int getVal(IrValue val, Map<IrValue, Integer> valueMap) {
        if (val instanceof IrConstInt ci)
            return ci.getValue();
        return valueMap.getOrDefault(val, 0);
    }
}
