package frontend.ast;

import java.util.ArrayList;
import java.util.List;

/**
 * 函数形参列表节点
 */
public class FuncFParams extends AbstractASTNode {
    private final List<FuncFParam> params;
    
    public FuncFParams() {
        super(SyntaxType.FUNC_FPARAMS);
        this.params = new ArrayList<>();
    }
    
    /**
     * 添加参数
     */
    public void addParam(FuncFParam param) {
        params.add(param);
        if (param != null) {
            addChild(param);
        }
    }
    
    /**
     * 获取所有参数
     */
    public List<FuncFParam> getParams() {
        return new ArrayList<>(params);
    }
    
    /**
     * 获取参数数量
     */
    public int getParamCount() {
        return params.size();
    }
}