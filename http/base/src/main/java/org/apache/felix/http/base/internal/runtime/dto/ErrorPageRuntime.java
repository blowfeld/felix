package org.apache.felix.http.base.internal.runtime.dto;

import java.util.Collection;

public class ErrorPageRuntime {
    private final ServletRuntime servletHandler;
    private final Collection<Integer> errorCodes;
    private final Collection<String> exceptions;

    public ErrorPageRuntime(ServletRuntime servletHandler,
            Collection<Integer> errorCodes,
            Collection<String> exceptions)
    {
        this.servletHandler = servletHandler;
        this.errorCodes = errorCodes;
        this.exceptions = exceptions;
    }

    public ServletRuntime getServletHandler()
    {
        return servletHandler;
    }

    public Collection<Integer> getErrorCodes()
    {
        return errorCodes;
    }

    public Collection<String> getExceptions()
    {
        return exceptions;
    }
}