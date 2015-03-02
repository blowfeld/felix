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

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;


@RunWith(JUnit4TestRunner.class)
public class ServletPatternTest extends BaseIntegrationTest 
{
	
	@Test
	public void testServletPatterns() throws Exception
	{
		CountDownLatch initLatch = new CountDownLatch(2);
		CountDownLatch destroyLatch = new CountDownLatch(2);
		
		TestServlet lowRankServlet = new TestServlet(initLatch, destroyLatch)
		{
			private static final long serialVersionUID = 1L;
			
			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException
			{
				resp.getWriter().print("lowRankServlet");
				resp.flushBuffer();
			}
		};
		
		TestServlet highRankServlet = new TestServlet(initLatch, destroyLatch)
		{
			private static final long serialVersionUID = 1L;
			
			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException
			{
				resp.getWriter().print("highRankServlet");
				resp.flushBuffer();
			}
		};
		
		Dictionary<String, Object> lowRankProps = new Hashtable<String, Object>();
		String lowRankPattern[] = {"/foo", "/bar"};
		lowRankProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, lowRankPattern);
		lowRankProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_INIT_PARAM_PREFIX + ".myname", "lowRankServlet");
		lowRankProps.put(Constants.SERVICE_RANKING, 1);
		
		ServiceRegistration<?> lowRankReg = m_context.registerService(Servlet.class.getName(), lowRankServlet, lowRankProps);
		
		Dictionary<String, Object> highRankProps = new Hashtable<String, Object>();
		String highRankPattern[] = {"/foo", "/baz"};
		highRankProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, highRankPattern);
		highRankProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_INIT_PARAM_PREFIX + ".myname", "highRankServlet");
		highRankProps.put(Constants.SERVICE_RANKING, 2);

		ServiceRegistration<?> highRankReg = m_context.registerService(Servlet.class.getName(), highRankServlet, highRankProps);
		
		try {
			assertTrue(initLatch.await(5, TimeUnit.SECONDS));
			
			assertContent("highRankServlet", createURL("/foo"));
			assertContent("lowRankServlet", createURL("/bar"));
			assertContent("highRankServlet", createURL("/baz"));
			
		} finally {
			lowRankReg.unregister();
			highRankReg.unregister();
		}
		
		assertTrue(destroyLatch.await(5, TimeUnit.SECONDS));
	}
}



