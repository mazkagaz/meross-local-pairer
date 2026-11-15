package org.localpairer.merosslib.model.http.exceptions;

import org.localpairer.merosslib.model.http.ErrorCodes;

public class HttpApiInvalidCredentialsException extends HttpApiException {
    public HttpApiInvalidCredentialsException(ErrorCodes code) {
        super(code);
    }
}
