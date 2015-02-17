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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.http.base.internal.handler.FilterHandler;
import org.apache.felix.http.base.internal.handler.ServletHandler;
import org.apache.felix.http.base.internal.runtime.ContextRuntime;
import org.apache.felix.http.base.internal.runtime.ContextRuntime.ErrorPage;
import org.apache.felix.http.base.internal.runtime.RegistryRuntime;
import org.apache.felix.http.base.internal.whiteboard.ContextHandler;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.runtime.dto.ErrorPageDTO;
import org.osgi.service.http.runtime.dto.FilterDTO;
import org.osgi.service.http.runtime.dto.ListenerDTO;
import org.osgi.service.http.runtime.dto.ResourceDTO;
import org.osgi.service.http.runtime.dto.RuntimeDTO;
import org.osgi.service.http.runtime.dto.ServletContextDTO;
import org.osgi.service.http.runtime.dto.ServletDTO;

public final class RuntimeDTOBuilder
{
    private static final ServletContextDTO[] CONTEXT_DTO_ARRAY = new ServletContextDTO[0];

    private static final ServletDTOBuilder SERVLET_DTO_BUILDER = new ServletDTOBuilder();
    private static final ResourceDTOBuilder RESOURCE_DTO_BUILDER = new ResourceDTOBuilder();
    private static final FilterDTOBuilder FILTER_DTO_BUILDER = new FilterDTOBuilder();
    private static final ErrorPageDTOBuilder ERROR_PAGE_DTO_BUILDER = new ErrorPageDTOBuilder();
    private static final ListenerDTOBuilder LISTENER_DTO_BUILDER = new ListenerDTOBuilder();

    private final RegistryRuntime registry;
    private final Collection<ServiceReference<?>> listenerRefs;
    private final Map<String, Object> serviceProperties;

    public RuntimeDTOBuilder(RegistryRuntime registry,
            Collection<ServiceReference<?>> listenerRefs,
            Map<String, Object> serviceProperties)
    {
        this.registry = registry;
        this.listenerRefs = listenerRefs;
        this.serviceProperties = serviceProperties;
    }

    public RuntimeDTO build()
    {
        RuntimeDTO runtimeDTO = new RuntimeDTO();
        runtimeDTO.attributes = createAttributes();
        //TODO <**
        runtimeDTO.failedErrorPageDTOs = null;
        runtimeDTO.failedFilterDTOs = null;
        runtimeDTO.failedListenerDTOs = null;
        runtimeDTO.failedResourceDTOs = null;
        runtimeDTO.failedServletContextDTOs = null;
        runtimeDTO.failedServletDTOs = null;
        //**>
        runtimeDTO.servletContextDTOs = createContextDTOs();
        return runtimeDTO;
    }

    private Map<String, String> createAttributes()
    {
        Map<String, String> attributes = new HashMap<String, String>();
        for (Map.Entry<String, Object> entry : this.serviceProperties.entrySet())
        {
            attributes.put(entry.getKey(), entry.getValue().toString());
        }
        return attributes;
    }

    private ServletContextDTO[] createContextDTOs()
    {
        List<ServletContextDTO> contextDTOs = new ArrayList<ServletContextDTO>();
        for (ContextHandler context : registry.getContexts())
        {
            contextDTOs.add(createContextDTO(context, registry.getContextRuntime(context)));
        }
        return contextDTOs.toArray(CONTEXT_DTO_ARRAY);
    }

    private ServletContextDTO createContextDTO(ContextHandler context, ContextRuntime contextRuntime)
    {
        Collection<ServletHandler> servletHandlers = contextRuntime.getServletHandlers();
        //TODO missing functionality
        Collection<ServletHandler> resourceHandlers = Collections.emptyList();
        Collection<FilterHandler> filterHandlers = contextRuntime.getFilterHandlers();
        Collection<ErrorPage> errorPages = contextRuntime.getErrorPages();
        long servletContextId = contextRuntime.getServiceId();

        Collection<ServletDTO> servletDTOs = SERVLET_DTO_BUILDER.build(servletHandlers, servletContextId);
        Collection<ResourceDTO> resourcesDTOs = RESOURCE_DTO_BUILDER.build(resourceHandlers, servletContextId);
        Collection<FilterDTO> filtersDTOs = FILTER_DTO_BUILDER.build(filterHandlers, servletContextId);
        Collection<ErrorPageDTO> errorsDTOs = ERROR_PAGE_DTO_BUILDER.build(errorPages, servletContextId);
        Collection<ListenerDTO> listenersDTOs = LISTENER_DTO_BUILDER.build(listenerRefs, servletContextId);

        return new ServletContextDTOBuilder(context,
                    servletDTOs,
                    resourcesDTOs,
                    filtersDTOs,
                    errorsDTOs,
                    listenersDTOs)
                .build();
    }
}
