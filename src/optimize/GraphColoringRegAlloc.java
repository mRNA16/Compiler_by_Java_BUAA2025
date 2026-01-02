package optimize;

import midend.llvm.value.IrFunction;
import midend.llvm.value.IrValue;
import midend.llvm.value.IrBasicBlock;
import midend.llvm.instr.Instr;
import midend.llvm.instr.MoveInstr;
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
                    // 对于 Move 指令，如果 live 中包含 src，则不添加冲突边 (Coalescing 预备，虽然这里没做合并)
                    // 但为了简单和正确性，先按标准处理：
                    // Move a <- b: a interferes with live \ {b}

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
                    if (nodes.contains(use)) {
                        live.add(use);
                    }
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

    private void simplify() {
        int K = Register.getUsableRegisters().size();
        // 使用临时集合避免修改 nodes
        Set<IrValue> workList = new HashSet<>(nodes);

        while (!workList.isEmpty()) {
            // 寻找度数 < K 的节点
            IrValue nodeToRemove = null;

            // 优先找度数小的
            for (IrValue v : workList) {
                if (degree.get(v) < K) {
                    nodeToRemove = v;
                    break;
                }
            }

            // 如果没有 < K 的节点，则需要溢出 (Optimistic Coloring: 只是推入栈，也许能着色)
            if (nodeToRemove == null) {
                // 启发式：选择度数最大的节点溢出
                int maxDegree = -1;
                for (IrValue v : workList) {
                    if (degree.get(v) > maxDegree) {
                        maxDegree = degree.get(v);
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
                    // 注意：这里只是逻辑上移除，实际 degree map 还是保留原值或者需要动态维护
                    // 为了简单，我们这里不修改 degree map 的值，而是假设移除后不再贡献冲突
                    // 但标准的 simplify 需要减少邻居的度数
                    // 让我们用一个动态的 degree map
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
        // 清除旧的分配 (除了参数 A0-A3，但 IrFunction.toMips 会重置它们，所以这里我们只负责添加新的)
        // 注意：IrFunction.getValueRegisterMap() 返回的是引用，直接修改它

        while (!selectStack.isEmpty()) {
            IrValue node = selectStack.pop();
            Set<Register> usedColors = new HashSet<>();

            // 检查已着色的邻居
            for (IrValue neighbor : adjList.get(node)) {
                // 邻居可能在栈中（还没着色），或者已经着色
                // 我们只关心已经着色的
                if (colors.containsKey(neighbor)) {
                    usedColors.add(colors.get(neighbor));
                }
            }

            // 选择一个未使用的颜色
            Register assignedReg = null;
            for (Register reg : usableRegisters) {
                if (!usedColors.contains(reg)) {
                    assignedReg = reg;
                    break;
                }
            }

            if (assignedReg != null) {
                colors.put(node, assignedReg);
            } else {
                // 溢出：不分配寄存器，MipsBuilder 会自动分配栈空间
                // System.out.println("Spilled: " + node);
            }
        }
    }
}
