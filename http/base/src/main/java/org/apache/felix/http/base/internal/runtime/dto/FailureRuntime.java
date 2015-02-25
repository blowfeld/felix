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

    private final List<Failure<ServletContextHelperRuntime>> contextRuntimes;
    private final List<Failure<ServletRuntime>> servletRuntimes;
    private final List<Failure<FilterRuntime>> filterRuntimes;
    private final List<Failure<ServletRuntime>> resourceRuntimes;
    private final List<Failure<ErrorPageRuntime>> errorPageRuntimes;
    private final List<Failure<ServiceReference<?>>> listenerRuntimes;

    public FailureRuntime(List<Failure<ServletContextHelperRuntime>> contextRuntimes,
            List<Failure<ServiceReference<?>>> listenerRuntimes,
            List<Failure<ServletRuntime>> servletRuntimes,
            List<Failure<FilterRuntime>> filterRuntimes,
            List<Failure<ServletRuntime>> resourceRuntimes,
            List<Failure<ErrorPageRuntime>> errorPageRuntimes)
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
        return new FailureRuntime(Collections.<Failure<ServletContextHelperRuntime>>emptyList(),
                Collections.<Failure<ServiceReference<?>>>emptyList(),
                Collections.<Failure<ServletRuntime>>emptyList(),
                Collections.<Failure<FilterRuntime>>emptyList(),
                Collections.<Failure<ServletRuntime>>emptyList(),
                Collections.<Failure<ErrorPageRuntime>>emptyList());
    }

    public static FailureRuntime forServiceInfos(Map<AbstractInfo<?>, Integer> failureInfos)
    {
        List<Failure<ServletContextHelperRuntime>> contextRuntimes = new ArrayList<FailureRuntime.Failure<ServletContextHelperRuntime>>();
        List<Failure<ServletRuntime>> servletRuntimes = new ArrayList<Failure<ServletRuntime>>();
        List<Failure<FilterRuntime>> filterRuntimes = new ArrayList<Failure<FilterRuntime>>();
        List<Failure<ServletRuntime>> resourceRuntimes = new ArrayList<Failure<ServletRuntime>>();
        List<Failure<ErrorPageRuntime>> errorPageRuntimes = new ArrayList<Failure<ErrorPageRuntime>>();
        List<Failure<ServiceReference<?>>> listenerRuntimes = new ArrayList<Failure<ServiceReference<?>>>();

        for (AbstractInfo<?> info : failureInfos.keySet())
        {
            if (info instanceof ServletContextHelperInfo)
            {
                ServletContextHelperRuntime servletRuntime = new FailureServletContextHelperRuntime((ServletContextHelperInfo) info);
                contextRuntimes.add(new Failure<ServletContextHelperRuntime>(servletRuntime, failureInfos.get(info)));
            }
            else if (info instanceof ServletInfo)
            {
                ServletRuntime servletRuntime = new FailureServletRuntime((ServletInfo) info);
                servletRuntimes.add(new Failure<ServletRuntime>(servletRuntime, failureInfos.get(info)));
            }
            else if (info instanceof FilterInfo)
            {
                FilterRuntime filterRuntime = new FailureFilterRuntime((FilterInfo) info);
                filterRuntimes.add(new Failure<FilterRuntime>(filterRuntime, failureInfos.get(info)));
            }
            else if (info instanceof ResourceInfo)
            {
                ServletRuntime servletRuntime = new FailureServletRuntime(new ServletInfo((ResourceInfo) info));
                resourceRuntimes.add(new Failure<ServletRuntime>(servletRuntime, failureInfos.get(info)));
            }
            else if (info instanceof ListenerInfo)
            {
                ServiceReference<?> serviceReference = ((ListenerInfo<?>) info).getServiceReference();
                listenerRuntimes.add(new Failure<ServiceReference<?>>(serviceReference, failureInfos.get(info)));
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
        for (Failure<ServletRuntime> failure : servletRuntimes)
        {
            servletDTOs.add(getServletDTO(failure.service, failure.failureCode));
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
        for (Failure<FilterRuntime> failure : filterRuntimes)
        {
            filterDTOs.add(getFilterDTO(failure.service, failure.failureCode));
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
        for (Failure<ServletRuntime> failure : resourceRuntimes)
        {
            resourceDTOs.add(getResourceDTO(failure.service, failure.failureCode));
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
        for (Failure<ErrorPageRuntime> failure : errorPageRuntimes)
        {
            errorPageDTOs.add(getErrorPageDTO(failure.service, failure.failureCode));
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
        for (Failure<ServiceReference<?>> failure : listenerRuntimes)
        {
            listenerDTOs.add(getListenerDTO(failure.service, failure.failureCode));
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
        for (Failure<ServletContextHelperRuntime> failure : contextRuntimes)
        {
            contextDTOs.add(getServletContextDTO(failure.service, failure.failureCode));
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

    private static class Failure<T>
    {
        final T service;
        final int failureCode;

        Failure(T service, int failureCode)
        {
            this.service = service;
            this.failureCode = failureCode;
        }
    }
}

