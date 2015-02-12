/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.http.whiteboard.internal.manager;

import static org.mockito.Mockito.when;

import java.util.Dictionary;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import junit.framework.TestCase;

import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.DualHashBidiMap;
import org.apache.felix.http.api.ExtHttpService;
import org.apache.felix.http.whiteboard.HttpWhiteboardConstants;
import org.apache.felix.http.whiteboard.internal.manager.ExtenderManagerTest.ExtServlet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.NamespaceException;

@RunWith(MockitoJUnitRunner.class)
public class WhiteboardTargetTest 
{
	@Mock
	private Bundle bundle;
	
	@Mock
	private Servlet servlet1;
	@Mock
	private Servlet servlet2;
	@Mock
	private Servlet servlet3;
	
	@Mock
	private Filter filter1;
	@Mock 
	private Filter filter2;
	@Mock
	private Filter filter3;
	
	@Mock
	private ServiceReference servlet1Reference;
	@Mock
	private ServiceReference servlet2Reference;
	@Mock
	private ServiceReference servlet3Reference;
	
	@Mock
	private ServiceReference filter1Reference;
	@Mock
	private ServiceReference filter2Reference;
	@Mock
	private ServiceReference filter3Reference;
	
	@Mock
	private ServiceReference http1ServiceReference;
	
	@Mock
	private ServiceReference http2ServiceReference;
	
	private static final String SERVLET_1_ALIAS = "/servlet1";
	private static final String SERVLET_2_ALIAS = "/servlet2";
	private static final String SERVLET_3_ALIAS = "/servlet3";
	
	private static final String HTTP_PORT_PROPERTY ="org.osgi.service.http.port";
	
	private static final String SERVLET_1_TARGET = "(" + HTTP_PORT_PROPERTY + "=80" + ")";
	private static final String SERVLET_2_TARGET = "(" + HTTP_PORT_PROPERTY + "=8080" + ")";
	
	private static final String FILTER_1_TARGET = "(" + HTTP_PORT_PROPERTY + "=80" + ")";
	private static final String FILTER_2_TARGET = "(" + HTTP_PORT_PROPERTY + "=8080" + ")";

	private MockExtHttpService http1Service;
	private MockExtHttpService http2Service;
	
	@Before
	public void setup()
	{
		String[] httpPropKeys = {"org.osgi.service.http.port"};
		
		when(bundle.getBundleId()).thenReturn(1L);
		
		when(servlet1Reference.getBundle()).thenReturn(bundle);
		when(servlet1Reference.getPropertyKeys()).thenReturn(new String[0]);
		when(servlet1Reference.getProperty(HttpWhiteboardConstants.ALIAS)).thenReturn(SERVLET_1_ALIAS);
		when(servlet1Reference.getProperty(org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_TARGET)).thenReturn(SERVLET_1_TARGET);
		when(servlet1Reference.getProperty(Constants.SERVICE_ID)).thenReturn(1L);        		
		
		when(servlet2Reference.getBundle()).thenReturn(bundle);
		when(servlet2Reference.getPropertyKeys()).thenReturn(new String[0]);
		when(servlet2Reference.getProperty(HttpWhiteboardConstants.ALIAS)).thenReturn(SERVLET_2_ALIAS);
		when(servlet2Reference.getProperty(org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_TARGET)).thenReturn(SERVLET_2_TARGET);
		when(servlet2Reference.getProperty(Constants.SERVICE_ID)).thenReturn(2L);
		
		when(servlet3Reference.getBundle()).thenReturn(bundle);
		when(servlet3Reference.getPropertyKeys()).thenReturn(new String[0]);
		when(servlet3Reference.getProperty(HttpWhiteboardConstants.ALIAS)).thenReturn(SERVLET_3_ALIAS);
		when(servlet3Reference.getProperty(Constants.SERVICE_ID)).thenReturn(3L);
		
		when(http1ServiceReference.getBundle()).thenReturn(bundle);
		when(http1ServiceReference.getPropertyKeys()).thenReturn(httpPropKeys);
		when(http1ServiceReference.getProperty(HTTP_PORT_PROPERTY)).thenReturn(80);
		when(http1ServiceReference.getProperty(Constants.SERVICE_ID)).thenReturn(4L);
		
		when(http2ServiceReference.getBundle()).thenReturn(bundle);
		when(http2ServiceReference.getPropertyKeys()).thenReturn(httpPropKeys);
		when(http2ServiceReference.getProperty(HTTP_PORT_PROPERTY)).thenReturn(8080);
		when(http2ServiceReference.getProperty(Constants.SERVICE_ID)).thenReturn(5L);

		when(filter1Reference.getBundle()).thenReturn(bundle);
		when(filter1Reference.getPropertyKeys()).thenReturn(new String[0]);
		when(filter1Reference.getProperty(HttpWhiteboardConstants.PATTERN)).thenReturn(SERVLET_1_ALIAS);
		when(filter1Reference.getProperty(org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_TARGET)).thenReturn(FILTER_1_TARGET);
		when(filter1Reference.getProperty(Constants.SERVICE_ID)).thenReturn(6L);
		
		when(filter2Reference.getBundle()).thenReturn(bundle);
		when(filter2Reference.getPropertyKeys()).thenReturn(new String[0]);
		when(filter2Reference.getProperty(HttpWhiteboardConstants.PATTERN)).thenReturn(SERVLET_2_ALIAS);
		when(filter2Reference.getProperty(org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_TARGET)).thenReturn(FILTER_2_TARGET);
		when(filter2Reference.getProperty(Constants.SERVICE_ID)).thenReturn(7L);

		when(filter3Reference.getBundle()).thenReturn(bundle);
		when(filter3Reference.getPropertyKeys()).thenReturn(new String[0]);
		when(filter3Reference.getProperty(HttpWhiteboardConstants.PATTERN)).thenReturn(SERVLET_3_ALIAS);
		when(filter3Reference.getProperty(Constants.SERVICE_ID)).thenReturn(8L);

		this.http1Service = new MockExtHttpService();
		this.http2Service = new MockExtHttpService();
	}
	
	@After
	public void tearDown()
	{
		this.http1Service = null;
		this.http2Service = null;
	}
	
	@Test
	public void test_register_servlet()
	{
		ExtenderManager em = new ExtenderManager();
		
		// http1Service: org.osgi.service.http.port=80
		em.setHttpService(http1Service, http1ServiceReference);
		
		TestCase.assertTrue(http1Service.getServlets().isEmpty());
		
		// servlet1: osgi.http.whiteboard.target = "(org.osgi.service.http.port=80)" 
		// serlvet1 must be registered with http1Service
		em.add(servlet1, servlet1Reference);
		TestCase.assertEquals(1, http1Service.getServlets().size());
		TestCase.assertEquals(servlet1, http1Service.getServlets().get(SERVLET_1_ALIAS));

		// servlet2: osgi.http.whiteboard.target = "(org.osgi.service.http.port=8080)" 
		// serlvet2 must not be registered with http1Service
		em.add(servlet2, servlet2Reference);
		TestCase.assertEquals(1, http1Service.getServlets().size());
		TestCase.assertEquals(null, http1Service.getServlets().get(SERVLET_2_ALIAS));

		// servlet3: osgi.http.whiteboard.target is not set 
		// serlvet3 must be registered with http1Service
		em.add(servlet3, servlet3Reference);
		TestCase.assertEquals(2, http1Service.getServlets().size());
		TestCase.assertEquals(servlet3, http1Service.getServlets().get(SERVLET_3_ALIAS));
	}
	
	@Test
	public void test_servlet_after_httpService_reset()
	{
		ExtenderManager em = new ExtenderManager();
		
		// http1Service: org.osgi.service.http.port=80
		em.setHttpService(http1Service, http1ServiceReference);
		
		// servlet1 and servlet3 must both be registered with http1Service
		// servlet1's target property matches the http1Service properties
		// servlet3's target property is not set
		em.add(servlet1, servlet1Reference);
		em.add(servlet3, servlet3Reference);
		
		TestCase.assertEquals(2, http1Service.getServlets().size());
		TestCase.assertEquals(servlet1, http1Service.getServlets().get(SERVLET_1_ALIAS));
		TestCase.assertEquals(servlet3, http1Service.getServlets().get(SERVLET_3_ALIAS));
		
		// now set http2Service, expect servlet3 to remain registered, as its target property is not set
		// expect servlet1 not to be registered, its target property does not match the http2Service properties
		TestCase.assertEquals(0, http2Service.getServlets().size());
		em.unsetHttpService(); // TODO: clarify this!
		em.setHttpService(http2Service, http2ServiceReference);
		TestCase.assertEquals(1, http2Service.getServlets().size());
		TestCase.assertEquals(null, http2Service.getServlets().get(SERVLET_1_ALIAS));
		TestCase.assertEquals(servlet3, http2Service.getServlets().get(SERVLET_3_ALIAS));
	}
	
	@Test
	public void test_register_filter()
	{
		ExtenderManager em = new ExtenderManager();
		em.setHttpService(http1Service, http1ServiceReference);
		
		TestCase.assertTrue(http1Service.getFilters().isEmpty());
		
		// filter1 must be registered with http1Service, its target property matches
		// the properties of the http1Service
		em.add(filter1, filter1Reference);
		TestCase.assertEquals(1, http1Service.getFilters().size());
		TestCase.assertEquals(filter1, http1Service.getFilters().get(SERVLET_1_ALIAS));
		
		// filter2 must not be registered with http1Service, its target property
		// does not match the properties of the http1Service
		em.add(filter2, filter2Reference);
		TestCase.assertEquals(1, http1Service.getFilters().size());
		TestCase.assertEquals(null, http1Service.getFilters().get(SERVLET_2_ALIAS));
		
		// filter 3 must be registered with http1Service since it does not have 
		// a target property set
		em.add(filter3, filter3Reference);
		TestCase.assertEquals(2, http1Service.getFilters().size());
		TestCase.assertEquals(filter3, http1Service.getFilters().get(SERVLET_3_ALIAS));
	}
	
	@Test
	public void test_filter_after_httpService_reset()
	{
		ExtenderManager em = new ExtenderManager();
		em.setHttpService(http1Service, http1ServiceReference);
		
		// filter1 and filter3 must be registered with the http1Service
		// target property of filter1 matches the properties of http1Service
		// filter3 does not have a target property set
		em.add(filter1, filter1Reference);
		em.add(filter3, filter3Reference);
		
		TestCase.assertEquals(2, http1Service.getFilters().size());
		TestCase.assertEquals(filter1, http1Service.getFilters().get(SERVLET_1_ALIAS));
		TestCase.assertEquals(filter3, http1Service.getFilters().get(SERVLET_3_ALIAS));
		
		TestCase.assertEquals(0, http2Service.getFilters().size());
		
		// reset to http2Service
		// filter1 must not be registered, its target property does not match the 
		// properties of http2Service
		// fitler3 must be registered since it does not have a target property
		em.unsetHttpService();
		em.setHttpService(http2Service, http2ServiceReference);
		TestCase.assertEquals(1, http2Service.getFilters().size());
		TestCase.assertEquals(null, http2Service.getFilters().get(SERVLET_1_ALIAS));
		TestCase.assertEquals(filter3, http2Service.getFilters().get(SERVLET_3_ALIAS));
	}
		
	static final class MockExtHttpService implements ExtHttpService
	{
		private final BidiMap servlets = new DualHashBidiMap();
		private final BidiMap filters = new DualHashBidiMap();
		
		public BidiMap getServlets()
		{
			return servlets;
		}
		
		public BidiMap getFilters()
		{
			return filters;
		}

		@Override
		public void registerServlet(String alias, Servlet servlet,
				Dictionary initparams, HttpContext context)
		{
			TestCase.assertNotNull(context);
			this.servlets.put(alias, servlet);
		}

		@Override
		public void registerResources(String alias, String name,
				HttpContext context) throws NamespaceException {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void unregister(String alias) {
			this.servlets.remove(alias);			
		}

		@Override
		public HttpContext createDefaultHttpContext() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void registerFilter(Filter filter, String pattern,
				Dictionary initParams, int ranking, HttpContext context)
				throws ServletException {
			
			this.filters.put(pattern, filter);
			
		}

		@Override
		public void unregisterFilter(Filter filter) {
			this.filters.removeValue(filter);
		}

		@Override
		public void unregisterServlet(Servlet servlet) {
			this.servlets.removeValue(servlet);
			
		}
	}
}
