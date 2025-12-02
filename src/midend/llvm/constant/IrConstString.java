package midend.llvm.constant;

import midend.llvm.type.IrArrayType;
import midend.llvm.type.IrBaseType;
import midend.llvm.type.IrPointerType;

public class IrConstString extends IrConstant{
    private final String value;

    public IrConstString(String name,String value) {
        super(new IrPointerType(new IrArrayType(getLength(value),IrBaseType.INT8)),name);
        this.value = value;
    }

    public static int getLength(String s) {
        int ans = 0;
        for(int i = 0;i < s.length(); i++) {
            if(s.charAt(i) != '\\') {
                ans++;
            } else {
                if(i+1 < s.length() && s.charAt(i+1) == 'n') {
                    ans++;
                    i++;
                }
            }
        }
        return ans+1;
    }

    @Override
    public String toString() {
        return this.irName +
                " = constant " +
                ((IrPointerType) this.irType).getTargetType() +
                " c\"" +
                this.value.replaceAll("\\n", "\\\\0A") +
                "\\00\"";
    }
}
