package error;

public enum ErrorType {
    SINGLE_LOGIC_OP("a"),
    MISSING_SEMICOLON("i"),
    MISSING_RIGHT_PARENTHESES("j"),
    MISSING_RIGHT_BRACKETS("k");


    private final String errorCode;

    ErrorType(String errorCode) {
        this.errorCode = errorCode;
    }

    @Override
    public String toString() {
        return errorCode;
    }
}
