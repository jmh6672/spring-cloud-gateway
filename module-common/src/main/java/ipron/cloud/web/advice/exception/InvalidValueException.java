package ipron.cloud.web.advice.exception;

import ipron.cloud.web.model.dto.ErrorCode;

public class InvalidValueException extends ErrorCommonException {
    public InvalidValueException(String value) {
        super(value, ErrorCode.INVALID_TYPE_VALUE);
    }

    public InvalidValueException(String value, ErrorCode errorCode) {
        super(value, errorCode);
    }
}
