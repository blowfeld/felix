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

import javax.servlet.DispatcherType;

import org.apache.felix.http.base.internal.handler.FilterHandler;
import org.apache.felix.http.base.internal.runtime.FilterInfo;
import org.osgi.service.http.runtime.dto.FilterDTO;

final class FilterDTOBuilder extends BaseDTOBuilder<FilterHandler, FilterDTO>
{
    private static final Map<String, String> EMPTY_INIT_PARAMS = Collections.<String, String>emptyMap();
    private static final String[] STRING_ARRAY = new String[0];

    @Override
    FilterDTO buildDTO(FilterHandler filterHandler, long servletContextId)
    {
        FilterInfo info = filterHandler.getFilterInfo();

        FilterDTO filterDTO = new FilterDTO();
        filterDTO.asyncSupported = info.isAsyncSupported();
        filterDTO.dispatcher = getNames(info.getDispatcher());
        Map<String, String> initParameters = info.getInitParameters();
		filterDTO.initParams = initParameters != null ? initParameters : EMPTY_INIT_PARAMS;
        filterDTO.name = info.getName();
        String[] patterns = info.getPatterns();
		filterDTO.patterns = patterns != null ? patterns : STRING_ARRAY;
        String[] regexs = info.getRegexs();
		filterDTO.regexs = regexs != null ? regexs: STRING_ARRAY;
        filterDTO.serviceId = filterHandler.getFilterInfo().getServiceId();
        filterDTO.servletContextId = servletContextId;
        filterDTO.servletNames = info.getServletNames();

        return filterDTO;
    }

    private String[] getNames(DispatcherType[] dispatcher)
    {
        if (dispatcher == null)
        {
            return STRING_ARRAY;
        }

        List<String> names = new ArrayList<String>();
        for (DispatcherType dispatcherType : dispatcher)
        {
            names.add(dispatcherType.name());
        }
        return names.toArray(STRING_ARRAY);
    }
}
