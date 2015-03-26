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
package org.apache.felix.http.base.internal.handler;

import static org.osgi.service.http.runtime.dto.DTOConstants.FAILURE_REASON_SERVLET_CONTEXT_FAILURE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nonnull;
import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.felix.http.base.internal.runtime.FilterInfo;
import org.apache.felix.http.base.internal.runtime.ServletContextHelperInfo;
import org.apache.felix.http.base.internal.runtime.ServletInfo;
import org.apache.felix.http.base.internal.runtime.WhiteboardServiceInfo;
import org.apache.felix.http.base.internal.runtime.dto.ContextRuntime;
import org.apache.felix.http.base.internal.runtime.dto.FailureRuntime;
import org.apache.felix.http.base.internal.runtime.dto.InfoServletContextHelperRuntime;
import org.apache.felix.http.base.internal.runtime.dto.ServletContextHelperRuntime;
import org.apache.felix.http.base.internal.whiteboard.RegistrationFailureException;

/**
 * Registry for all services.
 *
 * The registry is organized per servlet context and is dispatching to one
 * of the {@link PerContextHandlerRegistry} registries.
 */
public final class HandlerRegistry
{
    private static FilterHandler[] EMPTY_FILTER_HANDLER = new FilterHandler[0];

    /** Current list of context registrations. */
    private volatile List<PerContextHandlerRegistry> registrations = Collections.emptyList();

    /**
     * Register default context registry for Http Service
     */
    public void init()
    {
        this.add(new PerContextHandlerRegistry());
    }

    /**
     * Shutdown
     */
    public void shutdown()
    {
        final List<PerContextHandlerRegistry> list;

        synchronized ( this )
        {
            list = new ArrayList<PerContextHandlerRegistry>(this.registrations);
            this.registrations = Collections.emptyList();

        }

        for(final PerContextHandlerRegistry r : list)
        {
            r.removeAll();
        }
    }

    /**
     * Add a context registration.
     * @param info The servlet context helper info
     */
    public void add(@Nonnull ServletContextHelperInfo info)
    {
        this.add(new PerContextHandlerRegistry(info));
    }

    /**
     * Remove a context registration.
     * @param info The servlet context helper info
     */
    public void remove(@Nonnull ServletContextHelperInfo info)
    {
        synchronized ( this )
        {
            final List<PerContextHandlerRegistry> updatedList = new ArrayList<PerContextHandlerRegistry>(this.registrations);
            final Iterator<PerContextHandlerRegistry> i = updatedList.iterator();
            while ( i.hasNext() )
            {
                final PerContextHandlerRegistry reg = i.next();
                if ( reg.getContextServiceId() == info.getServiceId() )
                {
                    i.remove();
                    this.registrations = updatedList;
                    break;
                }
            }
        }
    }


    /**
     * Add a new context registration.
     */
    private void add(@Nonnull PerContextHandlerRegistry registry)
    {
        synchronized ( this )
        {
            final List<PerContextHandlerRegistry> updatedList = new ArrayList<PerContextHandlerRegistry>(this.registrations);
            updatedList.add(registry);
            Collections.sort(updatedList);

            this.registrations = updatedList;
        }
    }

    public void addServlet(ServletHandler handler) throws ServletException
    {
        getRegistryChecked(null, null).addServlet(handler);
    }

    public void addServlet(ServletContextHelperInfo contextInfo, ServletHandler handler) throws ServletException
    {
        getRegistryChecked(contextInfo, handler.getServletInfo()).addServlet(handler);
    }

    public void removeServlet(Servlet servlet, boolean destroy)
    {
        try
        {
            getRegistry(null).removeServlet(servlet, destroy);
        }
        catch (RegistrationFailureException e)
        {
            // TODO
        }
    }

    public Servlet removeServlet(ServletInfo servlet)
    {
        try
        {
            return getRegistry(null).removeServlet(servlet, true);
        }
        catch (RegistrationFailureException e)
        {
            // TODO
            return null;
        }
    }

    public Servlet removeServlet(ServletContextHelperInfo contextInfo, ServletInfo servletInfo)
    {
        PerContextHandlerRegistry registry = getRegistry(contextInfo);
        if (registry == null)
        {
            return null;
        }

        try
        {
            return registry.removeServlet(servletInfo, true);
        }
        catch (RegistrationFailureException e)
        {
            // TODO
            return null;
        }
    }

    public void addFilter(FilterHandler handler) throws ServletException
    {
        getRegistryChecked(null, null).addFilter(handler);
    }

    public void addFilter(ServletContextHelperInfo contextInfo, FilterHandler handler) throws ServletException
    {
        getRegistryChecked(contextInfo, handler.getFilterInfo()).addFilter(handler);
    }

    public void removeFilter(Filter filter, boolean destroy)
    {
        try
        {
            getRegistryChecked(null, null).removeFilter(filter, destroy);
        }
        catch (RegistrationFailureException e)
        {
            // TODO
        }
    }

    public Filter removeFilter(ServletContextHelperInfo contextInfo, FilterInfo filterInfo)
    {
        return getRegistry(contextInfo).removeFilter(filterInfo, true);
    }

    private PerContextHandlerRegistry getRegistryChecked(ServletContextHelperInfo info, WhiteboardServiceInfo<?> serviceInfo)
        throws RegistrationFailureException
    {
        PerContextHandlerRegistry registry = getRegistry(info);
        if (registry == null)
        {
            throw new RegistrationFailureException(serviceInfo, FAILURE_REASON_SERVLET_CONTEXT_FAILURE);
        }
        return registry;
    }

    /**
     * Get the per context registry.
     * @param info The servlet context helper info or {@code null} for the Http Service context.
     * @return A per context registry or {@code null}
     */
    private PerContextHandlerRegistry getRegistry(final ServletContextHelperInfo info)
    {
        final long key = (info == null ? 0 : info.getServiceId());

        synchronized ( this )
        {
            for(final PerContextHandlerRegistry r : this.registrations)
            {
                if ( key == r.getContextServiceId())
                {
                    return r;
                }
            }
        }

        return null;
    }

    public ServletHandler getErrorsHandler(String requestURI, Long serviceId, int code, String exceptionType)
    {
        ErrorsMapping errorsMapping = getErrorsMapping(requestURI, serviceId);
        if (errorsMapping == null)
        {
            return null;
        }

        // TODO check exception hierarchy
        ServletHandler errorHandler = errorsMapping.get(exceptionType);
        if (errorHandler != null)
        {
            return errorHandler;
        }

        return errorsMapping.get(code);
    }

    private ErrorsMapping getErrorsMapping(final String requestURI, final Long serviceId)
    {
        final List<PerContextHandlerRegistry> regs = this.registrations;
        for(final PerContextHandlerRegistry r : regs)
        {
            if ( serviceId != null && serviceId == r.getContextServiceId() )
            {
                return r.getErrorsMapping();
            }
            else if ( serviceId == null && r.isMatching(requestURI) != null )
            {
                return r.getErrorsMapping();
            }
        }

        return null;
    }

    public FilterHandler[] getFilterHandlers(@Nonnull final ServletHandler servletHandler,
            final DispatcherType dispatcherType,
            @Nonnull final String requestURI)
    {
        final long key = servletHandler.getContextServiceId();
        final List<PerContextHandlerRegistry> regs = this.registrations;
        for(final PerContextHandlerRegistry r : regs)
        {
            if ( key == r.getContextServiceId() )
            {
                return r.getFilterHandlers(servletHandler, dispatcherType, requestURI);
            }
        }
        return EMPTY_FILTER_HANDLER;
    }

    /**
     * Get the servlet handler for a servlet by name
     * @param contextId The context id or {@code null}
     * @param name The servlet name
     * @return The servlet handler or {@code null}
     */
    public ServletHandler getServletHandlerByName(final Long contextId, @Nonnull final String name)
    {
        final long key = (contextId == null ? 0 : contextId);
        final List<PerContextHandlerRegistry> regs = this.registrations;
        for(final PerContextHandlerRegistry r : regs)
        {
            if ( key == r.getContextServiceId() )
            {
                return r.getServletHandlerByName(name);
            }
        }

        return null;
    }

    /**
     * Search the servlet handler for the request uri
     * @param requestURI The request uri
     * @return
     */
    public ServletHandler getServletHander(@Nonnull final String requestURI)
    {
        // search the first matching context registry
        final List<PerContextHandlerRegistry> regs = this.registrations;
        for(final PerContextHandlerRegistry r : regs)
        {
        	final String pathInContext = r.isMatching(requestURI);
        	if ( pathInContext != null )
        	{
                final ServletHandler handler = r.getServletHandler(pathInContext);
                if ( handler != null )
                {
                    return handler;
                }
            }
        }
        return null;
    }

    public synchronized List<ContextRuntime> getRuntime(FailureRuntime.Builder failureRuntimeBuilder)
    {
        List<ContextRuntime> handlerRuntimes = new ArrayList<ContextRuntime>();
        for (PerContextHandlerRegistry contextRegistry : this.registrations)
        {
            handlerRuntimes.add(contextRegistry.getRuntime(failureRuntimeBuilder));
        }
        return handlerRuntimes;
    }

    public ServletContextHelperRuntime getHttpServiceContextRuntime()
    {
        ServletContextHelperInfo info = new ServletContextHelperInfo(Integer.MAX_VALUE, 0, "Http service context", "/", null);
        return new InfoServletContextHelperRuntime(info);
    }
}
