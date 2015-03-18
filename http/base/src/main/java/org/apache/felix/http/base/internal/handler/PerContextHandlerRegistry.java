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

import static org.osgi.service.http.runtime.dto.DTOConstants.FAILURE_REASON_EXCEPTION_ON_INIT;
import static org.osgi.service.http.runtime.dto.DTOConstants.FAILURE_REASON_SERVICE_ALREAY_USED;
import static org.osgi.service.http.runtime.dto.DTOConstants.FAILURE_REASON_SHADOWED_BY_OTHER_SERVICE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.felix.http.base.internal.handler.WhiteboardServiceQueue.Update;
import org.apache.felix.http.base.internal.runtime.FilterInfo;
import org.apache.felix.http.base.internal.runtime.ServletContextHelperInfo;
import org.apache.felix.http.base.internal.runtime.ServletInfo;
import org.apache.felix.http.base.internal.runtime.dto.ContextRuntime;
import org.apache.felix.http.base.internal.runtime.dto.ErrorPageRuntime;
import org.apache.felix.http.base.internal.runtime.dto.FailureRuntime;
import org.apache.felix.http.base.internal.runtime.dto.FilterRuntime;
import org.apache.felix.http.base.internal.runtime.dto.ServletRuntime;
import org.apache.felix.http.base.internal.util.PatternUtil.PatternComparator;
import org.apache.felix.http.base.internal.whiteboard.RegistrationFailureException;

public final class PerContextHandlerRegistry implements Comparable<PerContextHandlerRegistry>
{
    private final Map<Filter, FilterHandler> filterMap = new HashMap<Filter, FilterHandler>();

    private volatile HandlerMapping<ServletHandler> servletMapping = new HandlerMapping<ServletHandler>();
    private volatile HandlerMapping<FilterHandler> filterMapping = new HandlerMapping<FilterHandler>();
    private final ErrorsMapping errorsMapping = new ErrorsMapping();

    private final WhiteboardServiceQueue<Pattern, ServletHandler> servletQueue = new WhiteboardServiceQueue<Pattern, ServletHandler>(PatternComparator.INSTANCE);
    private final SortedSet<ServletHandler> allServletHandlers = new TreeSet<ServletHandler>();

    private final long serviceId;

    private final int ranking;

    private final String path;

    private final String prefix;

    public PerContextHandlerRegistry() {
        this.serviceId = 0;
        this.ranking = Integer.MAX_VALUE;
        this.path = "/";
        this.prefix = null;
    }

    public PerContextHandlerRegistry(final ServletContextHelperInfo info)
    {
        this.serviceId = info.getServiceId();
        this.ranking = info.getRanking();
        this.path = info.getPath();
        if ( this.path.equals("/") )
        {
        	prefix = null;
        }
        else
        {
        	prefix = this.path + "/";
        }
    }

    public synchronized void addFilter(FilterHandler handler) throws ServletException
    {
    	if(this.filterMapping.contains(handler))
    	{
            throw new RegistrationFailureException(handler.getFilterInfo(), FAILURE_REASON_SERVICE_ALREAY_USED, "Filter instance " + handler.getName() + " already registered");
    	}

        handler.init();
        this.filterMapping = this.filterMapping.add(handler);
        this.filterMap.put(handler.getFilter(), handler);
    }

    @Override
    public int compareTo(final PerContextHandlerRegistry other)
    {
        final int result = Integer.compare(other.path.length(), this.path.length());
        if ( result == 0 ) {
            if (this.ranking == other.ranking)
            {
                // Service id's can be negative. Negative id's follow the reverse natural ordering of integers.
                int reverseOrder = ( this.serviceId >= 0 && other.serviceId >= 0 ) ? 1 : -1;
                return reverseOrder * Long.compare(this.serviceId, other.serviceId);
            }

            return Integer.compare(other.ranking, this.ranking);
        }
        return result;
    }

    /**
     * Add a new servlet.
     */
    public synchronized void addServlet(final ServletHandler handler) throws ServletException
    {
        this.allServletHandlers.add(handler);

        Pattern[] patterns = handler.getPatterns();
        if (patterns != null && patterns.length > 0)
        {
            addServlet(handler, patterns);
        }
        else
        {
            addErrorPage(handler);
        }
    }

    private void addServlet(ServletHandler handler, Pattern[] patterns) throws ServletException
    {
        Update<Pattern, ServletHandler> update = servletQueue.add(handler.getPatterns(), handler);
        updateServletMapping(update, true);
    }

    private void addErrorPage(ServletHandler handler) throws ServletException
    {
        String[] errorPages = handler.getServletInfo().getErrorPage();
        for (String errorPage : errorPages)
        {
            handler.init();
            this.errorsMapping.addErrorServlet(errorPage, handler);
        }
    }

	public ErrorsMapping getErrorsMapping()
    {
        return this.errorsMapping;
    }

    public FilterHandler[] getFilterHandlers(ServletHandler servletHandler, DispatcherType dispatcherType, String requestURI)
    {
        // See Servlet 3.0 specification, section 6.2.4...
        List<FilterHandler> result = new ArrayList<FilterHandler>();
        result.addAll(this.filterMapping.getAllMatches(requestURI));

        // TODO this is not the most efficient/fastest way of doing this...
        Iterator<FilterHandler> iter = result.iterator();
        while (iter.hasNext())
        {
            if (!referencesDispatcherType(iter.next(), dispatcherType))
            {
                iter.remove();
            }
        }

        String servletName = (servletHandler != null) ? servletHandler.getName() : null;
        // TODO this is not the most efficient/fastest way of doing this...
        for (FilterHandler filterHandler : this.filterMapping.values())
        {
            if (referencesServletByName(filterHandler, servletName))
            {
                result.add(filterHandler);
            }
        }

        // TODO - we should already check for the context when building up the result set
        final Iterator<FilterHandler> i = result.iterator();
        while ( i.hasNext() )
        {
            final FilterHandler handler = i.next();
            if ( handler.getContextServiceId() != servletHandler.getContextServiceId() )
            {
                i.remove();
            }
        }
        return result.toArray(new FilterHandler[result.size()]);
    }

    public ServletHandler getServletHandlerByName(String name)
    {
        return this.servletMapping.getByName(name);
    }

    public ServletHandler getServletHander(String requestURI)
    {
        return this.servletMapping.getBestMatch(requestURI);
    }

    public synchronized void removeAll()
    {
        Collection<ServletHandler> servletHandlers = servletMapping.values();
        Collection<FilterHandler> filterHandlers = filterMapping.values();

        this.servletMapping = new HandlerMapping<ServletHandler>();
        this.filterMapping = new HandlerMapping<FilterHandler>();

        for (ServletHandler handler : servletHandlers)
        {
            handler.destroy();
        }

        for (FilterHandler handler : filterHandlers)
        {
            handler.destroy();
        }

        this.errorsMapping.clear();
        this.allServletHandlers.clear();
        this.filterMap.clear();
        this.servletQueue.clear();
    }

    public synchronized void removeFilter(Filter filter, final boolean destroy)
    {
        FilterHandler handler = this.filterMap.remove(filter);
        if (handler != null)
        {
            this.filterMapping = this.filterMapping.remove(handler);
            if (destroy)
            {
                handler.destroy();
            }
        }
    }

    public synchronized Filter removeFilter(final FilterInfo filterInfo, final boolean destroy)
    {
        FilterHandler handler = getFilterHandler(filterInfo);

        if (handler == null)
        {
            return null;
        }

        this.filterMapping = this.filterMapping.remove(handler);

        if (destroy)
        {
            handler.destroy();
        }
        return handler.getFilter();
    }

    private FilterHandler getFilterHandler(final FilterInfo filterInfo)
    {
        for(final FilterHandler handler : this.filterMap.values())
        {
            if ( handler.getFilterInfo().compareTo(filterInfo) == 0)
            {
                return handler;
            }
        }
        return null;
    }

    public synchronized Servlet removeServlet(ServletInfo servletInfo, final boolean destroy) throws RegistrationFailureException
    {
        ServletHandler handler = getServletHandler(servletInfo);
        if (handler == null)
        {
            return null;
        }

        Servlet servlet = handler.getServlet();

        Pattern[] patterns = handler.getPatterns();
        if (patterns != null && patterns.length > 0)
        {
            Update<Pattern, ServletHandler> update = servletQueue.remove(patterns, handler);
            updateServletMapping(update, destroy);
        }

        if (destroy)
        {
            handler.destroy();
        }

        return servlet;
    }

    private void updateServletMapping(Update<Pattern, ServletHandler> update, boolean destroy) throws RegistrationFailureException
    {
        for (ServletHandler servletHandler : update.getInit())
        {
            try
            {
                servletHandler.init();
            }
            catch (ServletException e)
            {
                // TODO we should collect this cases and not throw an exception immediately
                throw new RegistrationFailureException(servletHandler.getServletInfo(), FAILURE_REASON_EXCEPTION_ON_INIT);
            }
        }

        this.servletMapping = this.servletMapping.update(update.getActivated(), update.getDeactivated());

        if (destroy)
        {
            for (ServletHandler servletHandler : update.getDestroy())
            {
                servletHandler.destroy();
            }
        }
    }

    private ServletHandler getServletHandler(final ServletInfo servletInfo)
    {
    	Iterator<ServletHandler> it = this.allServletHandlers.iterator();
    	while(it.hasNext())
    	{
    		ServletHandler handler = it.next();
    		if(handler.getServletInfo().compareTo(servletInfo) == 0)
    		{
    			return handler;
    		}
    	}
    	return null;
    }
    
    public synchronized void removeServlet(Servlet servlet, final boolean destroy) throws RegistrationFailureException
    {
    	Iterator<ServletHandler> it = this.allServletHandlers.iterator();
    	while(it.hasNext())
    	{
    		ServletHandler handler = it.next();
    		if(handler.getServlet() == servlet) 
    		{
    			removeServlet(handler.getServletInfo(), destroy);
    		}
    	}
    }
    

    private boolean referencesDispatcherType(FilterHandler handler, DispatcherType dispatcherType)
    {
        return Arrays.asList(handler.getFilterInfo().getDispatcher()).contains(dispatcherType);
    }

    private boolean referencesServletByName(FilterHandler handler, String servletName)
    {
        if (servletName == null)
        {
            return false;
        }
        String[] names = handler.getFilterInfo().getServletNames();
        if (names != null && names.length > 0)
        {
            return Arrays.asList(names).contains(servletName);
        }
        return false;
    }

    public String isMatching(final String requestURI)
    {
        if ( requestURI.equals(this.path) )
        {
        	return "";
        }
        if ( this.prefix == null )
        {
        	return requestURI;
        }
        if ( requestURI.startsWith(this.prefix) )
        {
        	return requestURI.substring(this.prefix.length() - 1);
        }
        return null;
    }

    public long getContextServiceId()
    {
        return this.serviceId;
    }

    public synchronized ContextRuntime getRuntime(FailureRuntime.Builder failureRuntimeBuilder) {
        Collection<ErrorPageRuntime> errorPages = new TreeSet<ErrorPageRuntime>(ServletRuntime.COMPARATOR);
        Collection<ServletHandler> errorHandlers = errorsMapping.getMappedHandlers();
        for (ServletHandler servletHandler : errorHandlers)
        {
            errorPages.add(errorsMapping.getErrorPage(servletHandler));
        }

        Collection<FilterRuntime> filterRuntimes = new TreeSet<FilterRuntime>(FilterRuntime.COMPARATOR);
        for (FilterRuntime filterRuntime : filterMap.values())
        {
            filterRuntimes.add(filterRuntime);
        }

        Collection<ServletRuntime> servletRuntimes = new TreeSet<ServletRuntime>(ServletRuntime.COMPARATOR);
        Collection<ServletRuntime> resourceRuntimes = new TreeSet<ServletRuntime>(ServletRuntime.COMPARATOR);

        for (ServletHandler activeHandler : servletQueue.getActiveValues())
        {
            if (activeHandler.getServletInfo().isResource())
            {
                resourceRuntimes.add(activeHandler);
            }
            else
            {
                servletRuntimes.add(activeHandler);
            }
        }

        for (ServletHandler shadowedHandler : servletQueue.getShadowedValues())
        {
            failureRuntimeBuilder.add(shadowedHandler.getServletInfo(), FAILURE_REASON_SHADOWED_BY_OTHER_SERVICE);
        }

        return new ContextRuntime(servletRuntimes,
                filterRuntimes,
                resourceRuntimes,
                errorPages,
                serviceId);
    }
}
