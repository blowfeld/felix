package org.apache.felix.http.base.internal.runtime.dto;

import java.util.function.Supplier;

import org.osgi.service.http.runtime.dto.ErrorPageDTO;
import org.osgi.service.http.runtime.dto.FailedErrorPageDTO;
import org.osgi.service.http.runtime.dto.FailedFilterDTO;
import org.osgi.service.http.runtime.dto.FailedListenerDTO;
import org.osgi.service.http.runtime.dto.FailedResourceDTO;
import org.osgi.service.http.runtime.dto.FailedServletDTO;
import org.osgi.service.http.runtime.dto.FilterDTO;
import org.osgi.service.http.runtime.dto.ListenerDTO;
import org.osgi.service.http.runtime.dto.ResourceDTO;
import org.osgi.service.http.runtime.dto.ServletDTO;

final class DTOSuppliers
{
    static final Supplier<ServletDTO> SERVLET = new Supplier<ServletDTO>()
    {
        @Override
        public ServletDTO get()
        {
            return new ServletDTO();
        }
    };

    static final Supplier<FailedServletDTO> FAILED_SERVLET = new Supplier<FailedServletDTO>()
    {
        @Override
        public FailedServletDTO get()
        {
            return new FailedServletDTO();
        }
    };

    static final Supplier<FilterDTO> FILTER = new Supplier<FilterDTO>()
    {
        @Override
        public FilterDTO get()
        {
            return new FilterDTO();
        }
    };

    static final Supplier<FailedFilterDTO> FAILED_FILTER = new Supplier<FailedFilterDTO>()
    {
        @Override
        public FailedFilterDTO get()
        {
            return new FailedFilterDTO();
        }
    };

    static final Supplier<ResourceDTO> RESOURCE = new Supplier<ResourceDTO>()
    {
        @Override
        public ResourceDTO get()
        {
            return new ResourceDTO();
        }
    };

    static final Supplier<FailedResourceDTO> FAILED_RESOURCE = new Supplier<FailedResourceDTO>()
    {
        @Override
        public FailedResourceDTO get()
        {
            return new FailedResourceDTO();
        }
    };

    static final Supplier<ListenerDTO> LISTENER = new Supplier<ListenerDTO>()
    {
        @Override
        public ListenerDTO get()
        {
            return new ListenerDTO();
        }
    };

    static final Supplier<FailedListenerDTO> FAILED_LISTENER = new Supplier<FailedListenerDTO>()
    {
        @Override
        public FailedListenerDTO get()
        {
            return new FailedListenerDTO();
        }
    };

    static final Supplier<ErrorPageDTO> ERROR_PAGE = new Supplier<ErrorPageDTO>()
    {
        @Override
        public ErrorPageDTO get()
        {
            return new ErrorPageDTO();
        }
    };

    static final Supplier<FailedErrorPageDTO> FAILED_ERROR_PAGE = new Supplier<FailedErrorPageDTO>()
    {
        @Override
        public FailedErrorPageDTO get()
        {
            return new FailedErrorPageDTO();
        }
    };
}
