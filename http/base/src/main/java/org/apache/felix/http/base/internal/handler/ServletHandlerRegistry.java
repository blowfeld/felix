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
import static org.apache.felix.http.base.internal.util.CompareUtil.compareSafely;
import static org.osgi.service.http.runtime.dto.DTOConstants.FAILURE_REASON_EXCEPTION_ON_INIT;
import static org.osgi.service.http.runtime.dto.DTOConstants.FAILURE_REASON_SERVICE_ALREAY_USED;
import static org.osgi.service.http.runtime.dto.DTOConstants.FAILURE_REASON_SERVLET_CONTEXT_FAILURE;
import static org.osgi.service.http.runtime.dto.DTOConstants.FAILURE_REASON_SHADOWED_BY_OTHER_SERVICE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.annotation.Nonnull;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.felix.http.base.internal.handler.trie.ColoredValue;
import org.apache.felix.http.base.internal.handler.trie.Node;
import org.apache.felix.http.base.internal.handler.trie.PriorityTree;
import org.apache.felix.http.base.internal.handler.trie.PriorityTrie;
import org.apache.felix.http.base.internal.handler.trie.SearchPath;
import org.apache.felix.http.base.internal.runtime.ServletContextHelperInfo;
import org.apache.felix.http.base.internal.runtime.ServletInfo;
import org.apache.felix.http.base.internal.runtime.dto.FailureRuntime;
import org.apache.felix.http.base.internal.runtime.dto.ServletRegistryRuntime;
import org.apache.felix.http.base.internal.runtime.dto.ServletRuntime;
import org.apache.felix.http.base.internal.whiteboard.RegistrationFailureException;

final class ServletHandlerRegistry
{
    private static final Integer ZERO = Integer.valueOf(0);
    private static final Integer ONE = Integer.valueOf(1);

    private final Set<ServletHandler> initFailures = new TreeSet<ServletHandler>();
    private final Map<ServletHandler, Integer> useCounts = new TreeMap<ServletHandler, Integer>();
    private final Map<Long, ContextRanking> contextsById = new HashMap<Long, ContextRanking>();

    private volatile PriorityTree<ServletHandler, ContextRanking> servletHandlers;

    /**
     * Register default context registry for Http Service
     */
    synchronized void init()
    {
        contextsById.put(0L, new ContextRanking());
        servletHandlers = new PriorityTrie<ServletHandler, ContextRanking>();
    }

    synchronized void shutdown()
    {
        removeAll();
    }

    synchronized void add(ServletContextHelperInfo info)
    {
        contextsById.put(info.getServiceId(), new ContextRanking(info));
    }

    synchronized void remove(ServletContextHelperInfo info)
    {
        contextsById.remove(info.getServiceId());
    }

    synchronized void addServlet(final ServletHandler handler) throws RegistrationFailureException
    {
        if (this.useCounts.keySet().contains(handler))
        {
            throw new RegistrationFailureException(handler.getServletInfo(), FAILURE_REASON_SERVICE_ALREAY_USED,
                "Servlet instance " + handler.getName() + " already registered");
        }

        registerServlet(handler);
    }

    private void registerServlet(ServletHandler handler) throws RegistrationFailureException
    {
        List<String> patterns = getFullPathsChecked(handler);
        // TODO
        Collections.sort(patterns, Collections.reverseOrder());

        for (String path : patterns)
        {
            registerServlet(path, handler);
        }
    }

    private void registerServlet(String path, ServletHandler handler) throws RegistrationFailureException
    {
        ContextRanking contextRanking = contextsById.get(handler.getContextServiceId());
        SearchPath searchPath = SearchPath.forPattern(path);

        Node<ServletHandler, ContextRanking> parentNode = servletHandlers.search(searchPath);

        List<ServletHandler> destroyList = findShadowedNodes(handler, path, contextRanking, parentNode);

        if (!isShadowed(handler, searchPath, contextRanking, parentNode))
        {
            initHandler(handler);
        }

        // TODO addAll ?
        servletHandlers = servletHandlers.add(searchPath, handler, contextRanking);

        destroyHandlers(destroyList);
    }

    private boolean isShadowed(ServletHandler handler, SearchPath path, ContextRanking handlerColor, Node<ServletHandler, ContextRanking> node)
    {
        if (node == null)
        {
            return false;
        }
        return isShadowed(handler, path, handlerColor,
            node.firstValue(), node.getPath(), servletHandlers.getColor(node));
    }

    private boolean isShadowed(ServletHandler handler, SearchPath path, ContextRanking handlerColor,
        ServletHandler otherHandler, SearchPath otherPath, ContextRanking otherColor)
    {
        int contextComparison = otherColor.compareTo(handlerColor);
        if (contextComparison < 0)
        {
            return true;
        }

        if (path.equals(otherPath) && contextComparison == 0 && otherHandler.compareTo(handler) < 0)
        {
            return true;
        }
        return false;
    }

    private List<ServletHandler> findShadowedNodes(ServletHandler handler, String path, ContextRanking handlerColor, Node<ServletHandler, ContextRanking> parent)
    {
        if (parent != null && servletHandlers.getColor(parent).compareTo(handlerColor) < 0)
        {
            return Collections.emptyList();
        }

        List<ServletHandler> destroy = new ArrayList<ServletHandler>();

        SearchPath searchPath = SearchPath.forPattern(path);
        PriorityTree<ServletHandler, ContextRanking> subtrie = servletHandlers.getSubtrie(searchPath);
        for (Node<ServletHandler, ContextRanking> node : subtrie)
        {
            ServletHandler nodeHandler = node.firstValue();
            if (isShadowed(nodeHandler, node.getPath(), servletHandlers.getColor(node), handler, searchPath, handlerColor))
            {
                destroy.add(nodeHandler);
            }
        }
        return destroy;
    }

    private void initHandler(ServletHandler handler) throws RegistrationFailureException
    {
        if (!useCounts.containsKey(handler))
        {
            useCounts.put(handler, 0);
        }

        int useCount = useCounts.get(handler);
        if (useCount == 0)
        {
            try
            {
                handler.init();
            }
            catch (ServletException e)
            {
                useCounts.remove(handler);
                throw new RegistrationFailureException(handler.getServletInfo(), FAILURE_REASON_EXCEPTION_ON_INIT, e);
            }
        }

        useCounts.put(handler, useCount + 1);
    }

    private void destroyHandlers(List<ServletHandler> destroyList)
    {
        destroyHandlers(destroyList, false);
    }

    private void destroyHandlers(Collection<ServletHandler> destroyList, boolean force)
    {
        for (ServletHandler servletHandler : destroyList)
        {
            Integer useCount = useCounts.get(servletHandler);
            if (ONE.equals(useCount) || force)
            {
                servletHandler.destroy();
            }

            if (useCount != null && (useCount.compareTo(ZERO) > 0))
            {
                useCounts.put(servletHandler, useCount - 1);
            }
        }
    }

    synchronized void removeAll()
    {
        Collection<ServletHandler> oldHandlers = new TreeSet<ServletHandler>(servletHandlers.activeValues());
        this.servletHandlers = new PriorityTrie<ServletHandler, ContextRanking>();

        destroyHandlers(oldHandlers, true);

        this.contextsById.clear();
        this.useCounts.clear();
    }

    synchronized Servlet removeServlet(ServletInfo servletInfo)
    {
        return removeServlet(0L, servletInfo, true);
    }

    synchronized Servlet removeServlet(Long contextId, ServletInfo servletInfo)
    {
        return removeServlet(contextId, servletInfo, true);
    }

    synchronized Servlet removeServlet(Long contextId, ServletInfo servletInfo, final boolean destroy)
    {
        ServletHandler handler = getServletHandler(contextId, servletInfo);
        if (handler == null)
        {
            return null;
        }

        Servlet servlet = handler.getServlet();
        removeServlet(handler, destroy);

        useCounts.remove(handler);

        return servlet;
    }

    private ServletHandler getServletHandler(Long contextId, ServletInfo servletInfo)
    {
        ContextRanking contextRanking = contextsById.get(contextId);
        List<String> paths = getFullPaths(contextRanking.path, servletInfo);

        for (String path : paths)
        {
            Node<ServletHandler, ContextRanking> node = servletHandlers.getParent(SearchPath.forPattern(path));
            for (ColoredValue<ServletHandler, ContextRanking> value : node.getValues())
            {
                ServletHandler servletHandler = value.getValue();
                if (servletHandler.getServletInfo().equals(servletInfo))
                {
                    return servletHandler;
                }
            }
        }
        return null;
    }

    synchronized void removeServlet(Servlet servlet, final boolean destroy)
    {
        Iterator<ServletHandler> it = this.useCounts.keySet().iterator();
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

    private void removeServlet(ServletHandler handler, boolean destroy)
    {
        List<String> patterns = getFullPaths(handler);
        this.initFailures.remove(handler);

        for (String path : patterns)
        {
            removeServlet(path, handler, destroy);
        }
    }

    private void removeServlet(String path, ServletHandler handler, boolean destroy)
    {
        SearchPath searchPath = SearchPath.forPattern(path);
        Node<ServletHandler, ContextRanking> node = servletHandlers.getParent(searchPath);
        if (node == null || !searchPath.equals(node.getPath()))
        {
            // no such path registered
            return;
        }

        ContextRanking oldNodeColor = servletHandlers.getColor(node);
        ContextRanking contextColor = contextsById.get(handler.getContextServiceId());
        PriorityTree<ServletHandler, ContextRanking> newHandlers = servletHandlers.remove(searchPath, handler, contextColor);

        Node<ServletHandler, ContextRanking> newParent = newHandlers.getParent(searchPath);
        boolean nodeRemoved = newParent == null || newParent.getPath() == null ||
            !searchPath.equals(newParent.getPath());

        ContextRanking newColor = newHandlers.getColor(newParent);
        if (!nodeRemoved && compareSafely(newParent.getValueColor(), newColor) <= 0)
        {
            initNode(node, newParent);
        }

        if (compareSafely(newColor, oldNodeColor) > 0)
        {
            PriorityTree<ServletHandler, ContextRanking> subtrie = newHandlers.getSubtrie(searchPath);
            initSubtrie(subtrie, newColor, oldNodeColor);
        }

        servletHandlers = newHandlers;
        if (destroy)
        {
            destroyHandlers(asList(handler));
        }
    }

    private void initNode(Node<ServletHandler, ContextRanking> node, Node<ServletHandler, ContextRanking> newParent)
    {
        ServletHandler newHead = newParent.firstValue();
        if (node.firstValue().compareTo(newHead) != 0)
        {
            try
            {
                initHandler(newHead);
            }
            catch (RegistrationFailureException e)
            {
                initFailures.add(newHead);
            }
        }
    }

    private void initSubtrie(PriorityTree<ServletHandler, ContextRanking> subtrie,
        ContextRanking newColor, ContextRanking oldColor)
    {
        for (Node<ServletHandler, ContextRanking> node : subtrie)
        {
            ContextRanking nodeColor = subtrie.getColor(node);
            if (compareSafely(nodeColor, oldColor) > 0 && compareSafely(nodeColor, newColor) <= 0)
            {
                ServletHandler nodeHead = node.firstValue();
                try
                {
                    initHandler(nodeHead);
                }
                catch (RegistrationFailureException e)
                {
                    initFailures.add(nodeHead);
                }
            }
        }
    }

    private List<String> getFullPathsChecked(ServletHandler handler) throws RegistrationFailureException
    {
        String contextPath = contextsById.get(handler.getContextServiceId()).path;
        if (contextPath == null)
        {
            throw new RegistrationFailureException(handler.getServletInfo(), FAILURE_REASON_SERVLET_CONTEXT_FAILURE);
        }
        return getFullPaths(contextPath, handler.getServletInfo());
    }

    private List<String> getFullPaths(ServletHandler handler)
    {
        String contextPath = contextsById.get(handler.getContextServiceId()).path;
        return getFullPaths(contextPath, handler.getServletInfo());
    }

    private List<String> getFullPaths(String contextPath, ServletInfo info)
    {
        contextPath = contextPath.equals("/") ? "" : contextPath;

        List<String> patterns = new ArrayList<String>(asList(info.getPatterns()));
        for (int i = 0; i < patterns.size(); i++)
        {
            String pattern = patterns.get(i);
            pattern = pattern.startsWith("/") ? pattern : "/" + pattern;
            patterns.set(i, contextPath + pattern);
        }
        return patterns;
    }

    ServletHandler getServletHandler(String requestURI)
    {
        PriorityTree<ServletHandler, ContextRanking> currentHandlers = servletHandlers;

        Node<ServletHandler, ContextRanking> pathNode = currentHandlers.search(SearchPath.forPath(requestURI));
        SearchPath extensionPath = SearchPath.forExtensionPath(requestURI);
        Node<ServletHandler, ContextRanking> extensionNode = null;
        if (extensionPath != null)
        {
            extensionNode = currentHandlers.search(extensionPath);
        }

        Node<ServletHandler, ContextRanking> result = null;
        if (pathNode == null && extensionNode == null)
        {
            return null;
        }
        else if (pathNode != null && extensionNode != null)
        {
            ContextRanking pathRanking = extensionNode.getValueColor();
            ContextRanking extensionRanking = pathNode.getValueColor();
            result = compareSafely(pathRanking, extensionRanking) < 0 ? extensionNode : pathNode;
        }
        else
        {
            result = pathNode != null ? pathNode : extensionNode;
        }

        ServletHandler servletHandler = result.firstValue();
        if (initFailures.contains(servletHandler))
        {
            return null;
        }
        return servletHandler;
    }

    ServletHandler getServletHandlerByName(final Long contextId, @Nonnull final String name)
    {
        SearchPath searchPath = SearchPath.forPattern(contextsById.get(contextId).path);
        PriorityTree<ServletHandler, ContextRanking> contextTrie = servletHandlers.getSubtrie(searchPath);
        for (Node<ServletHandler, ContextRanking> node : contextTrie)
        {
            ServletHandler servletHandler = node.firstValue();
            if (servletHandler.getName().equals(name))
            {
                return servletHandler;
            }
        }
        return null;
    }

    synchronized ServletRegistryRuntime getRuntime(FailureRuntime.Builder failureRuntimeBuilder)
    {
        addFailures(failureRuntimeBuilder, this.initFailures, FAILURE_REASON_EXCEPTION_ON_INIT);

        Collection<ServletRuntime> servletRuntimes = new TreeSet<ServletRuntime>(ServletRuntime.COMPARATOR);
        Collection<ServletRuntime> resourceRuntimes = new TreeSet<ServletRuntime>(ServletRuntime.COMPARATOR);
        for (Map.Entry<ServletHandler, Integer> countEntry : this.useCounts.entrySet())
        {
            ServletHandler handler = countEntry.getKey();

            // TODO remove initFailures from trie and useCounts
            if (initFailures.contains(handler))
            {
                continue;
            }

            if (countEntry.getValue() > 0)
            {
                if (handler.getServletInfo().isResource())
                {
                    resourceRuntimes.add(handler);
                }
                else
                {
                    servletRuntimes.add(handler);
                }
            }
            else
            {
                failureRuntimeBuilder.add(handler.getServletInfo(), FAILURE_REASON_SHADOWED_BY_OTHER_SERVICE);
            }
        }

        return new ServletRegistryRuntime(servletRuntimes, resourceRuntimes);
    }

    private void addFailures(FailureRuntime.Builder failureRuntimeBuilder, Collection<ServletHandler> handlers, int failureCode)
    {
        for (ServletHandler handler : handlers)
        {
            failureRuntimeBuilder.add(handler.getServletInfo(), failureCode);
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
    }
}
