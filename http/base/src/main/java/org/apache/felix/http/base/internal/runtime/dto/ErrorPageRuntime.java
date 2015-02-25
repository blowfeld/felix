package org.apache.felix.http.base.internal.runtime.dto;

import java.util.Collection;

import javax.servlet.Servlet;

import org.apache.felix.http.base.internal.runtime.ServletInfo;

public final class ErrorPageRuntime implements ServletRuntime {
    private final ServletRuntime servletRuntime;
    private final Collection<Integer> errorCodes;
    private final Collection<String> exceptions;

    public ErrorPageRuntime(ServletRuntime servletRuntime,
            Collection<Integer> errorCodes,
            Collection<String> exceptions)
    {
        this.servletRuntime = servletRuntime;
        this.errorCodes = errorCodes;
        this.exceptions = exceptions;
    }

    public Collection<Integer> getErrorCodes()
    {
        return errorCodes;
    }

    public Collection<String> getExceptions()
    {
        return exceptions;
    }

    @Override
    public Long getContextServiceId()
    {
        return servletRuntime.getContextServiceId();
    }

    @Override
    public Servlet getServlet()
    {
        return servletRuntime.getServlet();
    }

    @Override
    public ServletInfo getServletInfo()
    {
        return servletRuntime.getServletInfo();
    }
}