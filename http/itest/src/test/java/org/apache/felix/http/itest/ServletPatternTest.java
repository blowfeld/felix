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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.osgi.framework.Constants.SERVICE_RANKING;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PATTERN;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PREFIX;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.http.itest.HttpServiceRuntimeTest.TestResource;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.spi.reactors.EagerSingleStagedReactorFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.context.ServletContextHelper;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy( EagerSingleStagedReactorFactory.class )
public class ServletPatternTest extends BaseIntegrationTest
{
    private List<ServiceRegistration<?>> registrations = new ArrayList<ServiceRegistration<?>>();

    private CountDownLatch initLatch;
    private CountDownLatch destroyLatch;

    public void setupLatches(int count)
    {
        initLatch = new CountDownLatch(count);
        destroyLatch = new CountDownLatch(count);
    }

    public void setupServlet(final String name, String[] path, int rank, String context) throws Exception
    {
        Dictionary<String, Object> servletProps = new Hashtable<String, Object>();
        servletProps.put(HTTP_WHITEBOARD_SERVLET_NAME, name);
        servletProps.put(HTTP_WHITEBOARD_SERVLET_PATTERN, path);
        servletProps.put(SERVICE_RANKING, rank);
        if (context != null)
        {
            servletProps.put(HTTP_WHITEBOARD_CONTEXT_SELECT, "(" + HTTP_WHITEBOARD_CONTEXT_NAME + "=" + context + ")");
        }

        TestServlet servletWithErrorCode = new TestServlet(initLatch, destroyLatch)
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws IOException
            {
                resp.getWriter().print(name);
                resp.flushBuffer();
            }
        };

        registrations.add(m_context.registerService(Servlet.class.getName(), servletWithErrorCode, servletProps));
    }

    private void setupContext(String name, String path) throws InterruptedException
    {
        Dictionary<String, ?> properties = createDictionary(
                HTTP_WHITEBOARD_CONTEXT_NAME, name,
                HTTP_WHITEBOARD_CONTEXT_PATH, path);

        ServletContextHelper servletContextHelper = new ServletContextHelper(m_context.getBundle()){
            // test helper
        };
        registrations.add(m_context.registerService(ServletContextHelper.class.getName(), servletContextHelper, properties));

        // Wait for registration to finish
        Thread.sleep(500);
    }

    @After
    public void unregisterServices() throws InterruptedException
    {
        for (ServiceRegistration<?> serviceRegistration : registrations)
        {
            serviceRegistration.unregister();
        }

        assertLatch(destroyLatch);
    }

    @Test
    public void testHighRankReplaces() throws Exception
    {
        setupLatches(2);

        setupServlet("lowRankServlet", new String[] { "/foo", "/bar" }, 1, null);
        setupServlet("highRankServlet", new String[] { "/foo", "/baz" }, 2, null);

        assertLatch(initLatch);

        assertContent("highRankServlet", createURL("/foo"));
        assertContent("lowRankServlet", createURL("/bar"));
        assertContent("highRankServlet", createURL("/baz"));
    }

    @Test
    public void testHttpServiceReplaces() throws Exception
    {
        setupLatches(2);

        setupContext("contextFoo", "/test");
        setupServlet("whiteboardServlet", new String[]{ "/foo", "/bar" }, Integer.MAX_VALUE, "contextFoo");

        TestServlet httpServiceServlet = new TestServlet(initLatch, destroyLatch)
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException
            {
                resp.getWriter().print("httpServiceServlet");
                resp.flushBuffer();
            }
        };

        register("/test/foo", httpServiceServlet);

        try
        {
            assertTrue(initLatch.await(5, TimeUnit.SECONDS));

            assertContent("whiteboardServlet", createURL("/test/bar"));
            assertContent("httpServiceServlet", createURL("/test/foo"));
        }
        finally
        {
            unregister(httpServiceServlet);
        }
    }

    @Test
    public void testSameRankDoesNotReplace() throws Exception
    {
        setupLatches(2);

        setupServlet("servlet1", new String[]{ "/foo", "/bar" }, 2, null);
        setupServlet("servlet2", new String[]{ "/foo", "/baz" }, 2, null);

        assertLatch(initLatch);

        assertContent("servlet1", createURL("/foo"));
        assertContent("servlet1", createURL("/bar"));
        assertContent("servlet2", createURL("/baz"));
    }

    @Test
    public void testHighRankResourceReplaces() throws Exception
    {
        setupLatches(1);

        setupServlet("lowRankServlet", new String[]{ "/foo" }, 1, null);

        assertLatch(initLatch);
        assertContent("lowRankServlet", createURL("/foo"));

        Dictionary<String, Object> resourceProps = new Hashtable<String, Object>();
        String highRankPattern[] = { "/foo" };
        resourceProps.put(HTTP_WHITEBOARD_RESOURCE_PATTERN, highRankPattern);
        resourceProps.put(HTTP_WHITEBOARD_RESOURCE_PREFIX, "/resource/test.html");
        resourceProps.put(SERVICE_RANKING, 2);

        registrations.add(m_context.registerService(TestResource.class.getName(),
            new TestResource(), resourceProps));

        assertLatch(destroyLatch);

        assertContent(getTestHtmlContent(), createURL("/foo"));
    }

    private String getTestHtmlContent() throws IOException
    {
        InputStream resourceAsStream = this.getClass().getResourceAsStream("/resource/test.html");
        return slurpAsString(resourceAsStream);
    }

    @Test
    public void testContextWithLongerPrefixIsChosen() throws Exception
    {
        setupLatches(2);

        setupContext("contextFoo", "/foo");
        setupContext("contextBar", "/foo/bar");

        setupServlet("servlet1", new String[]{ "/bar/test" }, 1, "contextFoo");
        Thread.sleep(500);

        assertEquals(1, initLatch.getCount());
        assertContent("servlet1", createURL("/foo/bar/test"));

        setupServlet("servlet2", new String[]{ "/test" }, 1, "contextBar");

        assertLatch(initLatch);
        assertContent("servlet2", createURL("/foo/bar/test"));
    }

    @Test
    public void testContextWithLongerPrefixWithWildcardIsPreferedOverExactMatch() throws Exception
    {
        setupLatches(2);
        setupContext("contextFoo", "/foo");
        setupContext("contextBar", "/foo/bar");

        setupServlet("servlet1", new String[]{ "/bar/test/servlet" }, 1, "contextFoo");

        assertEquals(1, initLatch.getCount());
        assertContent("servlet1", createURL("/foo/bar/test/servlet"));

        setupServlet("servlet2", new String[]{ "/test/*" }, 1, "contextBar");

        assertLatch(initLatch);
        assertContent("servlet2", createURL("/foo/bar/test/servlet"));
    }

    @Test
    public void testContextWithLongerPrefixWithExtensionIsPreferedOverExactMatch() throws Exception
    {
        setupLatches(2);
        setupContext("contextFoo", "/foo");
        setupContext("contextBar", "/foo/bar");

        setupServlet("servlet1", new String[]{ "/bar/test/page.html" }, 1, "contextFoo");

        assertEquals(1, initLatch.getCount());
        assertContent("servlet1", createURL("/foo/bar/test/page.html"));

        setupServlet("servlet2", new String[]{ "*.html" }, 1, "contextBar");

        assertLatch(initLatch);
        assertContent("servlet2", createURL("/foo/bar/test/page.html"));
    }

    @Test
    public void testExtensionMatch() throws Exception
    {
        setupLatches(1);

        setupServlet("servlet1", new String[]{ "*.exe" }, 1, null);

        assertLatch(initLatch);
        assertContent("servlet1", createURL("/test.exe"));
    }

    @Test
    public void testExactMatchIsPreferedOverWildcardMatch() throws Exception
    {
        setupLatches(2);

        setupServlet("servlet1", new String[]{ "test" }, 1, null);
        setupServlet("servlet2", new String[]{ "test/*" }, 1, null);

        assertLatch(initLatch);
        assertContent("servlet1", createURL("/test"));
    }

    @Test
    public void testPrefixIsIgnoredAndExtensionMatchIsChoosen() throws Exception
    {
        setupLatches(2);

        setupServlet("servlet1", new String[]{ "catalog" }, 1, null);
        setupServlet("servlet2", new String[]{ "*.bop" }, 1, null);

        assertLatch(initLatch);
        assertContent("servlet2", createURL("/catalog/racecar.bop"));
    }

    @Test
    public void testPrefixIsIgnored() throws Exception
    {
        setupLatches(1);

        setupServlet("servlet1", new String[]{ "catalog" }, 1, null);

        assertLatch(initLatch);
        assertResponseCode(404, createURL("/catalog/index.html"));
    }

    @Test
    public void testFullPathSegmentMustMatch() throws Exception
    {
        setupLatches(1);

        setupServlet("servlet1", new String[]{ "foo/*" }, 1, null);

        assertLatch(initLatch);
        assertResponseCode(404, createURL("/foobar"));
    }

    @Test
    public void testFullPathSegmentMustMatchForContext() throws Exception
    {
        setupLatches(1);
        setupContext("contextFoo", "/foo");

        setupServlet("servlet1", new String[]{ "/*" }, 1, "contextFoo");

        assertLatch(initLatch);
        assertContent("servlet1", createURL("foo/bar"));
        assertResponseCode(404, createURL("/foobar"));
        assertResponseCode(404, createURL("/foob/ar"));
    }

    private void assertLatch(CountDownLatch latch) throws InterruptedException
    {
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        // Wait for registration to finish
        Thread.sleep(250);
    }
}
