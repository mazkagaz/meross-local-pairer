package org.localpairer.merosslib.model.http.exceptions;

import org.localpairer.merosslib.model.http.ErrorCodes;

public class HttpApiMissingMFAException extends HttpApiException{
    public HttpApiMissingMFAException(ErrorCodes code) {
        super(code);
    }
}
