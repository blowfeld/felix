/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.http.base.internal.runtime.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.http.base.internal.runtime.AbstractInfo;
import org.apache.felix.http.base.internal.runtime.FilterInfo;
import org.apache.felix.http.base.internal.runtime.ListenerInfo;
import org.apache.felix.http.base.internal.runtime.ResourceInfo;
import org.apache.felix.http.base.internal.runtime.ServletContextHelperInfo;
import org.apache.felix.http.base.internal.runtime.ServletInfo;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.runtime.dto.FailedErrorPageDTO;
import org.osgi.service.http.runtime.dto.FailedFilterDTO;
import org.osgi.service.http.runtime.dto.FailedListenerDTO;
import org.osgi.service.http.runtime.dto.FailedResourceDTO;
import org.osgi.service.http.runtime.dto.FailedServletContextDTO;
import org.osgi.service.http.runtime.dto.FailedServletDTO;

public final class FailureRuntime
{
    private static final FailedServletDTO[] SERVLET_DTO_ARRAY = new FailedServletDTO[0];
    private static final FailedFilterDTO[] FILTER_DTO_ARRAY = new FailedFilterDTO[0];
    private static final FailedResourceDTO[] RESOURCE_DTO_ARRAY = new FailedResourceDTO[0];
    private static final FailedErrorPageDTO[] ERROR_PAGE_DTO_ARRAY = new FailedErrorPageDTO[0];
    private static final FailedListenerDTO[] LISTENER_DTO_ARRAY = new FailedListenerDTO[0];
    private static final FailedServletContextDTO[] CONTEXT_DTO_ARRAY = new FailedServletContextDTO[0];

    private final Map<ServletContextHelperRuntime, Integer> contextRuntimes;
    private final Map<ServletRuntime, Integer> servletRuntimes;
    private final Map<FilterRuntime, Integer> filterRuntimes;
    private final Map<ServletRuntime, Integer> resourceRuntimes;
    private final Map<ErrorPageRuntime, Integer> errorPageRuntimes;
    private final Map<ServiceReference<?>, Integer> listenerRuntimes;

    public FailureRuntime(Map<ServletContextHelperRuntime, Integer> contextRuntimes,
            Map<ServiceReference<?>, Integer> listenerRuntimes,
            Map<ServletRuntime, Integer> servletRuntimes,
            Map<FilterRuntime, Integer> filterRuntimes,
            Map<ServletRuntime, Integer> resourceRuntimes,
            Map<ErrorPageRuntime, Integer> errorPageRuntimes)
    {
        this.contextRuntimes = contextRuntimes;
        this.servletRuntimes = servletRuntimes;
        this.filterRuntimes = filterRuntimes;
        this.resourceRuntimes = resourceRuntimes;
        this.listenerRuntimes = listenerRuntimes;
        this.errorPageRuntimes = errorPageRuntimes;
    }

    public static FailureRuntime empty()
    {
        return new FailureRuntime(Collections.<ServletContextHelperRuntime, Integer>emptyMap(),
                Collections.<ServiceReference<?>, Integer>emptyMap(),
                Collections.<ServletRuntime, Integer>emptyMap(),
                Collections.<FilterRuntime, Integer>emptyMap(),
                Collections.<ServletRuntime, Integer>emptyMap(),
                Collections.<ErrorPageRuntime, Integer>emptyMap());
    }

    public static FailureRuntime forServiceInfos(Map<AbstractInfo<?>, Integer> failureInfos)
    {
        Map<ServletContextHelperRuntime, Integer> contextRuntimes = new HashMap<ServletContextHelperRuntime, Integer>();
        Map<ServletRuntime, Integer> servletRuntimes = new HashMap<ServletRuntime, Integer>();
        Map<FilterRuntime, Integer> filterRuntimes = new HashMap<FilterRuntime, Integer>();
        Map<ServletRuntime, Integer> resourceRuntimes = new HashMap<ServletRuntime, Integer>();
        Map<ErrorPageRuntime, Integer> errorPageRuntimes = new HashMap<ErrorPageRuntime, Integer>();
        Map<ServiceReference<?>, Integer> listenerRuntimes = new HashMap<ServiceReference<?>, Integer>();

        for (AbstractInfo<?> info : failureInfos.keySet())
        {
            if (info instanceof ServletContextHelperInfo)
            {
                ServletContextHelperRuntime servletRuntime = new FailureServletContextHelperRuntime((ServletContextHelperInfo) info);
                contextRuntimes.put(servletRuntime, failureInfos.get(info));
            }
            else if (info instanceof ServletInfo)
            {
                ServletRuntime servletRuntime = new FailureServletRuntime((ServletInfo) info);
                servletRuntimes.put(servletRuntime, failureInfos.get(info));
            }
            else if (info instanceof FilterInfo)
            {
                FilterRuntime filterRuntime = new FailureFilterRuntime((FilterInfo) info);
                filterRuntimes.put(filterRuntime, failureInfos.get(info));
            }
            else if (info instanceof ResourceInfo)
            {
                ServletRuntime servletRuntime = new FailureServletRuntime(new ServletInfo((ResourceInfo) info));
                resourceRuntimes.put(servletRuntime, failureInfos.get(info));
            }
            else if (info instanceof ListenerInfo)
            {
                ServiceReference<?> serviceReference = ((ListenerInfo<?>) info).getServiceReference();
                listenerRuntimes.put(serviceReference, failureInfos.get(info));
            }
            else
            {
                throw new IllegalArgumentException("Unsupported info type: " + info.getClass());
            }
        }

        return new FailureRuntime(contextRuntimes,
                listenerRuntimes,
                servletRuntimes,
                filterRuntimes,
                resourceRuntimes,
                errorPageRuntimes);
    }

    public FailedServletDTO[] getServletDTOs()
    {
        List<FailedServletDTO> servletDTOs = new ArrayList<FailedServletDTO>();
        for (Map.Entry<ServletRuntime, Integer> failure : servletRuntimes.entrySet())
        {
            servletDTOs.add(getServletDTO(failure.getKey(), failure.getValue()));
        }
        return servletDTOs.toArray(SERVLET_DTO_ARRAY);
    }

    private FailedServletDTO getServletDTO(ServletRuntime failedServlet, int failureCode)
    {
        ServletDTOBuilder<FailedServletDTO> dtoBuilder = new ServletDTOBuilder<FailedServletDTO>(DTOSuppliers.FAILED_SERVLET);
        FailedServletDTO servletDTO = dtoBuilder.buildDTO(failedServlet, 0);
        servletDTO.failureReason = failureCode;
        return servletDTO;
    }

    public FailedFilterDTO[] getFilterDTOs()
    {
        List<FailedFilterDTO> filterDTOs = new ArrayList<FailedFilterDTO>();
        for (Map.Entry<FilterRuntime, Integer> failure : filterRuntimes.entrySet())
        {
            filterDTOs.add(getFilterDTO(failure.getKey(), failure.getValue()));
        }
        return filterDTOs.toArray(FILTER_DTO_ARRAY);
    }

    private FailedFilterDTO getFilterDTO(FilterRuntime failedFilter, int failureCode)
    {
        FilterDTOBuilder<FailedFilterDTO> dtoBuilder = new FilterDTOBuilder<FailedFilterDTO>(DTOSuppliers.FAILED_FILTER);
        FailedFilterDTO filerDTO = dtoBuilder.buildDTO(failedFilter, 0);
        filerDTO.failureReason = failureCode;
        return filerDTO;
    }

    public FailedResourceDTO[] getResourceDTOs()
    {
        List<FailedResourceDTO> resourceDTOs = new ArrayList<FailedResourceDTO>();
        for (Map.Entry<ServletRuntime, Integer> failure : resourceRuntimes.entrySet())
        {
            resourceDTOs.add(getResourceDTO(failure.getKey(), failure.getValue()));
        }
        return resourceDTOs.toArray(RESOURCE_DTO_ARRAY);
    }

    private FailedResourceDTO getResourceDTO(ServletRuntime failedResource, int failureCode)
    {
        ResourceDTOBuilder<FailedResourceDTO> dtoBuilder = new ResourceDTOBuilder<FailedResourceDTO>(DTOSuppliers.FAILED_RESOURCE);
        FailedResourceDTO resourceDTO = dtoBuilder.buildDTO(failedResource, 0);
        resourceDTO.failureReason = failureCode;
        return resourceDTO;
    }

    public FailedErrorPageDTO[] getErrorPageDTOs()
    {
        List<FailedErrorPageDTO> errorPageDTOs = new ArrayList<FailedErrorPageDTO>();
        for (Map.Entry<ErrorPageRuntime, Integer> failure : errorPageRuntimes.entrySet())
        {
            errorPageDTOs.add(getErrorPageDTO(failure.getKey(), failure.getValue()));
        }
        return errorPageDTOs.toArray(ERROR_PAGE_DTO_ARRAY);
    }

    private FailedErrorPageDTO getErrorPageDTO(ErrorPageRuntime failedErrorPage, int failureCode)
    {
        ErrorPageDTOBuilder<FailedErrorPageDTO> dtoBuilder = new ErrorPageDTOBuilder<FailedErrorPageDTO>(DTOSuppliers.FAILED_ERROR_PAGE);
        FailedErrorPageDTO errorPageDTO = dtoBuilder.buildDTO(failedErrorPage, 0);
        errorPageDTO.failureReason = failureCode;
        return errorPageDTO;
    }

    public FailedListenerDTO[] getListenerDTOs()
    {
        List<FailedListenerDTO> listenerDTOs = new ArrayList<FailedListenerDTO>();
        for (Map.Entry<ServiceReference<?>, Integer> failure : listenerRuntimes.entrySet())
        {
            listenerDTOs.add(getListenerDTO(failure.getKey(), failure.getValue()));
        }
        return listenerDTOs.toArray(LISTENER_DTO_ARRAY);
    }

    private FailedListenerDTO getListenerDTO(ServiceReference<?> failedListener, int failureCode)
    {
        ListenerDTOBuilder<FailedListenerDTO> dtoBuilder = new ListenerDTOBuilder<FailedListenerDTO>(DTOSuppliers.FAILED_LISTENER);
        FailedListenerDTO errorPageDTO = dtoBuilder.buildDTO(failedListener, 0);
        errorPageDTO.failureReason = failureCode;
        return errorPageDTO;
    }

    public FailedServletContextDTO[] getServletContextDTOs()
    {
        List<FailedServletContextDTO> contextDTOs = new ArrayList<FailedServletContextDTO>();
        for (Map.Entry<ServletContextHelperRuntime, Integer> failure : contextRuntimes.entrySet())
        {
            contextDTOs.add(getServletContextDTO(failure.getKey(), failure.getValue()));
        }
        return contextDTOs.toArray(CONTEXT_DTO_ARRAY);
    }

    private FailedServletContextDTO getServletContextDTO(ServletContextHelperRuntime failedContext, int failureCode)
    {
        ServletContextDTOBuilder dtoBuilder = new ServletContextDTOBuilder(new FailedServletContextDTO(), failedContext);
        FailedServletContextDTO servletContextDTO = (FailedServletContextDTO) dtoBuilder.build();
        servletContextDTO.failureReason = failureCode;
        return servletContextDTO;
    }
}

