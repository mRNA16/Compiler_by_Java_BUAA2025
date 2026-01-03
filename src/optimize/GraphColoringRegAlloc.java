package optimize;

import midend.llvm.value.IrFunction;
import midend.llvm.value.IrValue;
import midend.llvm.value.IrBasicBlock;
import midend.llvm.instr.Instr;
import midend.llvm.instr.MoveInstr;
import midend.llvm.instr.memory.GepInstr;
import backend.mips.Register;

import java.util.*;

public class GraphColoringRegAlloc extends Optimizer {
    private Map<IrValue, Set<IrValue>> adjList;
    private Map<IrValue, Integer> degree;
    private Set<IrValue> nodes;
    private Stack<IrValue> selectStack;
    private LivenessAnalysis livenessAnalysis;

    public GraphColoringRegAlloc() {
        this.livenessAnalysis = new LivenessAnalysis();
    }

    @Override
    public void optimize() {
        // 确保活跃变量分析是最新的
        livenessAnalysis.optimize();

        for (IrFunction function : irModule.getFunctions()) {
            if (function.getBasicBlocks().isEmpty())
                continue;
            allocateRegister(function);
        }
    }

    private void allocateRegister(IrFunction function) {
        // 1. 初始化数据结构
        adjList = new HashMap<>();
        degree = new HashMap<>();
        nodes = new HashSet<>();
        selectStack = new Stack<>();

        // 2. 收集所有需要分配寄存器的值 (Nodes)
        // 排除前4个参数，因为它们固定在 A0-A3
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

        // 3. 构建冲突图
        buildInterferenceGraph(function);

        // 4. 简化 (Simplify) & 溢出选择 (Spill)
        simplify();

        // 5. 选择颜色 (Select)
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

        // 动态维护度数，因为 simplify 过程中度数会变化
        Map<IrValue, Integer> currentDegree = new HashMap<>(degree);

        while (!workList.isEmpty()) {
            IrValue nodeToRemove = null;

            // 1. 寻找度数 < K 的节点 (Simplify)
            for (IrValue v : workList) {
                if (currentDegree.get(v) < K) {
                    nodeToRemove = v;
                    break;
                }
            }

            // 2. 如果没有 < K 的节点，则需要溢出 (Spill)
            if (nodeToRemove == null) {
                // 使用 SpillCost / Degree 启发式
                // SpillCost = 使用次数 + 定义次数 (SSA 下定义次数为 1)
                double minCost = Double.MAX_VALUE;
                for (IrValue v : workList) {
                    double cost = (1.0 + v.getBeUsedList().size()) / (currentDegree.get(v) + 1);
                    if (cost < minCost) {
                        minCost = cost;
                        nodeToRemove = v;
                    }
                }
            }

            // 移除节点
            workList.remove(nodeToRemove);
            selectStack.push(nodeToRemove);

            // 更新邻居度数
            for (IrValue neighbor : adjList.get(nodeToRemove)) {
                if (workList.contains(neighbor)) {
                    currentDegree.put(neighbor, currentDegree.get(neighbor) - 1);
                }
            }
        }
    }

    private void assignColors(IrFunction function) {
        List<Register> usableRegisters = Register.getUsableRegisters();
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
            for (Register reg : usableRegisters) {
                if (!usedColors.contains(reg)) {
                    assignedReg = reg;
                    break;
                }
            }

            if (assignedReg != null) {
                colors.put(node, assignedReg);
            }
        }
    }
}
