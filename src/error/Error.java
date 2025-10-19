package error;

public class Error {
    private final int lineNum;
    private final ErrorType errorCode;

    public Error(ErrorType errorCode, int lineNum) {
        this.lineNum = lineNum;
        this.errorCode = errorCode;
    }

    @Override
    public String toString() {
        return this.errorCode.toString();
    }

    public String info() {
        return lineNum + " " + this.errorCode; 
    }

    public int getLineNum() {
        return lineNum;
    }
}
