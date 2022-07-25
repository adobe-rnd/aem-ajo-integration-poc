/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2022 Adobe
 ~
 ~ Licensed under the Apache License, Version 2.0 (the "License");
 ~ you may not use this file except in compliance with the License.
 ~ You may obtain a copy of the License at
 ~
 ~     http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing, software
 ~ distributed under the License is distributed on an "AS IS" BASIS,
 ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ~ See the License for the specific language governing permissions and
 ~ limitations under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
package com.adobe.cq.email.core.components.ajo.services.impl;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.engine.SlingRequestProcessor;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.email.core.components.ajo.AjoException;
import com.adobe.cq.email.core.components.ajo.services.AjoConnector;
import com.adobe.cq.email.core.components.ajo.services.AjoExporter;
import com.adobe.cq.email.core.components.services.StylesInlinerService;
import com.day.cq.contentsync.handler.util.RequestResponseFactory;
import com.day.cq.wcm.api.WCMMode;

@Component(service = AjoExporter.class)
public class AjoExporterImpl implements AjoExporter {
    private final Logger log = LoggerFactory.getLogger(AjoExporterImpl.class);

    private final transient RequestResponseFactory requestResponseFactory;
    private final transient SlingRequestProcessor requestProcessor;
    private final transient StylesInlinerService stylesInlinerService;
    private final transient AjoConnector ajoConnector;

    @Activate
    public AjoExporterImpl(
        @Reference RequestResponseFactory requestResponseFactory,
        @Reference SlingRequestProcessor requestProcessor,
        @Reference StylesInlinerService stylesInlinerService,
        @Reference AjoConnector ajoConnector) {
        this.requestResponseFactory = requestResponseFactory;
        this.requestProcessor = requestProcessor;
        this.stylesInlinerService = stylesInlinerService;
        this.ajoConnector = ajoConnector;
    }

    @Override
    public void export(String name, String description, String path, ResourceResolver resourceResolver) throws AjoException {
        String html = renderHtml(path, resourceResolver);
        String htmlWithInlinedStyles = stylesInlinerService.getHtmlWithInlineStyles(resourceResolver, html);
        ajoConnector.createTemplate(name, description, htmlWithInlinedStyles);
    }

    private String renderHtml(String path, ResourceResolver resourceResolver) throws AjoException {
        try {
            Map<String, Object> params = new HashMap<>();
            HttpServletRequest request = requestResponseFactory.createRequest("GET", path + ".html", params);
            WCMMode.DISABLED.toRequest(request);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            HttpServletResponse response = requestResponseFactory.createResponse(out);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            requestProcessor.processRequest(request, response, resourceResolver);
            return out.toString(StandardCharsets.UTF_8.name());
        } catch (ServletException | IOException e) {
            throw new AjoException("Error while rendering HTML", e);
        }
    }
}
