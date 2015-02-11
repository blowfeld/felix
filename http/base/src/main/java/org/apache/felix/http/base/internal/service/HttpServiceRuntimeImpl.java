/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.http.base.internal.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.felix.http.base.internal.runtime.RegistryRuntime;
import org.apache.felix.http.base.internal.runtime.dto.RuntimeDTOBuilder;
import org.apache.felix.http.base.internal.whiteboard.WhiteboardHttpService;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.runtime.HttpServiceRuntime;
import org.osgi.service.http.runtime.dto.RequestInfoDTO;
import org.osgi.service.http.runtime.dto.RuntimeDTO;
import org.osgi.util.tracker.ServiceTracker;

public final class HttpServiceRuntimeImpl implements HttpServiceRuntime
{
    private final Collection<ServiceTracker<?,?>> listenerTrackers;
    private final WhiteboardHttpService whiteboardHttpService;

    public HttpServiceRuntimeImpl(WhiteboardHttpService whiteboardHttpService)
    {
        this.whiteboardHttpService = whiteboardHttpService;
        // TODO
        this.listenerTrackers = Collections.emptyList();
    }

    public RuntimeDTO getRuntimeDTO()
    {
        RegistryRuntime runtime = whiteboardHttpService.getRuntime();
        List<ServiceReference<?>> listenerRefs = readListenerRefs(listenerTrackers);
        RuntimeDTOBuilder runtimeDTOBuilder = new RuntimeDTOBuilder(runtime, listenerRefs);
        return runtimeDTOBuilder.build();
    }

    private List<ServiceReference<?>> readListenerRefs(Collection<ServiceTracker<?,?>> listenerTrackers)
    {
        List<ServiceReference<?>> listeners = new ArrayList<ServiceReference<?>>();
        for (ServiceTracker<?,?> tracker : listenerTrackers)
        {
            ServiceReference<?>[] serviceReferences = tracker.getServiceReferences();
            if (serviceReferences != null)
            {
                listeners.addAll(Arrays.<ServiceReference<?>>asList(serviceReferences));
            }
        }
        return listeners;
    }

    @Override
    public RequestInfoDTO calculateRequestInfoDTO(String path)
    {
        // TODO Auto-generated method stub
        return null;
    }
}
