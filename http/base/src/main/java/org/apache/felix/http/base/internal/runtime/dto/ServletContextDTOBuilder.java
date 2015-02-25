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

import static java.util.Collections.list;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;

import org.apache.felix.http.base.internal.runtime.ServletContextHelperInfo;
import org.osgi.dto.DTO;
import org.osgi.service.http.runtime.dto.ErrorPageDTO;
import org.osgi.service.http.runtime.dto.FilterDTO;
import org.osgi.service.http.runtime.dto.ListenerDTO;
import org.osgi.service.http.runtime.dto.ResourceDTO;
import org.osgi.service.http.runtime.dto.ServletContextDTO;
import org.osgi.service.http.runtime.dto.ServletDTO;

final class ServletContextDTOBuilder
{
    private static final ServletDTO[] SERVLET_DTO_ARRAY = new ServletDTO[0];
    private static final ResourceDTO[] RESOURCE_DTO_ARRAY = new ResourceDTO[0];
    private static final FilterDTO[] FILTER_DTO_ARRAY = new FilterDTO[0];
    private static final ErrorPageDTO[] ERROR_PAGE_DTO_ARRAY = new ErrorPageDTO[0];
    private static final ListenerDTO[] LISTENER_DTO_ARRAY = new ListenerDTO[0];

    private final ServletContextDTO contextDTO;
    private final ServletContextHelperRuntime contextRuntime;
    private final ServletDTO[] servletDTOs;
    private final ResourceDTO[] resourceDTOs;
    private final FilterDTO[] filterDTOs;
    private final ErrorPageDTO[] errorPageDTOs;
    private final ListenerDTO[] listenerDTOs;

    ServletContextDTOBuilder(ServletContextDTO contextDTO,
            ServletContextHelperRuntime contextRuntime,
            Collection<ServletDTO> servletDTOs,
            Collection<ResourceDTO> resourceDTOs,
            Collection<FilterDTO> filterDTOs,
            Collection<ErrorPageDTO> errorPageDTOs,
            Collection<ListenerDTO> listenerDTOs)
    {
        this.contextDTO = contextDTO;
        this.contextRuntime = contextRuntime;
        this.servletDTOs = servletDTOs != null ?
                servletDTOs.toArray(SERVLET_DTO_ARRAY) : SERVLET_DTO_ARRAY;
        this.resourceDTOs = resourceDTOs != null ?
                resourceDTOs.toArray(RESOURCE_DTO_ARRAY) : RESOURCE_DTO_ARRAY;
        this.filterDTOs = filterDTOs != null ?
                filterDTOs.toArray(FILTER_DTO_ARRAY) : FILTER_DTO_ARRAY;
        this.errorPageDTOs = errorPageDTOs != null ?
                errorPageDTOs.toArray(ERROR_PAGE_DTO_ARRAY) : ERROR_PAGE_DTO_ARRAY;
        this.listenerDTOs = listenerDTOs != null ?
                listenerDTOs.toArray(LISTENER_DTO_ARRAY) : LISTENER_DTO_ARRAY;
    }

    ServletContextDTOBuilder(ServletContextHelperRuntime contextRuntime,
            Collection<ServletDTO> servletDTOs,
            Collection<ResourceDTO> resourceDTOs,
            Collection<FilterDTO> filterDTOs,
            Collection<ErrorPageDTO> errorPageDTOs,
            Collection<ListenerDTO> listenerDTOs)
    {
        this(new ServletContextDTO(), contextRuntime, servletDTOs, resourceDTOs, filterDTOs, errorPageDTOs, listenerDTOs);
    }

    ServletContextDTOBuilder(ServletContextDTO contextDTO, ServletContextHelperRuntime contextRuntime)
    {
        this(contextDTO, contextRuntime, null, null, null, null, null);
    }

    ServletContextDTO build()
    {
        ServletContext context  = contextRuntime.getSharedContext();
        ServletContextHelperInfo contextInfo = contextRuntime.getContextInfo();
        long contextId = contextInfo.getServiceId();

        contextDTO.attributes = getAttributes(context);
        contextDTO.contextName = contextInfo.getName();
        contextDTO.contextPath = context == null ? contextInfo.getPath() : context.getContextPath();
        contextDTO.errorPageDTOs = errorPageDTOs;
        contextDTO.filterDTOs = filterDTOs;
        contextDTO.initParams = contextInfo.getInitParameters();
        contextDTO.listenerDTOs = listenerDTOs;
        contextDTO.name = contextId >= 0 ? contextInfo.getName() : null;
        contextDTO.resourceDTOs = resourceDTOs;
        contextDTO.servletDTOs = servletDTOs;
        contextDTO.serviceId = contextId;
        return contextDTO;
    }

    private Map<String, Object> getAttributes(ServletContext context)
    {
        if (context == null)
        {
            return Collections.emptyMap();
        }

        Map<String, Object> attributes = new HashMap<String, Object>();
        for (String name : list(context.getAttributeNames()))
        {
            Object attribute = context.getAttribute(name);
            if (isSupportedType(attribute))
            {
                attributes.put(name, attribute);
            }
        }
        return attributes;
    }

    private boolean isSupportedType(Object attribute)
    {
        Class<?> attributeClass = attribute.getClass();
        Class<?> type = !attributeClass.isArray() ?
                attributeClass : attributeClass.getComponentType();

        return Number.class.isAssignableFrom(type) ||
                Boolean.class.isAssignableFrom(type) ||
                String.class.isAssignableFrom(type) ||
                DTO.class.isAssignableFrom(type) ||
                boolean.class.isAssignableFrom(type) ||
                int.class.isAssignableFrom(type) ||
                double.class.isAssignableFrom(type) ||
                float.class.isAssignableFrom(type) ||
                long.class.isAssignableFrom(type) ||
                short.class.isAssignableFrom(type) ||
                byte.class.isAssignableFrom(type) ||
                char.class.isAssignableFrom(type);
    }
}
