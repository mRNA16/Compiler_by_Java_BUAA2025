package error;

public enum ErrorType {
    SINGLE_LOGIC_OP("a"),
    NAME_REDEFINED("b"),
    NAME_UNDEFINED("c"),
    FUNCTION_PARAM_COUNT_MISMATCH("d"),
    FUNCTION_PARAM_TYPE_MISMATCH("e"),
    RETURN_TYPE_MISMATCH("f"),
    MISSING_RETURN("g"),
    MODIFY_CONSTANT("h"),
    MISSING_SEMICOLON("i"),
    MISSING_RIGHT_PARENTHESES("j"),
    MISSING_RIGHT_BRACKETS("k"),
    PRINTF_FORMAT_MISMATCH("l"),
    BREAK_CONTINUE_OUTSIDE_LOOP("m");


    private final String errorCode;

    ErrorType(String errorCode) {
        this.errorCode = errorCode;
    }

    @Override
    public String toString() {
        return errorCode;
    }
}
