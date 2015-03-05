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
package org.apache.felix.http.itest;

import static java.util.Arrays.asList;
import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.ServletRequestAttributeListener;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionListener;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.http.runtime.HttpServiceRuntime;
import org.osgi.service.http.runtime.HttpServiceRuntimeConstants;
import org.osgi.service.http.runtime.dto.RuntimeDTO;
import org.osgi.service.http.runtime.dto.ServletContextDTO;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

@RunWith(JUnit4TestRunner.class)
public class HttpServiceRuntimeTest extends BaseIntegrationTest
{
    private static final long DEFAULT_SLEEP = 10;

    private void registerServlet(String name, String path)
    {
        Dictionary<String, ?> properties = createDictionary(
                HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, path,
                HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, name);

        m_context.registerService(Servlet.class.getName(), new TestServlet(), properties);
    }

    private void registerFilter(String name, String path)
    {
        Dictionary<String, ?> properties = createDictionary(
                HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, path,
                HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME, name);

        m_context.registerService(Filter.class.getName(), new TestFilter(), properties);
    }

    private void registerResource(String prefix, String path)
    {
        Dictionary<String, ?> properties = createDictionary(
                HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PATTERN, path,
                HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PREFIX, prefix);

        m_context.registerService(TestResource.class.getName(), new TestResource(), properties);
    }

    private void registerErrorPage(String name, String path, List<String> errors)
    {
        Dictionary<String, ?> properties = createDictionary(
                HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_ERROR_PAGE, errors,
                HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, name);

        m_context.registerService(Servlet.class.getName(), new TestServlet(), properties);
    }

    private void registerListener(Class<?> listenerClass, boolean useWithWhiteboard)
    {
        Dictionary<String, ?> properties = createDictionary(
                HttpWhiteboardConstants.HTTP_WHITEBOARD_LISTENER, useWithWhiteboard ? "true" : "false");

        m_context.registerService(listenerClass.getName(), mock(listenerClass), properties);
    }

    private void registerContext(String name, String path)
    {
        Dictionary<String, ?> properties = createDictionary(
                HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, name,
                HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, path);

        m_context.registerService(ServletContextHelper.class.getName(), mock(ServletContextHelper.class), properties);
    }

    @Test
    public void testRuntimeAvailable() throws Exception
    {
        awaitService(HttpServiceRuntime.class.getName());

        HttpServiceRuntime serviceRuntime = (HttpServiceRuntime) getService(HttpServiceRuntime.class.getName());
        assertNotNull("HttpServiceRuntime unavailable", serviceRuntime);

        RuntimeDTO runtimeDTO = serviceRuntime.getRuntimeDTO();
        Map<String, String> runtimeDTOAttributes = runtimeDTO.attributes;

        assertNotNull(runtimeDTOAttributes);
        assertTrue(runtimeDTOAttributes.containsKey(HttpServiceRuntimeConstants.HTTP_SERVICE_ID_ATTRIBUTE));
        assertTrue(runtimeDTOAttributes.containsKey(HttpServiceRuntimeConstants.HTTP_SERVICE_ENDPOINT_ATTRIBUTE));
        assertTrue(0 < Integer.valueOf(runtimeDTOAttributes.get(HttpServiceRuntimeConstants.HTTP_SERVICE_ID_ATTRIBUTE)));

        assertEquals(0, runtimeDTO.failedErrorPageDTOs.length);
        assertEquals(0, runtimeDTO.failedFilterDTOs.length);
        assertEquals(0, runtimeDTO.failedListenerDTOs.length);
        assertEquals(0, runtimeDTO.failedResourceDTOs.length);
        assertEquals(0, runtimeDTO.failedServletContextDTOs.length);
        assertEquals(0, runtimeDTO.failedServletDTOs.length);

        assertEquals(1, runtimeDTO.servletContextDTOs.length);
        assertEquals("default", runtimeDTO.servletContextDTOs[0].name);
        assertEquals(0, runtimeDTO.servletContextDTOs[0].attributes.size());
        // TODO ??
//        assertTrue(0 > runtimeDTO.servletContextDTOs[0].serviceId);
        assertEquals("", runtimeDTO.servletContextDTOs[0].contextPath);
        assertEquals(0, runtimeDTO.servletContextDTOs[0].initParams.size());

        assertEquals(0, runtimeDTO.servletContextDTOs[0].filterDTOs.length);
        assertEquals(0, runtimeDTO.servletContextDTOs[0].servletDTOs.length);
        assertEquals(0, runtimeDTO.servletContextDTOs[0].resourceDTOs.length);
        assertEquals(0, runtimeDTO.servletContextDTOs[0].errorPageDTOs.length);
        assertEquals(0, runtimeDTO.servletContextDTOs[0].listenerDTOs.length);
    }

    @Test
    public void testServletsInRuntime() throws Exception
    {
        registerServlet("testServlet 1", "/servlet_1");
        awaitService(Servlet.class.getName());

        HttpServiceRuntime serviceRuntime = (HttpServiceRuntime) getService(HttpServiceRuntime.class.getName());
        assertNotNull("HttpServiceRuntime unavailable", serviceRuntime);

        RuntimeDTO runtimeDTOWithFirstSerlvet = serviceRuntime.getRuntimeDTO();

        assertEquals(0, runtimeDTOWithFirstSerlvet.failedServletDTOs.length);
        assertEquals(1, runtimeDTOWithFirstSerlvet.servletContextDTOs.length);

        ServletContextDTO contextDTO = runtimeDTOWithFirstSerlvet.servletContextDTOs[0];
        assertEquals(1, contextDTO.servletDTOs.length);
        assertEquals("testServlet 1", contextDTO.servletDTOs[0].name);

        registerServlet("testServlet 2", "/servlet_2");
        awaitServices(Servlet.class.getName(), 2);

        RuntimeDTO runtimeDTOWithBothSerlvets = serviceRuntime.getRuntimeDTO();

        assertNotSame(runtimeDTOWithFirstSerlvet, runtimeDTOWithBothSerlvets);
        assertEquals(0, runtimeDTOWithBothSerlvets.failedServletDTOs.length);
        assertEquals(1, runtimeDTOWithBothSerlvets.servletContextDTOs.length);

        contextDTO = runtimeDTOWithBothSerlvets.servletContextDTOs[0];
        assertEquals(2, contextDTO.servletDTOs.length);
        assertEquals("testServlet 1", contextDTO.servletDTOs[0].name);
        assertEquals("testServlet 2", contextDTO.servletDTOs[1].name);
    }

    @Test
    public void testFiltersInRuntime() throws Exception
    {
        registerFilter("testFilter 1", "/servlet_1");
        awaitService(Filter.class.getName());

        HttpServiceRuntime serviceRuntime = (HttpServiceRuntime) getService(HttpServiceRuntime.class.getName());
        assertNotNull("HttpServiceRuntime unavailable", serviceRuntime);

        RuntimeDTO runtimeDTOWithFirstFilter = serviceRuntime.getRuntimeDTO();

        assertEquals(0, runtimeDTOWithFirstFilter.failedFilterDTOs.length);
        assertEquals(1, runtimeDTOWithFirstFilter.servletContextDTOs.length);

        ServletContextDTO contextDTO = runtimeDTOWithFirstFilter.servletContextDTOs[0];
        assertEquals(1, contextDTO.filterDTOs.length);
        assertEquals("testFilter 1", contextDTO.filterDTOs[0].name);

        registerFilter("testFilter 2", "/servlet_1");
        awaitServices(Filter.class.getName(), 2);

        RuntimeDTO runtimeDTOWithBothFilters = serviceRuntime.getRuntimeDTO();

        assertNotSame(runtimeDTOWithFirstFilter, runtimeDTOWithBothFilters);
        assertEquals(0, runtimeDTOWithBothFilters.failedFilterDTOs.length);
        assertEquals(1, runtimeDTOWithBothFilters.servletContextDTOs.length);

        contextDTO = runtimeDTOWithBothFilters.servletContextDTOs[0];
        assertEquals(2, contextDTO.filterDTOs.length);
        assertEquals("testFilter 1", contextDTO.filterDTOs[0].name);
        assertEquals("testFilter 2", contextDTO.filterDTOs[1].name);
    }

    @Test
    public void testResourcesInRuntime() throws Exception
    {
        registerResource("/resources", "/resource_1/*");
        awaitService(TestResource.class.getName());

        HttpServiceRuntime serviceRuntime = (HttpServiceRuntime) getService(HttpServiceRuntime.class.getName());
        assertNotNull("HttpServiceRuntime unavailable", serviceRuntime);

        RuntimeDTO runtimeDTOWithFirstResource = serviceRuntime.getRuntimeDTO();

        assertEquals(0, runtimeDTOWithFirstResource.failedResourceDTOs.length);
        assertEquals(1, runtimeDTOWithFirstResource.servletContextDTOs.length);

        ServletContextDTO contextDTO = runtimeDTOWithFirstResource.servletContextDTOs[0];
        assertEquals(1, contextDTO.resourceDTOs.length);
        assertEquals("/resources", contextDTO.resourceDTOs[0].prefix);
        assertArrayEquals(new String[] { "/resource_1/*" }, contextDTO.resourceDTOs[0].patterns);

        registerResource("/resources", "/resource_2/*");
        awaitServices(TestResource.class.getName(), 2);

        RuntimeDTO runtimeDTOWithBothResources = serviceRuntime.getRuntimeDTO();

        assertNotSame(runtimeDTOWithFirstResource, runtimeDTOWithBothResources);
        assertEquals(0, runtimeDTOWithBothResources.failedResourceDTOs.length);
        assertEquals(1, runtimeDTOWithBothResources.servletContextDTOs.length);

        contextDTO = runtimeDTOWithBothResources.servletContextDTOs[0];
        assertEquals(2, contextDTO.resourceDTOs.length);
        assertEquals("/resources", contextDTO.resourceDTOs[0].prefix);
        assertArrayEquals(new String[] { "/resource_1/*" }, contextDTO.resourceDTOs[0].patterns);
        assertEquals("/resources", contextDTO.resourceDTOs[1].prefix);
        assertArrayEquals(new String[] { "/resource_2/*" }, contextDTO.resourceDTOs[1].patterns);
    }

    @Test
    public void testErrorPagesInRuntime() throws Exception
    {
        registerErrorPage("error page 1", "/error/404", asList("404", NoSuchElementException.class.getName()));
        awaitService(Servlet.class.getName());

        HttpServiceRuntime serviceRuntime = (HttpServiceRuntime) getService(HttpServiceRuntime.class.getName());
        assertNotNull("HttpServiceRuntime unavailable", serviceRuntime);

        RuntimeDTO runtimeDTOWithFirstErrorPage = serviceRuntime.getRuntimeDTO();

        assertEquals(0, runtimeDTOWithFirstErrorPage.failedServletDTOs.length);
        assertEquals(0, runtimeDTOWithFirstErrorPage.failedErrorPageDTOs.length);
        assertEquals(1, runtimeDTOWithFirstErrorPage.servletContextDTOs.length);

        ServletContextDTO contextDTO = runtimeDTOWithFirstErrorPage.servletContextDTOs[0];
        assertEquals(1, contextDTO.errorPageDTOs.length);
        assertEquals("error page 1", contextDTO.errorPageDTOs[0].name);
        assertArrayEquals(new String[] { NoSuchElementException.class.getName() }, contextDTO.errorPageDTOs[0].exceptions);
        assertArrayEquals(new long[] { 404 }, contextDTO.errorPageDTOs[0].errorCodes);

        registerErrorPage("error page 2", "/error/500", asList("500", ServletException.class.getName()));
        awaitServices(Servlet.class.getName(), 2);

        RuntimeDTO runtimeDTOWithBothErrorPages = serviceRuntime.getRuntimeDTO();

        assertNotSame(runtimeDTOWithFirstErrorPage, runtimeDTOWithBothErrorPages);
        assertEquals(0, runtimeDTOWithBothErrorPages.failedServletDTOs.length);
        assertEquals(0, runtimeDTOWithBothErrorPages.failedErrorPageDTOs.length);
        assertEquals(1, runtimeDTOWithBothErrorPages.servletContextDTOs.length);

        contextDTO = runtimeDTOWithBothErrorPages.servletContextDTOs[0];
        assertEquals(2, contextDTO.errorPageDTOs.length);
        assertEquals("error page 1", contextDTO.errorPageDTOs[0].name);
        assertEquals("error page 2", contextDTO.errorPageDTOs[1].name);
        assertArrayEquals(new String[] { ServletException.class.getName() }, contextDTO.errorPageDTOs[1].exceptions);
        assertArrayEquals(new long[] { 500 }, contextDTO.errorPageDTOs[1].errorCodes);
    }

    @Test
    public void testListenersInRuntime() throws Exception
    {
        registerListener(ServletContextListener.class, true);
        awaitService(ServletContextListener.class.getName());

        HttpServiceRuntime serviceRuntime = (HttpServiceRuntime) getService(HttpServiceRuntime.class.getName());
        assertNotNull("HttpServiceRuntime unavailable", serviceRuntime);

        RuntimeDTO runtimeDTOWithFirstListener = serviceRuntime.getRuntimeDTO();

        assertEquals(0, runtimeDTOWithFirstListener.failedListenerDTOs.length);
        assertEquals(1, runtimeDTOWithFirstListener.servletContextDTOs.length);

        ServletContextDTO contextDTO = runtimeDTOWithFirstListener.servletContextDTOs[0];
        // TODO fix : servlet context listener is only added when registerd before context activation
        assertEquals(0, contextDTO.listenerDTOs.length);
        // TODO
//        assertEquals(ServletContextListener.class.getName(), contextDTO.listenerDTOs[0].types[0]);

        registerListener(ServletContextAttributeListener.class, true);
        registerListener(ServletRequestListener.class, true);
        registerListener(ServletRequestAttributeListener.class, true);
        registerListener(HttpSessionListener.class, true);
        registerListener(HttpSessionAttributeListener.class, true);

        awaitService(ServletContextAttributeListener.class.getName());
        awaitService(ServletRequestListener.class.getName());
        awaitService(ServletRequestAttributeListener.class.getName());
        awaitService(HttpSessionListener.class.getName());
        awaitService(HttpSessionAttributeListener.class.getName());

        RuntimeDTO runtimeDTOWithAllListeners = serviceRuntime.getRuntimeDTO();

        assertNotSame(runtimeDTOWithFirstListener, runtimeDTOWithAllListeners);
        assertEquals(0, runtimeDTOWithAllListeners.failedListenerDTOs.length);
        assertEquals(1, runtimeDTOWithAllListeners.servletContextDTOs.length);

        contextDTO = runtimeDTOWithAllListeners.servletContextDTOs[0];
        // TODO
        assertEquals(5, contextDTO.listenerDTOs.length);
//        assertEquals(ServletContextListener.class.getName(), contextDTO.listenerDTOs[0].types[0]);
        // TODO for listeners ordering is reverse compared to other services
        assertEquals(ServletContextAttributeListener.class.getName(), contextDTO.listenerDTOs[4].types[0]);
        assertEquals(ServletRequestListener.class.getName(), contextDTO.listenerDTOs[3].types[0]);
        assertEquals(ServletRequestAttributeListener.class.getName(), contextDTO.listenerDTOs[2].types[0]);
        assertEquals(HttpSessionListener.class.getName(), contextDTO.listenerDTOs[1].types[0]);
        assertEquals(HttpSessionAttributeListener.class.getName(), contextDTO.listenerDTOs[0].types[0]);
    }

    @Test
    public void testContextsInRuntime() throws Exception
    {
        registerContext("contextA", "/contextA");
        awaitServices(ServletContextHelper.class.getName(), 2);

        HttpServiceRuntime serviceRuntime = (HttpServiceRuntime) getService(HttpServiceRuntime.class.getName());
        assertNotNull("HttpServiceRuntime unavailable", serviceRuntime);

        RuntimeDTO runtimeDTOWithAdditionalContext = serviceRuntime.getRuntimeDTO();

        assertEquals(0, runtimeDTOWithAdditionalContext.failedServletContextDTOs.length);
        assertEquals(2, runtimeDTOWithAdditionalContext.servletContextDTOs.length);

        // TODO order ?
        assertEquals("contextA", runtimeDTOWithAdditionalContext.servletContextDTOs[0].name);
        assertEquals("/contextA", runtimeDTOWithAdditionalContext.servletContextDTOs[0].contextPath);
        assertEquals("default", runtimeDTOWithAdditionalContext.servletContextDTOs[1].name);
        assertEquals("", runtimeDTOWithAdditionalContext.servletContextDTOs[1].contextPath);

        registerContext("contextB", "/contextB");
        awaitServices(ServletContextHelper.class.getName(), 3);

        RuntimeDTO runtimeDTOWithAllContexts = serviceRuntime.getRuntimeDTO();

        assertEquals(0, runtimeDTOWithAllContexts.failedServletContextDTOs.length);
        assertEquals(3, runtimeDTOWithAllContexts.servletContextDTOs.length);

        // TODO order ?
        assertEquals("contextA", runtimeDTOWithAllContexts.servletContextDTOs[0].name);
        assertEquals("/contextA", runtimeDTOWithAllContexts.servletContextDTOs[0].contextPath);
        assertEquals("contextB", runtimeDTOWithAllContexts.servletContextDTOs[1].name);
        assertEquals("/contextB", runtimeDTOWithAllContexts.servletContextDTOs[1].contextPath);
        assertEquals("default", runtimeDTOWithAllContexts.servletContextDTOs[2].name);
        assertEquals("", runtimeDTOWithAllContexts.servletContextDTOs[2].contextPath);
    }

    private void awaitServices(String serviceName, int count)
    {
        long elapsed = 0;
        while(getServiceReferences(serviceName).length < count)
        {
            if (elapsed <= DEFAULT_TIMEOUT)
            {
                try
                {
                    Thread.sleep(DEFAULT_SLEEP);
                } catch (InterruptedException e)
                {
                    return;
                }
                elapsed += DEFAULT_SLEEP;
            }
            else
            {
                fail("Gave up on waiting for " + count  + " services for " + serviceName + " to be available." );
            }
        }
    }

    public static class TestResource
    {
        // Tagging class
    }
}
