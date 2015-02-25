package org.apache.felix.http.base.internal.runtime.dto;

import javax.servlet.ServletContext;

import org.apache.felix.http.base.internal.runtime.ServletContextHelperInfo;

public interface ServletContextHelperRuntime
{
    ServletContext getSharedContext();

    ServletContextHelperInfo getContextInfo();
}
