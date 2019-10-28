package com.runisys.web.rest;

import com.runisys.ErrorCodes;
import com.runisys.HosException;
import com.runisys.web.security.IOperationAccessControl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.ControllerAdvice;

import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class BaseController {

    @Autowired
    @Qualifier("DefaultAccessControl")
    protected IOperationAccessControl operationAccessControl;

    public Map<String, Object> exceptionHandler(Exception ex, HttpServletResponse response) {
        ex.printStackTrace();
        if (HosException.class.isAssignableFrom(ex.getClass())) {
            HosException hosException = (HosException) ex;
            if (hosException.errorCode() == ErrorCodes.ERROR_PERMISSION_DENIED) {
                response.setStatus(403);
            } else {
                response.setStatus(500);
            }
            return getResultMap(hosException.errorCode(), null, hosException.errorMessage(), null);
        } else {
            response.setStatus(500);
            return getResultMap(500, null, ex.getMessage(), null);
        }

    }

    protected Map<String, Object> getResult(Object data) {
        return getResultMap(null, data, null, null);
    }

    protected Map<String, Object> getResult(Object data, Map<String, Object> extraMap) {
        return getResultMap(null, data, null, extraMap);
    }

    protected Map<String, Object> getError(int errorCode, String errorMsg) {
        return getResultMap(errorCode, null, errorMsg, null);
    }

    private Map<String, Object> getResultMap(Integer code, Object data, String msg, Map<String, Object> extraMap) {
        Map<String, Object> map = new HashMap<>();
        if (code == null || code.equals(200)) {
            map.put("code", 200);
            map.put("data", data);
        } else {
            map.put("code", code);
            map.put("msg", msg);
        }
        if (extraMap != null && !extraMap.isEmpty()) {
            map.putAll(extraMap);
        }
        return map;
    }
}
