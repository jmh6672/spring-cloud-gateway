package ipron.cloud.web.advice.exception;

import ipron.cloud.web.model.dto.ErrorCode;

public class ErrorCommonException extends RuntimeException {
    private ErrorCode code;

    public ErrorCommonException() { super(); }
    public ErrorCommonException(String msg) { super(msg); }
    public ErrorCommonException(Throwable t) { super(t); }
    public ErrorCommonException(String msg, Throwable t) {
        super(msg, t);
    }

    public ErrorCommonException(String msg, ErrorCode code) {
        super(msg);
        this.code = code;
    }
    public ErrorCommonException(ErrorCode code) {
        super(code.getMsg());
        this.code = code;
    }

    public ErrorCode getErrorCode() {
        return code;
    }
}
