package org.apache.felix.http.base.internal.runtime.dto;

import javax.servlet.Servlet;

import org.apache.felix.http.base.internal.runtime.ServletInfo;



public class FailureServletRuntime implements ServletRuntime
{
    private final ServletInfo servletInfo;

    FailureServletRuntime(ServletInfo servletInfo)
    {
        this.servletInfo = servletInfo;
    }

    @Override
    public ServletInfo getServletInfo()
    {
        return servletInfo;
    }

    @Override
    public Long getContextServiceId()
    {
        return 0L;
    }

    @Override
    public Servlet getServlet()
    {
        return null;
    }
}
