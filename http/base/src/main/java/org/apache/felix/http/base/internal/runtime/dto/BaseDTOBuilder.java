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

import static java.util.Arrays.copyOf;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.osgi.dto.DTO;


abstract class BaseDTOBuilder<T, U extends DTO>
{
    private DTOFactory<U> dtoFactory;

    BaseDTOBuilder(DTOFactory<U> dtoFactory)
    {
        this.dtoFactory = dtoFactory;
    }

    Collection<U> build(Collection<? extends T> whiteboardServices, long servletContextId)
    {
        List<U> dtoList = new ArrayList<U>();
        for (T whiteboardService : whiteboardServices)
        {
            dtoList.add(buildDTO(whiteboardService, servletContextId));
        }
        return dtoList;
    }

    abstract U buildDTO(T whiteboardService, long servletContextId);

    DTOFactory<U> getDTOFactory()
    {
        return dtoFactory;
    }

    <V> V[] copyWithDefault(V[] array, V[] defaultArray)
    {
        return array == null ? defaultArray : copyOf(array, array.length);
    }
}