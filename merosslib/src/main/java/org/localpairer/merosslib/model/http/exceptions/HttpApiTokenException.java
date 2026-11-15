package org.localpairer.merosslib.model.http.exceptions;

import org.localpairer.merosslib.model.http.ErrorCodes;

public class HttpApiTokenException extends HttpApiException{
    public HttpApiTokenException(ErrorCodes code) {
        super(code);
    }
}
