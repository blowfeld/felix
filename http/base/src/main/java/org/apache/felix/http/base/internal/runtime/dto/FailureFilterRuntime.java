package org.apache.felix.http.base.internal.runtime.dto;

import org.apache.felix.http.base.internal.runtime.FilterInfo;



public class FailureFilterRuntime implements FilterRuntime
{
    private final FilterInfo FilterInfo;

    FailureFilterRuntime(FilterInfo FilterInfo)
    {
        this.FilterInfo = FilterInfo;
    }

    @Override
    public FilterInfo getFilterInfo()
    {
        return FilterInfo;
    }

    @Override
    public Long getContextServiceId()
    {
        return 0L;
    }
}
