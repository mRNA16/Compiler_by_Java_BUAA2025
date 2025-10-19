package frontend.ast;

import java.util.ArrayList;
import java.util.List;

/**
 * 函数实参列表节点
 */
public class FuncRParams extends AbstractASTNode {
    private final List<Exp> params;
    
    public FuncRParams() {
        super(SyntaxType.FUNC_RPARAMS);
        this.params = new ArrayList<>();
    }
    
    /**
     * 添加实参
     */
    public void addParam(Exp param) {
        if (param != null) {
            params.add(param);
            addChild(param);
        }
    }
    
    /**
     * 获取所有实参
     */
    public List<Exp> getParams() {
        return new ArrayList<>(params);
    }
    
    /**
     * 获取实参数量
     */
    public int getParamCount() {
        return params.size();
    }
}