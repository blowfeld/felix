package org.apache.felix.http.base.internal.runtime.dto;

import static java.util.Arrays.asList;

import org.apache.felix.http.base.internal.handler.FilterHandler;
import org.apache.felix.http.base.internal.handler.HandlerRegistry;
import org.apache.felix.http.base.internal.handler.ServletHandler;
import org.osgi.service.http.runtime.dto.FilterDTO;
import org.osgi.service.http.runtime.dto.RequestInfoDTO;

public final class RequestInfoDTOBuilder
{
    private static final FilterDTO[] FILTER_DTO_ARRAY = new FilterDTO[0];

    private final HandlerRegistry registry;
    private final String path;

    public RequestInfoDTOBuilder(HandlerRegistry registry, String path)
    {
        this.registry = registry;
        this.path = path;
    }

    public RequestInfoDTO build()
    {
        ServletHandler servletHandler = registry.getServletHander(path);
        FilterHandler[] filterHandlers = registry.getFilterHandlers(servletHandler, null, path);
        Long contextServiceId = servletHandler.getContextServiceId();

        RequestInfoDTO requestInfoDTO = new RequestInfoDTO();
        requestInfoDTO.path = path;
        requestInfoDTO.servletContextId = contextServiceId;

        requestInfoDTO.filterDTOs = FilterDTOBuilder.create()
                .build(asList(filterHandlers), contextServiceId)
                .toArray(FILTER_DTO_ARRAY);

        if (servletHandler.getServletInfo().isResource())
        {
            requestInfoDTO.resourceDTO = ResourceDTOBuilder.create()
                    .buildDTO(servletHandler, contextServiceId);
        }
        else
        {
            requestInfoDTO.servletDTO = ServletDTOBuilder.create()
                    .buildDTO(servletHandler, contextServiceId);
        }
        return requestInfoDTO;
    }
}
