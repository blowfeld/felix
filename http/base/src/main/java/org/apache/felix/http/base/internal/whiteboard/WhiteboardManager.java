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
package org.apache.felix.http.base.internal.whiteboard;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;

import org.apache.felix.http.base.internal.context.ExtServletContext;
import org.apache.felix.http.base.internal.handler.HttpSessionWrapper;
import org.apache.felix.http.base.internal.service.HttpServiceFactory;
import org.apache.felix.http.base.internal.service.HttpServiceRuntimeImpl;
import org.apache.felix.http.base.internal.whiteboard.tracker.FilterTracker;
import org.apache.felix.http.base.internal.whiteboard.tracker.HttpSessionAttributeListenerTracker;
import org.apache.felix.http.base.internal.whiteboard.tracker.HttpSessionListenerTracker;
import org.apache.felix.http.base.internal.whiteboard.tracker.ResourceTracker;
import org.apache.felix.http.base.internal.whiteboard.tracker.ServletContextAttributeListenerTracker;
import org.apache.felix.http.base.internal.whiteboard.tracker.ServletContextHelperTracker;
import org.apache.felix.http.base.internal.whiteboard.tracker.ServletContextListenerTracker;
import org.apache.felix.http.base.internal.whiteboard.tracker.ServletRequestAttributeListenerTracker;
import org.apache.felix.http.base.internal.whiteboard.tracker.ServletRequestListenerTracker;
import org.apache.felix.http.base.internal.whiteboard.tracker.ServletTracker;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.runtime.HttpServiceRuntime;
import org.osgi.service.http.runtime.HttpServiceRuntimeConstants;
import org.osgi.util.tracker.ServiceTracker;

public final class WhiteboardManager
{
    private final BundleContext bundleContext;

    private final List<ServiceTracker<?, ?>> trackers = new ArrayList<ServiceTracker<?, ?>>();

    private final HttpServiceFactory httpServiceFactory;

    private final HttpServiceRuntimeImpl serviceRuntime;

    private volatile ServletContextHelperManager contextManager;

    private volatile ServiceRegistration<HttpServiceRuntime> runtimeServiceReg;

    /**
     * Create a new whiteboard http service
     * @param bundleContext
     * @param contextManager
     * @param httpServiceFactory
     * @param serviceRuntime
     */
    public WhiteboardManager(final BundleContext bundleContext,
            ServletContextHelperManager contextManager,
            HttpServiceFactory httpServiceFactory,
            HttpServiceRuntimeImpl serviceRuntime)
    {
        this.bundleContext = bundleContext;
        this.httpServiceFactory = httpServiceFactory;
        this.contextManager = contextManager;
        this.serviceRuntime = serviceRuntime;
    }

    public void start(final ServletContext context)
    {
        // TODO set Endpoint
        this.serviceRuntime.setAttribute(HttpServiceRuntimeConstants.HTTP_SERVICE_ID_ATTRIBUTE,
                this.httpServiceFactory.getHttpServiceServiceId());
        this.runtimeServiceReg = this.bundleContext.registerService(HttpServiceRuntime.class,
                serviceRuntime,
                this.serviceRuntime.getAttributes());

        contextManager.start(context, runtimeServiceReg.getReference());

        addTracker(new FilterTracker(bundleContext, contextManager));
        addTracker(new ServletTracker(bundleContext, this.contextManager));
        addTracker(new ResourceTracker(bundleContext, this.contextManager));

        addTracker(new HttpSessionListenerTracker(bundleContext, this.contextManager));
        addTracker(new HttpSessionAttributeListenerTracker(bundleContext, this.contextManager));

        addTracker(new ServletContextHelperTracker(bundleContext, this.contextManager));
        addTracker(new ServletContextListenerTracker(bundleContext, this.contextManager));
        addTracker(new ServletContextAttributeListenerTracker(bundleContext, this.contextManager));

        addTracker(new ServletRequestListenerTracker(bundleContext, this.contextManager));
        addTracker(new ServletRequestAttributeListenerTracker(bundleContext, this.contextManager));
    }

    public void stop()
    {
        for(final ServiceTracker<?, ?> t : this.trackers)
        {
            t.close();
        }
        this.trackers.clear();

        if ( this.contextManager != null )
        {
            this.contextManager.close();
            this.contextManager = null;
        }

        if ( this.runtimeServiceReg != null )
        {
            this.runtimeServiceReg.unregister();
            this.runtimeServiceReg = null;
        }
    }

    private void addTracker(ServiceTracker<?, ?> tracker)
    {
        this.trackers.add(tracker);
        tracker.open();
    }

    public void setProperties(final Hashtable<String, Object> props)
    {
        // runtime service gets the same props for now
        this.serviceRuntime.setAllAttributes(props);

        if (this.runtimeServiceReg != null)
        {
            this.serviceRuntime.setAttribute(HttpServiceRuntimeConstants.HTTP_SERVICE_ID_ATTRIBUTE,
                    this.httpServiceFactory.getHttpServiceServiceId());
            this.runtimeServiceReg.setProperties(this.serviceRuntime.getAttributes());
        }
    }

    public void sessionDestroyed(@Nonnull final HttpSession session, final Set<Long> contextIds)
    {
        for(final Long contextId : contextIds)
        {
            final ContextHandler handler = this.contextManager.getContextHandler(contextId);
            if ( handler != null )
            {
                final ExtServletContext context = handler.getServletContext(this.bundleContext.getBundle());
                new HttpSessionWrapper(contextId, session, context, true).invalidate();
                handler.ungetServletContext(this.bundleContext.getBundle());
            }
        }
    }
}
