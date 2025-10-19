package error;

import java.util.ArrayList;
import java.util.Comparator;

public class ErrorRecorder {
    private final ArrayList<Error> errorList;
    private boolean active = true;

    // 单例懒加载ErrorRecorder
    private ErrorRecorder() {
        this.errorList = new ArrayList<>();
    }

    private static class Holder {
        private static final ErrorRecorder INSTANCE = new ErrorRecorder();
    }

    public static ErrorRecorder getErrorRecorder() {
        return Holder.INSTANCE;
    }

    public void addError(Error e) {
        if(active) this.errorList.add(e);
    }

    public ArrayList<Error> getErrors() {
        this.errorList.sort(Comparator.comparingInt(Error::getLineNum));
        return this.errorList;
    }

    public static boolean haveError(){
        return !Holder.INSTANCE.errorList.isEmpty();
    }

    public void negative() {
        this.active = false;
    }

    public void positive() {
        this.active = true;
    }
}
