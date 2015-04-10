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

import static java.util.Arrays.asList;
import static org.osgi.service.http.runtime.dto.DTOConstants.FAILURE_REASON_EXCEPTION_ON_INIT;
import static org.osgi.service.http.runtime.dto.DTOConstants.FAILURE_REASON_SERVICE_ALREAY_USED;
import static org.osgi.service.http.runtime.dto.DTOConstants.FAILURE_REASON_SERVLET_CONTEXT_FAILURE;
import static org.osgi.service.http.runtime.dto.DTOConstants.FAILURE_REASON_SHADOWED_BY_OTHER_SERVICE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.felix.http.base.internal.handler.HandlerRankingMultimap.Update;
import org.apache.felix.http.base.internal.runtime.ServletContextHelperInfo;
import org.apache.felix.http.base.internal.runtime.ServletInfo;
import org.apache.felix.http.base.internal.runtime.dto.FailureRuntime;
import org.apache.felix.http.base.internal.runtime.dto.ServletRegistryRuntime;
import org.apache.felix.http.base.internal.runtime.dto.ServletRuntime;
import org.apache.felix.http.base.internal.util.PatternUtil;
import org.apache.felix.http.base.internal.util.PatternUtil.PatternComparator;
import org.apache.felix.http.base.internal.whiteboard.RegistrationFailureException;

public final class ServletHandlerRegistry
{
    private volatile Map<Long, HandlerMapping<ServletHandler>> servletMappingsPerContext = new HashMap<Long, HandlerMapping<ServletHandler>>();

    private final HandlerRankingMultimap<String> registeredServletHandlers;
    private final SortedSet<ServletHandler> allServletHandlers = new TreeSet<ServletHandler>();

    private final ContextServletHandlerRegistry handlerRegistry;

    public ServletHandlerRegistry()
    {
        handlerRegistry = new ContextServletHandlerRegistry();
        this.registeredServletHandlers = new HandlerRankingMultimap<String>(null, handlerRegistry);
    }

    /**
     * Register default context registry for Http Service
     */
    public void init()
    {
        handlerRegistry.add(0L, new ContextRanking());
    }

    public void shutdown()
    {
        removeAll();
    }

    public void add(@Nonnull ServletContextHelperInfo info)
    {
        handlerRegistry.add(info);
    }

    public void remove(@Nonnull ServletContextHelperInfo info)
    {
        handlerRegistry.remove(info);
    }

    public ServletHandler getServletHandlerByName(final Long contextId, @Nonnull final String name)
    {
        return servletMappingsPerContext.get(contextId).getByName(name);
    }

    public synchronized void addServlet(final ServletHandler handler) throws RegistrationFailureException
    {
        if (this.allServletHandlers.contains(handler))
        {
            throw new RegistrationFailureException(handler.getServletInfo(), FAILURE_REASON_SERVICE_ALREAY_USED,
                "Filter instance " + handler.getName() + " already registered");
        }

        registerServlet(handler);

        this.allServletHandlers.add(handler);
    }

    private void registerServlet(ServletHandler handler) throws RegistrationFailureException
    {
        String contextPath = handlerRegistry.getPath(handler.getContextServiceId());
        if (contextPath == null)
        {
            throw new RegistrationFailureException(handler.getServletInfo(), FAILURE_REASON_SERVLET_CONTEXT_FAILURE);
        }
        contextPath = contextPath.equals("/") ? "" : contextPath;
        List<String> patterns = new ArrayList<String>(asList(handler.getServletInfo().getPatterns()));
        for (int i = 0; i < patterns.size(); i++)
        {
            patterns.set(i, contextPath + patterns.get(i));
        }
        Update<String> update = this.registeredServletHandlers.add(patterns, handler);
        initHandlers(update.getInit());
        updateServletMapping(update, handler.getContextServiceId());
        destroyHandlers(update.getDestroy());
    }

    private void updateServletMapping(Update<String> update, long contextId)
    {
        HandlerMapping<ServletHandler> servletMapping = servletMappingsPerContext.get(contextId);
        if (servletMapping == null)
        {
            servletMapping = new HandlerMapping<ServletHandler>();
        }
        servletMappingsPerContext.put(contextId, servletMapping.update(convert(update.getActivated()), convert(update.getDeactivated())));
    }

    private Map<Pattern, ServletHandler> convert(Map<String, ServletHandler> mapping)
    {
        TreeMap<Pattern, ServletHandler> converted = new TreeMap<Pattern, ServletHandler>(PatternComparator.INSTANCE);
        for (Map.Entry<String, ServletHandler> entry : mapping.entrySet())
        {
            Pattern pattern = Pattern.compile(PatternUtil.convertToRegEx(entry.getKey()));
            converted.put(pattern, entry.getValue());
        }
        return converted;
    }

    public synchronized void removeAll()
    {
        Collection<ServletHandler> servletHandlers = new ArrayList<ServletHandler>();
        for (HandlerMapping<ServletHandler> servletMapping : this.servletMappingsPerContext.values())
        {
            servletHandlers.addAll(servletMapping.values());
        }

        this.servletMappingsPerContext.clear();

        destroyHandlers(servletHandlers);

        this.allServletHandlers.clear();
        this.registeredServletHandlers.clear();
    }

    synchronized Servlet removeServlet(ServletInfo servletInfo) throws RegistrationFailureException
    {
        return removeServlet(0L, servletInfo, true);
    }

    public synchronized Servlet removeServlet(Long contextId, ServletInfo servletInfo) throws RegistrationFailureException
    {
        return removeServlet(contextId, servletInfo, true);
    }

    public synchronized Servlet removeServlet(Long contextId, ServletInfo servletInfo, final boolean destroy) throws RegistrationFailureException
    {
        ServletHandler handler = getServletHandler(servletInfo);
        if (handler == null)
        {
            return null;
        }

        Servlet servlet = handler.getServlet();

        removeServlet(handler, destroy);

        if (destroy)
        {
            handler.destroy();
        }


        return servlet;
    }

    synchronized void removeServlet(Servlet servlet, final boolean destroy) throws RegistrationFailureException
    {
        Iterator<ServletHandler> it = this.allServletHandlers.iterator();
        List<ServletHandler> removals = new ArrayList<ServletHandler>();
        while (it.hasNext())
        {
            ServletHandler handler = it.next();
            if (handler.getServlet() != null && handler.getServlet() == servlet)
            {
                removals.add(handler);
            }
        }

        for (ServletHandler servletHandler : removals)
        {
            removeServlet(0L, servletHandler.getServletInfo(), destroy);
        }
    }

    private void removeServlet(ServletHandler handler, boolean destroy) throws RegistrationFailureException
    {
        String contextPath = handlerRegistry.getPath(handler.getContextServiceId());
        contextPath = contextPath.equals("/") ? "" : contextPath;
        List<String> patterns = new ArrayList<String>(asList(handler.getServletInfo().getPatterns()));
        for (int i = 0; i < patterns.size(); i++)
        {
            patterns.set(i, contextPath + patterns.get(i));
        }
        Update<String> update = this.registeredServletHandlers.remove(patterns, handler);
        initHandlers(update.getInit());
        updateServletMapping(update, handler.getContextServiceId());
        if (destroy)
        {
            destroyHandlers(update.getDestroy());
        }
        this.allServletHandlers.remove(handler);
    }

    private void initHandlers(Collection<ServletHandler> handlers) throws RegistrationFailureException
    {
        for (ServletHandler servletHandler : handlers)
        {
            try
            {
                servletHandler.init();
            }
            catch (ServletException e)
            {
                // TODO we should collect this cases and not throw an exception immediately
                throw new RegistrationFailureException(servletHandler.getServletInfo(), FAILURE_REASON_EXCEPTION_ON_INIT, e);
            }
        }
    }

    private void destroyHandlers(Collection<? extends AbstractHandler<?>> servletHandlers)
    {
        for (AbstractHandler<?> handler : servletHandlers)
        {
            handler.destroy();
        }
    }

    public synchronized ServletHandler getServletHandler(String requestURI)
    {
        List<Long> contextIds = handlerRegistry.getContextId(requestURI);
        for (Long contextId : contextIds)
        {
            HandlerMapping<ServletHandler> servletMapping = this.servletMappingsPerContext.get(contextId);
            if (servletMapping != null)
            {
                ServletHandler bestMatch = servletMapping.getBestMatch(requestURI);
                if (bestMatch != null)
                {
                    return bestMatch;
                }
            }
        }
        return null;
    }

    private ServletHandler getServletHandler(final ServletInfo servletInfo)
    {
        Iterator<ServletHandler> it = this.allServletHandlers.iterator();
        while (it.hasNext())
        {
            ServletHandler handler = it.next();
            if (handler.getServletInfo().compareTo(servletInfo) == 0)
            {
                return handler;
            }
        }
        return null;
    }


    public synchronized ServletRegistryRuntime getRuntime(FailureRuntime.Builder failureRuntimeBuilder)
    {
        addShadowedHandlers(failureRuntimeBuilder, this.registeredServletHandlers.getShadowedValues());

        Collection<ServletRuntime> servletRuntimes = new TreeSet<ServletRuntime>(ServletRuntime.COMPARATOR);
        Collection<ServletRuntime> resourceRuntimes = new TreeSet<ServletRuntime>(ServletRuntime.COMPARATOR);
        for (ServletHandler activeHandler : this.registeredServletHandlers.getActiveValues())
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
        return new ServletRegistryRuntime(servletRuntimes, resourceRuntimes);
    }

    private void addShadowedHandlers(FailureRuntime.Builder failureRuntimeBuilder, Collection<ServletHandler> handlers)
    {
        for (ServletHandler handler : handlers)
        {
            failureRuntimeBuilder.add(handler.getServletInfo(), FAILURE_REASON_SHADOWED_BY_OTHER_SERVICE);
        }
    }

    private static class ContextServletHandlerRegistry implements Comparator<ServletHandler>
    {
        private final Map<Long, ContextRanking> contextRankingsPerId = new HashMap<Long, ContextRanking>();
        private final TreeSet<ContextRanking> contextRankings = new TreeSet<ContextRanking>();

        @Override
        public int compare(ServletHandler o1, ServletHandler o2)
        {
            ContextRanking contextRankingOne = contextRankingsPerId.get(o1.getContextServiceId());
            ContextRanking contextRankingTwo = contextRankingsPerId.get(o2.getContextServiceId());
            int contextComparison = contextRankingOne.compareTo(contextRankingTwo);
            return contextComparison == 0 ? o1.compareTo(o2) : contextComparison;
        }

        synchronized void add(long id, ContextRanking contextRanking)
        {
            contextRankingsPerId.put(id, contextRanking);
            contextRankings.add(contextRanking);
        }

        synchronized void add(ServletContextHelperInfo info)
        {
            add(info.getServiceId(), new ContextRanking(info));
        }

        synchronized void remove(ServletContextHelperInfo info)
        {
            contextRankingsPerId.remove(info.getServiceId());
            contextRankings.remove(new ContextRanking(info));
        }

        synchronized String getPath(long contextId)
        {
            return contextRankingsPerId.get(contextId).path;
        }

        synchronized List<Long> getContextId(String path)
        {
            List<Long> ids = new ArrayList<Long>();
            for (ContextRanking contextRanking : contextRankings)
            {
                if (contextRanking.isMatching(path))
                {
                    ids.add(contextRanking.serviceId);
                }
            }
            return ids;
        }
    }

    // TODO combine with PerContextHandlerRegistry
    private static class ContextRanking implements Comparable<ContextRanking>
    {
        private final long serviceId;
        private final int ranking;
        private final String path;

        ContextRanking()
        {
            this.serviceId = 0;
            this.ranking = Integer.MAX_VALUE;
            this.path = "/";
        }

        ContextRanking(ServletContextHelperInfo info)
        {
            this.serviceId = info.getServiceId();
            this.ranking = info.getRanking();
            this.path = info.getPath();
        }

        @Override
        public int compareTo(ContextRanking other)
        {
            // the context of the HttpService is the least element
            if (this.serviceId == 0 ^ other.serviceId == 0)
            {
                return this.serviceId == 0 ? -1 : 1;
            }

            final int result = Integer.compare(other.path.length(), this.path.length());
            if ( result == 0 ) {
                if (this.ranking == other.ranking)
                {
                    // Service id's can be negative. Negative id's follow the reverse natural ordering of integers.
                    int reverseOrder = ( this.serviceId <= 0 && other.serviceId <= 0 ) ? -1 : 1;
                    return reverseOrder * Long.compare(this.serviceId, other.serviceId);
                }

                return Integer.compare(other.ranking, this.ranking);
            }
            return result;
        }

        public boolean isMatching(final String requestURI)
        {
            return "".equals(path) || "/".equals(path) || requestURI.startsWith(path);
        }
    }
}
