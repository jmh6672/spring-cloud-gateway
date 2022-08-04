package ipron.cloud.web.model.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CommonResult {

    protected boolean result;

    protected String code;
    
    protected int status;

    protected String title;

    protected String msg;
}
