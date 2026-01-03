package optimize;

import midend.llvm.value.IrFunction;
import midend.llvm.value.IrValue;
import midend.llvm.value.IrBasicBlock;
import midend.llvm.instr.Instr;
import midend.llvm.instr.MoveInstr;
import midend.llvm.instr.ctrl.CallInstr;
import midend.llvm.instr.memory.GepInstr;
import backend.mips.Register;
import backend.mips.MipsBuilder;

import java.util.*;

public class GraphColoringRegAlloc extends Optimizer {
    private Map<IrValue, Set<IrValue>> adjList;
    private Map<IrValue, Integer> degree;
    private Set<IrValue> nodes;
    private Set<IrValue> spansCall; // 记录跨越函数调用的变量
    private Stack<IrValue> selectStack;
    private LivenessAnalysis livenessAnalysis;

    public GraphColoringRegAlloc() {
        this.livenessAnalysis = new LivenessAnalysis();
    }

    @Override
    public void optimize() {
        livenessAnalysis.optimize();

        for (IrFunction function : irModule.getFunctions()) {
            if (function.getBasicBlocks().isEmpty())
                continue;
            allocateRegister(function);
        }
    }

    private void allocateRegister(IrFunction function) {
        adjList = new HashMap<>();
        degree = new HashMap<>();
        nodes = new HashSet<>();
        spansCall = new HashSet<>();
        selectStack = new Stack<>();

        // 收集节点
        for (int i = 4; i < function.getParameters().size(); i++) {
            addNode(function.getParameters().get(i));
        }
        for (IrBasicBlock block : function.getBasicBlocks()) {
            for (Instr instr : block.getInstructions()) {
                if (instr instanceof MoveInstr move) {
                    IrValue dst = move.getDstValue();
                    if (livenessAnalysis.isAllocatable(dst)) {
                        addNode(dst);
                    }
                }
                if (livenessAnalysis.isAllocatable(instr)) {
                    addNode(instr);
                }
            }
        }

        buildInterferenceGraph(function);
        simplify();
        assignColors(function);
    }

    private void addNode(IrValue value) {
        if (!nodes.contains(value)) {
            nodes.add(value);
            adjList.put(value, new HashSet<>());
            degree.put(value, 0);
        }
    }

    private void addEdge(IrValue u, IrValue v) {
        if (u == v)
            return;
        if (!nodes.contains(u) || !nodes.contains(v))
            return;

        if (adjList.get(u).add(v)) {
            degree.put(u, degree.get(u) + 1);
        }
        if (adjList.get(v).add(u)) {
            degree.put(v, degree.get(v) + 1);
        }
    }

    private void buildInterferenceGraph(IrFunction function) {
        for (IrBasicBlock block : function.getBasicBlocks()) {
            HashSet<IrValue> live = new HashSet<>(block.getLiveOut());
            live.retainAll(nodes);

            List<Instr> instructions = block.getInstructions();
            for (int i = instructions.size() - 1; i >= 0; i--) {
                Instr instr = instructions.get(i);

                // 定义点处理
                if (instr instanceof MoveInstr move) {
                    IrValue dst = move.getDstValue();
                    if (nodes.contains(dst)) {
                        IrValue src = move.getSrcValue();
                        for (IrValue v : live) {
                            if (v == src)
                                continue;
                            addEdge(dst, v);
                        }
                        live.remove(dst);
                    }
                } else if (nodes.contains(instr)) {
                    for (IrValue v : live) {
                        addEdge(instr, v);
                    }
                    live.remove(instr);
                }

                // 核心改进：识别跨调用变量
                if (instr instanceof CallInstr) {
                    spansCall.addAll(live);
                }

                for (IrValue use : instr.getUseValueList()) {
                    addUseToLive(use, live);
                }
            }

            if (block == function.getEntryBlock()) {
                List<IrValue> liveList = new ArrayList<>(live);
                for (int i = 0; i < liveList.size(); i++) {
                    for (int j = i + 1; j < liveList.size(); j++) {
                        addEdge(liveList.get(i), liveList.get(j));
                    }
                }
            }
        }
    }

    private void addUseToLive(IrValue val, HashSet<IrValue> live) {
        if (val == null)
            return;
        if (val instanceof GepInstr gep && gep.canBeFoldedIntoAllUsers()) {
            for (IrValue op : gep.getUseValueList()) {
                addUseToLive(op, live);
            }
        } else if (nodes.contains(val)) {
            live.add(val);
        }
    }

    private void simplify() {
        int K = Register.getUsableRegisters().size();
        Set<IrValue> workList = new HashSet<>(nodes);
        Map<IrValue, Integer> currentDegree = new HashMap<>(degree);

        while (!workList.isEmpty()) {
            IrValue nodeToRemove = null;
            for (IrValue v : workList) {
                if (currentDegree.get(v) < K) {
                    nodeToRemove = v;
                    break;
                }
            }

            if (nodeToRemove == null) {
                double minCost = Double.MAX_VALUE;
                for (IrValue v : workList) {
                    // 跨调用变量的溢出代价更高
                    double weight = spansCall.contains(v) ? 10.0 : 1.0;
                    double cost = (weight + v.getBeUsedList().size()) / (currentDegree.get(v) + 1);
                    if (cost < minCost) {
                        minCost = cost;
                        nodeToRemove = v;
                    }
                }
            }

            workList.remove(nodeToRemove);
            selectStack.push(nodeToRemove);

            for (IrValue neighbor : adjList.get(nodeToRemove)) {
                if (workList.contains(neighbor)) {
                    currentDegree.put(neighbor, currentDegree.get(neighbor) - 1);
                }
            }
        }
    }

    private void assignColors(IrFunction function) {
        List<Register> usableRegisters = Register.getUsableRegisters();

        // 将可用寄存器分为 T 类和 S 类
        List<Register> tRegs = new ArrayList<>();
        List<Register> sRegs = new ArrayList<>();
        for (Register reg : usableRegisters) {
            if (MipsBuilder.isCalleeSaved(reg))
                sRegs.add(reg);
            else
                tRegs.add(reg);
        }

        Map<IrValue, Register> colors = function.getValueRegisterMap();

        while (!selectStack.isEmpty()) {
            IrValue node = selectStack.pop();
            Set<Register> usedColors = new HashSet<>();
            for (IrValue neighbor : adjList.get(node)) {
                if (colors.containsKey(neighbor)) {
                    usedColors.add(colors.get(neighbor));
                }
            }

            Register assignedReg = null;
            if (spansCall.contains(node)) {
                // 跨调用变量：优先选 S 寄存器，避开 T 寄存器
                assignedReg = pickRegister(sRegs, usedColors);
                if (assignedReg == null)
                    assignedReg = pickRegister(tRegs, usedColors);
            } else {
                // 非跨调用变量：优先选 T 寄存器，节省 Prologue/Epilogue 开销
                assignedReg = pickRegister(tRegs, usedColors);
                if (assignedReg == null)
                    assignedReg = pickRegister(sRegs, usedColors);
            }

            if (assignedReg != null) {
                colors.put(node, assignedReg);
            }
        }
    }

    private Register pickRegister(List<Register> preferred, Set<Register> used) {
        for (Register reg : preferred) {
            if (!used.contains(reg))
                return reg;
        }
        return null;
    }
}
