package org.apache.felix.http.base.internal.runtime.dto;

import org.apache.felix.http.base.internal.runtime.FilterInfo;



public interface FilterRuntime extends WhiteboardServiceRuntime
{
    FilterInfo getFilterInfo();
}
