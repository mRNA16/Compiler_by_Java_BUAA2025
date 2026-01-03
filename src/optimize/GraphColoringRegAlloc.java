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

            // 只需要考虑我们在 nodes 集合中的变量
            live.retainAll(nodes);

            List<Instr> instructions = block.getInstructions();
            for (int i = instructions.size() - 1; i >= 0; i--) {
                Instr instr = instructions.get(i);

                // 如果是定义点
                if (nodes.contains(instr)) {
                    // 对于 Move 指令，如果 live 中包含 src，则不添加冲突边
                    boolean isMove = instr instanceof MoveInstr;
                    IrValue moveSrc = isMove ? ((MoveInstr) instr).getSrcValue() : null;

                    for (IrValue v : live) {
                        if (isMove && v == moveSrc)
                            continue;
                        addEdge(instr, v);
                    }

                    // 定义变量不再活跃
                    live.remove(instr);
                }

                // 添加使用点到 live
                for (IrValue use : instr.getUseValueList()) {
                    addUseToLive(use, live);
                }
            }

            // Fix: 在入口块的开始处，所有 LiveIn 的变量（主要是参数）都互相冲突
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
            // 如果是折叠的 Gep，则其操作数才是真正的使用点
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

        while (!workList.isEmpty()) {
            IrValue nodeToRemove = null;

            for (IrValue v : workList) {
                if (degree.get(v) < K) {
                    nodeToRemove = v;
                    break;
                }
            }

            if (nodeToRemove == null) {
                int maxDegree = -1;
                for (IrValue v : workList) {
                    if (degree.get(v) > maxDegree) {
                        maxDegree = degree.get(v);
                        nodeToRemove = v;
                    }
                }
            }

            workList.remove(nodeToRemove);
            selectStack.push(nodeToRemove);

            for (IrValue neighbor : adjList.get(nodeToRemove)) {
                if (workList.contains(neighbor)) {
                    int currentDegree = degree.get(neighbor);
                    if (currentDegree > 0) {
                        degree.put(neighbor, currentDegree - 1);
                    }
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
