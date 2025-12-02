package midend.llvm;

import midend.llvm.constant.IrConstString;
import midend.llvm.value.IrFunction;
import midend.llvm.value.IrGlobalValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IrModule extends IrNode{
    private final List<String> declares;
    private final Map<String, IrConstString> stringConstantMap;
    private final List<IrGlobalValue> globalValues;
    private final List<IrFunction> functions;

    public IrModule() {
        this.declares = new ArrayList<>();
        this.stringConstantMap = new HashMap<>();
        this.globalValues = new ArrayList<>();
        this.functions = new ArrayList<>();

        this.declares.add("declare i32 @getint()");
        this.declares.add("declare i32 @getch()");
        this.declares.add("declare void @putint(i32)");
        this.declares.add("declare void @putch(i32)");
        this.declares.add("declare void @putstr(i8*)");
    }

    public void addIrFunction(IrFunction function) {
        this.functions.add(function);
    }

    public void addIrGlobalValue(IrGlobalValue globalValue) {
        this.globalValues.add(globalValue);
    }

    public List<IrFunction> getFunctions() {
        return this.functions;
    }

    public List<IrGlobalValue> getGlobalValues() {
        return this.globalValues;
    }

    public IrConstString getNewConstantStringIr(String string) {
        if (stringConstantMap.containsKey(string)) {
            return stringConstantMap.get(string);
        } else {
            IrConstString irConstString =
                    new IrConstString(IrBuilder.getStringNameIr(), string);
            this.stringConstantMap.put(string, irConstString);
            return irConstString;
        }
    }

    public void skipBlankBlock(){
        for(IrFunction function : this.functions){
            function.skipBlankBlock();
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        for (String declare : declares) {
            builder.append(declare);
            builder.append("\n");
        }
        builder.append("\n");

        List<Map.Entry<String, IrConstString>> entryList =
                new ArrayList<>(stringConstantMap.entrySet());
        entryList.sort((o1, o2) ->
                CharSequence.compare(o1.getValue().getIrName(), o2.getValue().getIrName()));
        for (Map.Entry<String, IrConstString> entry : entryList) {
            builder.append(entry.getValue());
            builder.append("\n");
        }
        builder.append("\n");

        for (IrGlobalValue globalValue : globalValues) {
            builder.append(globalValue);
            builder.append("\n");
        }
        builder.append("\n");

        for (IrFunction irFunction : functions) {
            builder.append(irFunction.toString());
            builder.append("\n\n");
        }

        return builder.toString();
    }
}
