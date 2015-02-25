package org.apache.felix.http.base.internal.runtime.dto;

import javax.servlet.ServletContext;

import org.apache.felix.http.base.internal.runtime.ServletContextHelperInfo;

public class FailureServletContextHelperRuntime implements ServletContextHelperRuntime
{
    private final ServletContextHelperInfo info;

    public FailureServletContextHelperRuntime(ServletContextHelperInfo info)
    {
        this.info = info;
    }

    @Override
    public ServletContext getSharedContext()
    {
        return null;
    }

    @Override
    public ServletContextHelperInfo getContextInfo()
    {
        return info;
    }
}
