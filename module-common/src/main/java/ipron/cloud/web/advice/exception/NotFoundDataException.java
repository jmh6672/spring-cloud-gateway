package ipron.cloud.web.advice.exception;

import ipron.cloud.web.model.dto.ErrorCode;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NotFoundDataException extends ErrorCommonException {
    public NotFoundDataException(String message) {
        super(message, ErrorCode.NOT_FOUND_DATA);
    }
}
