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
import static org.osgi.service.http.runtime.dto.DTOConstants.FAILURE_REASON_NO_SERVLET_CONTEXT_MATCHING;
import static org.osgi.service.http.runtime.dto.DTOConstants.FAILURE_REASON_SHADOWED_BY_OTHER_SERVICE;
import static org.osgi.service.http.runtime.dto.DTOConstants.FAILURE_REASON_VALIDATION_FAILED;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME;

import java.util.Arrays;
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

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.http.runtime.HttpServiceRuntime;
import org.osgi.service.http.runtime.HttpServiceRuntimeConstants;
import org.osgi.service.http.runtime.dto.FailedServletDTO;
import org.osgi.service.http.runtime.dto.RuntimeDTO;
import org.osgi.service.http.runtime.dto.ServletContextDTO;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

@RunWith(JUnit4TestRunner.class)
public class HttpServiceRuntimeTest extends BaseIntegrationTest
{
    private static final long DEFAULT_SLEEP = 10;

    private void registerServlet(String name, String path)
    {
        registerServlet(name, path, null);
    }

    private void registerServlet(String name, String path, String context)
    {
        List<Object> propertyEntries = Arrays.<Object>asList(
                HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, path,
                HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, name,
                HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, context);

        Dictionary<String, ?> properties = createDictionary(context == null ?
                propertyEntries.subList(0, 4).toArray() : propertyEntries.toArray());

        m_context.registerService(Servlet.class.getName(), new TestServlet(), properties);
    }

    private void registerFilter(String name, String path)
    {
        registerFilter(name, path, null);
    }

    private void registerFilter(String name, String path, String context)
    {
        List<Object> propertyEntries = Arrays.<Object>asList(
                HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, path,
                HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME, name,
                HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, context);

        Dictionary<String, ?> properties = createDictionary(context == null ?
                propertyEntries.subList(0, 4).toArray() : propertyEntries.toArray());

        m_context.registerService(Filter.class.getName(), new TestFilter(), properties);
    }

    private void registerResource(String prefix, String path)
    {
        registerResource(prefix, path, null);
    }

    private void registerResource(String prefix, String path, String context)
    {
        List<Object> propertyEntries = Arrays.<Object>asList(
                HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PATTERN, path,
                HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PREFIX, prefix,
                HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, context);

        Dictionary<String, ?> properties = createDictionary(context == null ?
                propertyEntries.subList(0, 4).toArray() : propertyEntries.toArray());

        m_context.registerService(TestResource.class.getName(), new TestResource(), properties);
    }

    private void registerErrorPage(String name, List<String> errors)
    {
        registerErrorPage(name, errors, null);
    }

    private void registerErrorPage(String name, List<String> errors, String context)
    {
        List<Object> propertyEntries = Arrays.<Object>asList(
                HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_ERROR_PAGE, errors,
                HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, name,
                HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, context);

        Dictionary<String, ?> properties = createDictionary(context == null ?
                propertyEntries.subList(0, 4).toArray() : propertyEntries.toArray());

        m_context.registerService(Servlet.class.getName(), new TestServlet(), properties);
    }

    private void registerListener(Class<?> listenerClass, boolean useWithWhiteboard)
    {
        registerListener(listenerClass, useWithWhiteboard, null);
    }

    private void registerListener(Class<?> listenerClass, boolean useWithWhiteboard, String context)
    {
        List<Object> propertyEntries = Arrays.<Object>asList(
                HttpWhiteboardConstants.HTTP_WHITEBOARD_LISTENER, useWithWhiteboard ? "true" : "false",
                HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, context);

        Dictionary<String, ?> properties = createDictionary(context == null ?
                propertyEntries.subList(0, 2).toArray() : propertyEntries.toArray());

        m_context.registerService(listenerClass.getName(), mock(listenerClass), properties);
    }

    private ServiceRegistration<?> registerContext(String name, String path)
    {
        Dictionary<String, ?> properties = createDictionary(
                HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, name,
                HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, path);

        return m_context.registerService(ServletContextHelper.class.getName(), mock(ServletContextHelper.class), properties);
    }

    @Test
    public void httpRuntimeServiceIsAvailableAfterBundleActivation() throws Exception
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

        // TODO The default context should have a negative service Id
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
    public void dtosForSuccesfullyRegisteredServlets() throws Exception
    {
        //register first servlet
        registerServlet("testServlet 1", "/servlet_1");

        awaitServices(Servlet.class.getName(), 2); // Felix web console also registers a servlet

        HttpServiceRuntime serviceRuntime = (HttpServiceRuntime) getService(HttpServiceRuntime.class.getName());
        assertNotNull("HttpServiceRuntime unavailable", serviceRuntime);

        RuntimeDTO runtimeDTOWithFirstSerlvet = serviceRuntime.getRuntimeDTO();

        assertEquals(0, runtimeDTOWithFirstSerlvet.failedServletDTOs.length);
        assertEquals(1, runtimeDTOWithFirstSerlvet.servletContextDTOs.length);

        ServletContextDTO contextDTO = runtimeDTOWithFirstSerlvet.servletContextDTOs[0];
        assertEquals(1, contextDTO.servletDTOs.length);
        assertEquals("testServlet 1", contextDTO.servletDTOs[0].name);

        //register second servlet
        registerServlet("testServlet 2", "/servlet_2");
        awaitServices(Servlet.class.getName(), 3);

        RuntimeDTO runtimeDTOWithBothSerlvets = serviceRuntime.getRuntimeDTO();

        assertEquals(0, runtimeDTOWithBothSerlvets.failedServletDTOs.length);
        assertEquals(1, runtimeDTOWithBothSerlvets.servletContextDTOs.length);

        contextDTO = runtimeDTOWithBothSerlvets.servletContextDTOs[0];
        assertEquals(2, contextDTO.servletDTOs.length);
        assertEquals("testServlet 1", contextDTO.servletDTOs[0].name);
        assertEquals("testServlet 2", contextDTO.servletDTOs[1].name);
    }

    @Test
    public void dtosForSuccesfullyRegisteredFilters() throws Exception
    {
        //register first filter
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

        //register second filter
        registerFilter("testFilter 2", "/servlet_1");
        awaitServices(Filter.class.getName(), 2);

        RuntimeDTO runtimeDTOWithBothFilters = serviceRuntime.getRuntimeDTO();

        assertEquals(0, runtimeDTOWithBothFilters.failedFilterDTOs.length);
        assertEquals(1, runtimeDTOWithBothFilters.servletContextDTOs.length);

        contextDTO = runtimeDTOWithBothFilters.servletContextDTOs[0];
        assertEquals(2, contextDTO.filterDTOs.length);
        assertEquals("testFilter 1", contextDTO.filterDTOs[0].name);
        assertEquals("testFilter 2", contextDTO.filterDTOs[1].name);
    }

    @Test
    public void dtosForSuccesfullyRegisteredResources() throws Exception
    {
        // register first resource service
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

        // register second resource service
        registerResource("/resources", "/resource_2/*");
        awaitServices(TestResource.class.getName(), 2);

        RuntimeDTO runtimeDTOWithBothResources = serviceRuntime.getRuntimeDTO();

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
    public void dtosForSuccesfullyRegisteredErrorPages() throws Exception
    {
        // register first error page
        registerErrorPage("error page 1", asList("404", NoSuchElementException.class.getName()));
        awaitServices(Servlet.class.getName(), 2);

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

        // register second error page
        registerErrorPage("error page 2", asList("500", ServletException.class.getName()));
        awaitServices(Servlet.class.getName(), 3);

        RuntimeDTO runtimeDTOWithBothErrorPages = serviceRuntime.getRuntimeDTO();

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
    public void dtosForSuccesfullyRegisteredListeners() throws Exception
    {
        // register a servlet context listenere as first listener
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

        // register all other listener types
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

        assertEquals(0, runtimeDTOWithAllListeners.failedListenerDTOs.length);
        assertEquals(1, runtimeDTOWithAllListeners.servletContextDTOs.length);

        contextDTO = runtimeDTOWithAllListeners.servletContextDTOs[0];
        // TODO
        assertEquals(5, contextDTO.listenerDTOs.length);
//        assertEquals(ServletContextListener.class.getName(), contextDTO.listenerDTOs[0].types[0]);
        assertEquals(ServletContextAttributeListener.class.getName(), contextDTO.listenerDTOs[0].types[0]);
        assertEquals(ServletRequestListener.class.getName(), contextDTO.listenerDTOs[1].types[0]);
        assertEquals(ServletRequestAttributeListener.class.getName(), contextDTO.listenerDTOs[2].types[0]);
        assertEquals(HttpSessionListener.class.getName(), contextDTO.listenerDTOs[3].types[0]);
        assertEquals(HttpSessionAttributeListener.class.getName(), contextDTO.listenerDTOs[4].types[0]);
    }

    @Test
    public void dtosForSuccesfullyRegisteredContexts() throws Exception
    {
        // register first additional context
        registerContext("contextA", "/contextA");
        awaitServices(ServletContextHelper.class.getName(), 2);

        HttpServiceRuntime serviceRuntime = (HttpServiceRuntime) getService(HttpServiceRuntime.class.getName());
        assertNotNull("HttpServiceRuntime unavailable", serviceRuntime);

        RuntimeDTO runtimeDTOWithAdditionalContext = serviceRuntime.getRuntimeDTO();

        assertEquals(0, runtimeDTOWithAdditionalContext.failedServletContextDTOs.length);
        assertEquals(2, runtimeDTOWithAdditionalContext.servletContextDTOs.length);

        // default context is last, as it has the lowest service ranking
        assertEquals("contextA", runtimeDTOWithAdditionalContext.servletContextDTOs[0].name);
        assertEquals("/contextA", runtimeDTOWithAdditionalContext.servletContextDTOs[0].contextPath);
        assertEquals("default", runtimeDTOWithAdditionalContext.servletContextDTOs[1].name);
        assertEquals("", runtimeDTOWithAdditionalContext.servletContextDTOs[1].contextPath);

        // register second additional context
        registerContext("contextB", "/contextB");
        awaitServices(ServletContextHelper.class.getName(), 3);

        RuntimeDTO runtimeDTOWithAllContexts = serviceRuntime.getRuntimeDTO();

        assertEquals(0, runtimeDTOWithAllContexts.failedServletContextDTOs.length);
        assertEquals(3, runtimeDTOWithAllContexts.servletContextDTOs.length);

        // default context is last, as it has the lowest service ranking
        assertEquals("contextA", runtimeDTOWithAllContexts.servletContextDTOs[0].name);
        assertEquals("/contextA", runtimeDTOWithAllContexts.servletContextDTOs[0].contextPath);
        assertEquals("contextB", runtimeDTOWithAllContexts.servletContextDTOs[1].name);
        assertEquals("/contextB", runtimeDTOWithAllContexts.servletContextDTOs[1].contextPath);
        assertEquals("default", runtimeDTOWithAllContexts.servletContextDTOs[2].name);
        assertEquals("", runtimeDTOWithAllContexts.servletContextDTOs[2].contextPath);
    }

    @Test
    public void successfulSetup()
    {
        registerContext("test-context", "/test-context");

        registerServlet("default servlet", "/default");
        registerFilter("default filter", "/default");
        registerErrorPage("default error page", asList(Exception.class.getName()));
        registerResource("/", "/default/resource");
        registerListener(ServletRequestListener.class, true);

        registerServlet("context servlet", "/default", "(" + HTTP_WHITEBOARD_CONTEXT_NAME + "=test-context)");
        registerFilter("context filter", "/default", "(" + HTTP_WHITEBOARD_CONTEXT_NAME + "=test-context)");
        registerErrorPage("context error page", asList("500"), "(" + HTTP_WHITEBOARD_CONTEXT_NAME + "=test-context)");
        registerResource("/", "/test-contextd/resource", "(" + HTTP_WHITEBOARD_CONTEXT_NAME + "=test-context)");
        registerListener(ServletRequestListener.class, true, "(" + HTTP_WHITEBOARD_CONTEXT_NAME + "=test-context)");

        awaitServices(Servlet.class.getName(), 5); // Felix web console also registers a servlet
        awaitServices(Filter.class.getName(), 2);
        awaitServices(TestResource.class.getName(), 2);
        awaitServices(ServletRequestListener.class.getName(), 2);

        HttpServiceRuntime serviceRuntime = (HttpServiceRuntime) getService(HttpServiceRuntime.class.getName());
        assertNotNull("HttpServiceRuntime unavailable", serviceRuntime);

        RuntimeDTO runtimeDTO = serviceRuntime.getRuntimeDTO();

        assertEquals(0, runtimeDTO.failedErrorPageDTOs.length);
        assertEquals(0, runtimeDTO.failedFilterDTOs.length);
        assertEquals(0, runtimeDTO.failedListenerDTOs.length);
        assertEquals(0, runtimeDTO.failedResourceDTOs.length);
        assertEquals(0, runtimeDTO.failedServletContextDTOs.length);
        assertEquals(0, runtimeDTO.failedServletDTOs.length);

        assertEquals(2, runtimeDTO.servletContextDTOs.length);
        assertEquals("test-context", runtimeDTO.servletContextDTOs[0].name);
        assertEquals("default", runtimeDTO.servletContextDTOs[1].name);

        ServletContextDTO defaultContextDTO = runtimeDTO.servletContextDTOs[1];
        long contextServiceId = defaultContextDTO.serviceId;

        assertEquals(1, defaultContextDTO.servletDTOs.length);
        assertEquals("default servlet", defaultContextDTO.servletDTOs[0].name);
        assertEquals(contextServiceId, defaultContextDTO.servletDTOs[0].servletContextId);
        assertEquals(1, defaultContextDTO.filterDTOs.length);
        assertEquals("default filter", defaultContextDTO.filterDTOs[0].name);
        assertEquals(contextServiceId, defaultContextDTO.filterDTOs[0].servletContextId);
        assertEquals(1, defaultContextDTO.errorPageDTOs.length);
        assertEquals(Exception.class.getName(), defaultContextDTO.errorPageDTOs[0].exceptions[0]);
        assertEquals(contextServiceId, defaultContextDTO.errorPageDTOs[0].servletContextId);
        assertEquals(1, defaultContextDTO.listenerDTOs.length);
        assertEquals(ServletRequestListener.class.getName(), defaultContextDTO.listenerDTOs[0].types[0]);
        assertEquals(contextServiceId, defaultContextDTO.listenerDTOs[0].servletContextId);

        ServletContextDTO testContextDTO = runtimeDTO.servletContextDTOs[0];
        contextServiceId = testContextDTO.serviceId;

        assertEquals(1, testContextDTO.servletDTOs.length);
        assertEquals("context servlet", testContextDTO.servletDTOs[0].name);
        assertEquals(contextServiceId, testContextDTO.servletDTOs[0].servletContextId);
        assertEquals(1, testContextDTO.filterDTOs.length);
        assertEquals("context filter", testContextDTO.filterDTOs[0].name);
        assertEquals(contextServiceId, testContextDTO.filterDTOs[0].servletContextId);
        assertEquals(1, testContextDTO.errorPageDTOs.length);
        assertEquals(500L, testContextDTO.errorPageDTOs[0].errorCodes[0]);
        assertEquals(contextServiceId, testContextDTO.errorPageDTOs[0].servletContextId);
        assertEquals(1, testContextDTO.listenerDTOs.length);
        assertEquals(ServletRequestListener.class.getName(), testContextDTO.listenerDTOs[0].types[0]);
        assertEquals(contextServiceId, testContextDTO.listenerDTOs[0].servletContextId);
    }

    // As specified in OSGi Compendium Release 6, Chapter 140.1 (TODO : exact version)
    @Test
    public void hiddenDefaultContextAppearsAsFailure()
    {
        registerContext("default", "");
        awaitServices(ServletContextHelper.class.getName(), 2);

        HttpServiceRuntime serviceRuntime = (HttpServiceRuntime) getService(HttpServiceRuntime.class.getName());
        assertNotNull("HttpServiceRuntime unavailable", serviceRuntime);

        RuntimeDTO runtimeDTO = serviceRuntime.getRuntimeDTO();
        assertEquals(1, runtimeDTO.failedServletContextDTOs.length);
        assertEquals("default", runtimeDTO.failedServletContextDTOs[0].name);
        assertEquals(1, runtimeDTO.servletContextDTOs.length);
        assertEquals("default", runtimeDTO.servletContextDTOs[0].name);
    }

    // As specified in OSGi Compendium Release 6, Chapter 140.1
    @Test
    public void contextHelperWithDuplicateNameAppearsAsFailure()
    {
        ServiceRegistration<?> firstContextReg = registerContext("contextA", "/first");
        registerContext("contextA", "/second");
        awaitServices(ServletContextHelper.class.getName(), 3);

        HttpServiceRuntime serviceRuntime = (HttpServiceRuntime) getService(HttpServiceRuntime.class.getName());
        assertNotNull("HttpServiceRuntime unavailable", serviceRuntime);

        RuntimeDTO runtimeDTO = serviceRuntime.getRuntimeDTO();

        assertEquals(1, runtimeDTO.failedServletContextDTOs.length);
        assertEquals("contextA", runtimeDTO.failedServletContextDTOs[0].name);
        assertEquals("/second", runtimeDTO.failedServletContextDTOs[0].contextPath);
        assertEquals(FAILURE_REASON_SHADOWED_BY_OTHER_SERVICE, runtimeDTO.failedServletContextDTOs[0].failureReason);

        assertEquals(2, runtimeDTO.servletContextDTOs.length);
        assertEquals("default", runtimeDTO.servletContextDTOs[1].name);

        assertEquals("contextA", runtimeDTO.servletContextDTOs[0].name);
        assertEquals("/first", runtimeDTO.servletContextDTOs[0].contextPath);

        firstContextReg.unregister();
        awaitServices(ServletContextHelper.class.getName(), 2);

        runtimeDTO = serviceRuntime.getRuntimeDTO();

        assertEquals(0, runtimeDTO.failedServletContextDTOs.length);

        assertEquals(2, runtimeDTO.servletContextDTOs.length);
        assertEquals("default", runtimeDTO.servletContextDTOs[1].name);

        assertEquals("contextA", runtimeDTO.servletContextDTOs[0].name);
        assertEquals("/second", runtimeDTO.servletContextDTOs[0].contextPath);
    }

    // As specified in OSGi Compendium Release 6, Chapter 140.1
    @Test
    public void missingContextHelperNameAppearsAsFailure()
    {
        Dictionary<String, ?> properties = createDictionary(
                HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "");

        m_context.registerService(ServletContextHelper.class.getName(), mock(ServletContextHelper.class), properties);
        awaitServices(ServletContextHelper.class.getName(), 2);

        HttpServiceRuntime serviceRuntime = (HttpServiceRuntime) getService(HttpServiceRuntime.class.getName());
        assertNotNull("HttpServiceRuntime unavailable", serviceRuntime);

        RuntimeDTO runtimeDTO = serviceRuntime.getRuntimeDTO();

        assertEquals(1, runtimeDTO.failedServletContextDTOs.length);
        assertEquals(null, runtimeDTO.failedServletContextDTOs[0].name);
        assertEquals(FAILURE_REASON_VALIDATION_FAILED, runtimeDTO.failedServletContextDTOs[0].failureReason);
    }

    // As specified in OSGi Compendium Release 6, Chapter 140.1
    @Test
    public void invalidContextHelperNameAppearsAsFailure()
    {
        registerContext("context A", "");
        awaitServices(ServletContextHelper.class.getName(), 2);

        HttpServiceRuntime serviceRuntime = (HttpServiceRuntime) getService(HttpServiceRuntime.class.getName());
        assertNotNull("HttpServiceRuntime unavailable", serviceRuntime);

        RuntimeDTO runtimeDTO = serviceRuntime.getRuntimeDTO();

        assertEquals(1, runtimeDTO.failedServletContextDTOs.length);
        assertEquals("context A", runtimeDTO.failedServletContextDTOs[0].name);
        assertEquals(FAILURE_REASON_VALIDATION_FAILED, runtimeDTO.failedServletContextDTOs[0].failureReason);
    }

    // As specified in OSGi Compendium Release 6, Chapter 140.1
    @Test
    public void invalidContextHelperPathAppearsAsFailure()
    {
        registerContext("contextA", "#");
        awaitServices(ServletContextHelper.class.getName(), 2);

        HttpServiceRuntime serviceRuntime = (HttpServiceRuntime) getService(HttpServiceRuntime.class.getName());
        assertNotNull("HttpServiceRuntime unavailable", serviceRuntime);

        RuntimeDTO runtimeDTO = serviceRuntime.getRuntimeDTO();

        assertEquals(1, runtimeDTO.failedServletContextDTOs.length);
        assertEquals("#", runtimeDTO.failedServletContextDTOs[0].contextPath);
        assertEquals(FAILURE_REASON_VALIDATION_FAILED, runtimeDTO.failedServletContextDTOs[0].failureReason);
    }

    // As specified in OSGi Compendium Release 6, Chapter 140.3
    @Test
    public void selectionOfNonExistingContextHelperAppearsAsFailure()
    {
        registerServlet("servlet 1", "/", "(" + HTTP_WHITEBOARD_CONTEXT_NAME + "=contextA)");

        HttpServiceRuntime serviceRuntime = (HttpServiceRuntime) getService(HttpServiceRuntime.class.getName());
        assertNotNull("HttpServiceRuntime unavailable", serviceRuntime);

        RuntimeDTO runtimeDTO = serviceRuntime.getRuntimeDTO();

        assertEquals(1, runtimeDTO.failedServletDTOs.length);
        assertEquals("servlet 1", runtimeDTO.failedServletDTOs[0].name);
        assertEquals(FAILURE_REASON_NO_SERVLET_CONTEXT_MATCHING, runtimeDTO.failedServletDTOs[0].failureReason);

        registerContext("contextA", "/contextA");

        runtimeDTO = serviceRuntime.getRuntimeDTO();

        assertEquals(0, runtimeDTO.failedServletDTOs.length);
        assertEquals(2, runtimeDTO.servletContextDTOs.length);
        assertEquals("contextA", runtimeDTO.servletContextDTOs[0].name);
        assertEquals(1, runtimeDTO.servletContextDTOs[0].servletDTOs.length);
        assertEquals("servlet 1", runtimeDTO.servletContextDTOs[0].servletDTOs[0].name);
    }

    // As specified in OSGi Compendium Release 6, Chapter 140.3
    @Test
    public void differentTargetIsIgnored()
    {
        Dictionary<String, ?> properties = createDictionary(
                HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/servlet",
                HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "servlet",
                HttpWhiteboardConstants.HTTP_WHITEBOARD_TARGET, "(org.osgi.service.http.port=8282)");

        m_context.registerService(Servlet.class.getName(), new TestServlet(), properties);

        HttpServiceRuntime serviceRuntime = (HttpServiceRuntime) getService(HttpServiceRuntime.class.getName());
        assertNotNull("HttpServiceRuntime unavailable", serviceRuntime);

        RuntimeDTO runtimeDTO = serviceRuntime.getRuntimeDTO();
        assertEquals(1, runtimeDTO.servletContextDTOs.length);

        assertEquals(0, runtimeDTO.failedServletDTOs.length);
        assertEquals(0, runtimeDTO.servletContextDTOs[0].servletDTOs.length);
    }

    // As specified in OSGi Compendium Release 6, Chapter 140.4
    @Test
    public void servletWithoutNameGetsFullyQualifiedName()
    {
        Dictionary<String, ?> properties = createDictionary(
                HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/servlet");

        m_context.registerService(Servlet.class.getName(), new TestServlet(), properties);

        HttpServiceRuntime serviceRuntime = (HttpServiceRuntime) getService(HttpServiceRuntime.class.getName());
        assertNotNull("HttpServiceRuntime unavailable", serviceRuntime);

        RuntimeDTO runtimeDTO = serviceRuntime.getRuntimeDTO();
        assertEquals(1, runtimeDTO.servletContextDTOs.length);

        assertEquals(0, runtimeDTO.failedServletDTOs.length);
        assertEquals(1, runtimeDTO.servletContextDTOs[0].servletDTOs.length);
        assertEquals(TestServlet.class.getName(), runtimeDTO.servletContextDTOs[0].servletDTOs[0].name);
    }

    // As specified in OSGi Compendium Release 6, Chapter 140.4
    @Test
    @Ignore
    public void mulitpleServletsWithSamePatternChoosenByServiceRankingRules() throws Exception
    {
        // TODO
    }

    // As specified in OSGi Compendium Release 6, Chapter 140.4.1
    @Test
    public void patternAndErrorPageSpecifiedInvalidAndAppearsAsFailure()
    {
        Dictionary<String, ?> properties = createDictionary(
                HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/servlet",
                HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "servlet",
                HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_ERROR_PAGE, asList("400"));

        m_context.registerService(Servlet.class.getName(), new TestServlet(), properties);

        awaitServices(Servlet.class.getName(), 2); // Felix web console also registers a servlet

        HttpServiceRuntime serviceRuntime = (HttpServiceRuntime) getService(HttpServiceRuntime.class.getName());
        assertNotNull("HttpServiceRuntime unavailable", serviceRuntime);

        RuntimeDTO runtimeDTO = serviceRuntime.getRuntimeDTO();
        assertEquals(1, runtimeDTO.servletContextDTOs.length);
        assertEquals(0, runtimeDTO.servletContextDTOs[0].servletDTOs.length);
        assertEquals(0, runtimeDTO.servletContextDTOs[0].errorPageDTOs.length);

        assertEquals(0, runtimeDTO.failedServletDTOs.length);
        assertEquals(1, runtimeDTO.failedErrorPageDTOs.length);
        assertEquals("servlet", runtimeDTO.failedErrorPageDTOs[0].name);
        assertEquals(FAILURE_REASON_VALIDATION_FAILED, runtimeDTO.failedErrorPageDTOs[0].failureReason);
    }

    // As specified in OSGi Compendium Release 6, Chapter 140.4.1
    @Test
    public void multipleServletsForSamePatternChoosenByServiceRankingRules()
    {
        registerServlet("servlet 1", "/pathcollision");

        awaitServices(Servlet.class.getName(), 2);

        HttpServiceRuntime serviceRuntime = (HttpServiceRuntime) getService(HttpServiceRuntime.class.getName());
        assertNotNull("HttpServiceRuntime unavailable", serviceRuntime);

        RuntimeDTO runtimeDTO = serviceRuntime.getRuntimeDTO();

        assertEquals(1, runtimeDTO.servletContextDTOs.length);
        assertEquals("default", runtimeDTO.servletContextDTOs[0].name);
        ServletContextDTO defaultContext = runtimeDTO.servletContextDTOs[0];

        assertEquals(0, runtimeDTO.failedServletDTOs.length);
        assertEquals(1, defaultContext.servletDTOs.length);

        Dictionary<String, ?> properties = createDictionary(
                HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/pathcollision",
                HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "servlet 2",
                Constants.SERVICE_RANKING, Integer.MAX_VALUE);

        ServiceRegistration<?> higherRankingServlet = m_context.registerService(Servlet.class.getName(), new TestServlet(), properties);

        awaitServices(Servlet.class.getName(), 3);

        RuntimeDTO runtimeWithShadowedServlet = serviceRuntime.getRuntimeDTO();

        assertEquals(1, runtimeWithShadowedServlet.servletContextDTOs.length);
        assertEquals("default", runtimeWithShadowedServlet.servletContextDTOs[0].name);
        defaultContext = runtimeWithShadowedServlet.servletContextDTOs[0];

        assertEquals(1, defaultContext.servletDTOs.length);

        assertEquals(1, runtimeWithShadowedServlet.failedServletDTOs.length);
        FailedServletDTO failedServletDTO = runtimeWithShadowedServlet.failedServletDTOs[0];
        assertEquals("servlet 1", failedServletDTO.name);
        assertEquals(FAILURE_REASON_SHADOWED_BY_OTHER_SERVICE, failedServletDTO.failureReason);

        higherRankingServlet.unregister();
        awaitServices(Servlet.class.getName(), 2);

        runtimeDTO = serviceRuntime.getRuntimeDTO();

        assertEquals(1, runtimeDTO.servletContextDTOs.length);
        assertEquals("default", runtimeDTO.servletContextDTOs[0].name);
        defaultContext = runtimeDTO.servletContextDTOs[0];

        assertEquals(0, runtimeDTO.failedServletDTOs.length);
        assertEquals(1, defaultContext.servletDTOs.length);
        assertEquals("servlet 1", defaultContext.servletDTOs[0].name);
    }

    // As specified in OSGi Compendium Release 6, Chapter 140.4.1
    @Test
    @Ignore
    public void multipleErrorPagesForSameExceptionsChoosenByServiceRankingRules()
    {
        // TODO
    }

    // As specified in OSGi Compendium Release 6, Chapter 140.4
    @Test
    @Ignore
    public void mulitpleServletsWithSamePatternHttpServiceRegistrationWins()
    {
        // TODO
    }

    // As specified in OSGi Compendium Release 6, Chapter 140.7
    @Test
    public void invalidListenerPopertyValueAppearsAsFailure() throws Exception
    {
        Dictionary<String, ?> properties = createDictionary(
                HttpWhiteboardConstants.HTTP_WHITEBOARD_LISTENER, "invalid");

        m_context.registerService(ServletRequestListener.class.getName(), mock(ServletRequestListener.class), properties);

        awaitService(ServletRequestListener.class.getName());

        HttpServiceRuntime serviceRuntime = (HttpServiceRuntime) getService(HttpServiceRuntime.class.getName());
        assertNotNull("HttpServiceRuntime unavailable", serviceRuntime);

        RuntimeDTO runtimeDTO = serviceRuntime.getRuntimeDTO();

        assertEquals(1, runtimeDTO.failedListenerDTOs.length);
        assertEquals(FAILURE_REASON_VALIDATION_FAILED, runtimeDTO.failedListenerDTOs[0].failureReason);
    }

    // As specified in OSGi Compendium Release 6, Chapter 140.8
    @Test
    public void contextReplacedWithHigherRankingContext() throws Exception
    {
        ServiceRegistration<?> firstContext = registerContext("test-context", "/first");
        Long firstContextId = (Long) firstContext.getReference().getProperty(Constants.SERVICE_ID);

        registerServlet("servlet", "/servlet", "(" + HTTP_WHITEBOARD_CONTEXT_NAME + "=test-context)");

        awaitServices(ServletContextHelper.class.getName(), 2);

        HttpServiceRuntime serviceRuntime = (HttpServiceRuntime) getService(HttpServiceRuntime.class.getName());
        assertNotNull("HttpServiceRuntime unavailable", serviceRuntime);

        RuntimeDTO runtimeDTO = serviceRuntime.getRuntimeDTO();

        assertEquals(0, runtimeDTO.failedServletContextDTOs.length);
        assertEquals(2, runtimeDTO.servletContextDTOs.length);
        assertEquals(firstContextId.longValue(), runtimeDTO.servletContextDTOs[0].serviceId);
        assertEquals("test-context", runtimeDTO.servletContextDTOs[0].name);
        assertEquals("/first", runtimeDTO.servletContextDTOs[0].contextPath);
        assertEquals("default", runtimeDTO.servletContextDTOs[1].name);

        assertEquals(1, runtimeDTO.servletContextDTOs[0].servletDTOs.length);
        assertEquals("servlet", runtimeDTO.servletContextDTOs[0].servletDTOs[0].name);

        Dictionary<String, ?> properties = createDictionary(
                HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "test-context",
                HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/second",
                Constants.SERVICE_RANKING, Integer.MAX_VALUE);

        ServiceRegistration<?> secondContext = m_context.registerService(ServletContextHelper.class.getName(), mock(ServletContextHelper.class), properties);
        Long secondContextId = (Long) secondContext.getReference().getProperty(Constants.SERVICE_ID);

        awaitServices(ServletContextHelper.class.getName(), 3);

        runtimeDTO = serviceRuntime.getRuntimeDTO();

        assertEquals(1, runtimeDTO.failedServletContextDTOs.length);
        assertEquals(firstContextId.longValue(), runtimeDTO.failedServletContextDTOs[0].serviceId);
        assertEquals("test-context", runtimeDTO.failedServletContextDTOs[0].name);
        assertEquals("/first", runtimeDTO.failedServletContextDTOs[0].contextPath);

        assertEquals(2, runtimeDTO.servletContextDTOs.length);

        assertEquals(secondContextId.longValue(), runtimeDTO.servletContextDTOs[0].serviceId);
        assertEquals("test-context", runtimeDTO.servletContextDTOs[0].name);
        assertEquals("/second", runtimeDTO.servletContextDTOs[0].contextPath);
        assertEquals("default", runtimeDTO.servletContextDTOs[1].name);

        assertEquals(1, runtimeDTO.servletContextDTOs[0].servletDTOs.length);
        assertEquals("servlet", runtimeDTO.servletContextDTOs[0].servletDTOs[0].name);

        secondContext.unregister();
        awaitServices(ServletContextHelper.class.getName(), 2);

        runtimeDTO = serviceRuntime.getRuntimeDTO();

        assertEquals(0, runtimeDTO.failedServletContextDTOs.length);
        assertEquals(2, runtimeDTO.servletContextDTOs.length);
        assertEquals(firstContextId.longValue(), runtimeDTO.servletContextDTOs[0].serviceId);
        assertEquals("test-context", runtimeDTO.servletContextDTOs[0].name);
        assertEquals("/first", runtimeDTO.servletContextDTOs[0].contextPath);
        assertEquals("default", runtimeDTO.servletContextDTOs[1].name);

        assertEquals(1, runtimeDTO.servletContextDTOs[0].servletDTOs.length);
        assertEquals("servlet", runtimeDTO.servletContextDTOs[0].servletDTOs[0].name);
    }

    // As specified in OSGi Compendium Release 6, Chapter 140.9
    @Test
    public void httServiceIdIsSet()
    {
        ServiceReference<?> httpServiceRef = m_context.getServiceReference(HttpService.class.getName());
        ServiceReference<?> httpServiceRuntimeRef = m_context.getServiceReference(HttpServiceRuntime.class.getName());

        Long expectedId = (Long) httpServiceRef.getProperty(Constants.SERVICE_ID);
        Long actualId = (Long) httpServiceRuntimeRef.getProperty(HttpServiceRuntimeConstants.HTTP_SERVICE_ID_ATTRIBUTE);

        assertEquals(expectedId, actualId);
    }

    // As specified in OSGi Compendium Release 6, Chapter 140.9
    @Test
    @Ignore // This is still broken
    public void serviceRegisteredWithHttServiceHasNegativeServiceId() throws Exception
    {
        register("/test", new TestServlet());

        HttpServiceRuntime serviceRuntime = (HttpServiceRuntime) getService(HttpServiceRuntime.class.getName());
        assertNotNull("HttpServiceRuntime unavailable", serviceRuntime);

        RuntimeDTO runtimeDTO = serviceRuntime.getRuntimeDTO();

        assertEquals(1, runtimeDTO.servletContextDTOs.length);
        assertEquals(1, runtimeDTO.servletContextDTOs[0].servletDTOs.length);
        assertTrue(0 > runtimeDTO.servletContextDTOs[0].servletDTOs[0].serviceId);
    }

    // As specified in OSGi Compendium Release 6, Chapter 140.9
    @Test
    public void serviceWithoutRequiredPropertiesIsIgnored()
    {
        Dictionary<String, ?> properties = createDictionary(
                // Neither pattern nor error page specified
                HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "servlet");

        m_context.registerService(Servlet.class.getName(), new TestServlet(), properties);

        HttpServiceRuntime serviceRuntime = (HttpServiceRuntime) getService(HttpServiceRuntime.class.getName());
        assertNotNull("HttpServiceRuntime unavailable", serviceRuntime);

        RuntimeDTO runtimeDTO = serviceRuntime.getRuntimeDTO();

        assertEquals(0, runtimeDTO.failedServletContextDTOs.length);
        assertEquals(1, runtimeDTO.servletContextDTOs.length);
        assertEquals(0, runtimeDTO.servletContextDTOs[0].servletDTOs.length);
    }

    @Test
    public void dtosAreIndependentCopies() throws Exception
    {
        //register first servlet
        Dictionary<String, ?> properties = createDictionary(
                HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/test",
                HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "servlet 1",
                HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_INIT_PARAM_PREFIX + "test", "testValue");

        m_context.registerService(Servlet.class.getName(), new TestServlet(), properties);
        awaitServices(Servlet.class.getName(), 2);

        HttpServiceRuntime serviceRuntime = (HttpServiceRuntime) getService(HttpServiceRuntime.class.getName());
        assertNotNull("HttpServiceRuntime unavailable", serviceRuntime);

        RuntimeDTO runtimeDTOWithFirstSerlvet = serviceRuntime.getRuntimeDTO();

        //register second servlet
        registerServlet("testServlet 2", "/servlet_2");
        awaitServices(Servlet.class.getName(), 3);

        RuntimeDTO runtimeDTOWithTwoSerlvets = serviceRuntime.getRuntimeDTO();

        assertNotSame(runtimeDTOWithFirstSerlvet, runtimeDTOWithTwoSerlvets);

        assertNotSame(runtimeDTOWithFirstSerlvet.servletContextDTOs[0].servletDTOs[0].patterns,
                runtimeDTOWithTwoSerlvets.servletContextDTOs[0].servletDTOs[0].patterns);

        boolean mapsModifiable = true;
        try
        {
            runtimeDTOWithTwoSerlvets.servletContextDTOs[0].servletDTOs[0].initParams.clear();
        } catch (UnsupportedOperationException e)
        {
            mapsModifiable = false;
        }

        if (mapsModifiable)
        {
            assertNotSame(runtimeDTOWithFirstSerlvet.servletContextDTOs[0].servletDTOs[0].initParams,
                    runtimeDTOWithTwoSerlvets.servletContextDTOs[0].servletDTOs[0].initParams);
        }
    }

    private void awaitServices(String serviceName, int count)
    {
        long elapsed = 0;
        while(getServiceReferences(serviceName).length != count)
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
