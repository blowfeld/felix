package org.apache.felix.http.base.internal.runtime.dto;

import javax.servlet.Servlet;

import org.apache.felix.http.base.internal.runtime.ServletInfo;



public interface ServletRuntime extends WhiteboardServiceRuntime
{
    Servlet getServlet();

    ServletInfo getServletInfo();
}
