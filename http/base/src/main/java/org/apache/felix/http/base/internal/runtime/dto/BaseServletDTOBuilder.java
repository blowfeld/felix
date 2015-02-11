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

import java.util.Collections;
import java.util.Map;

import javax.servlet.Servlet;

import org.apache.felix.http.base.internal.handler.ServletHandler;
import org.apache.felix.http.base.internal.runtime.ServletInfo;
import org.osgi.service.http.runtime.dto.BaseServletDTO;

abstract class BaseServletDTOBuilder<T, U extends BaseServletDTO> extends BaseDTOBuilder<T, U>
{
    private static final Map<String, String> EMPTY_INIT_PARAMS = Collections.<String, String>emptyMap();

    final U setBaseFields(U dto, ServletHandler servletHandler, long servletContextId)
    {
        ServletInfo info = servletHandler.getServletInfo();
        Servlet servlet = servletHandler.getServlet();

        dto.asyncSupported = info.isAsyncSupported();
        Map<String, String> initParameters = info.getInitParameters();
		dto.initParams = initParameters != null ? initParameters : EMPTY_INIT_PARAMS;
        dto.name = info.getName();
        dto.serviceId = servletHandler.getServletInfo().getServiceId();
        dto.servletContextId = servletContextId;
        dto.servletInfo = servlet.getServletInfo();
        return dto;
    }
}
