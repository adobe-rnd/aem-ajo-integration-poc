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
package com.adobe.cq.email.core.components.ajo.impl;

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
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.adobe.cq.email.core.components.ajo.AjoException;
import com.adobe.cq.email.core.components.ajo.EmailContentRenderer;
import com.adobe.cq.email.core.components.services.StylesInlinerService;
import com.day.cq.contentsync.handler.util.RequestResponseFactory;
import com.day.cq.wcm.api.WCMMode;

@Component(service = EmailContentRenderer.class)
public class EmailContentRendererImpl implements EmailContentRenderer {

    @Reference
    private transient RequestResponseFactory requestResponseFactory;

    @Reference
    private transient SlingRequestProcessor requestProcessor;

    @Reference
    private transient StylesInlinerService stylesInlinerService;

    public String render(String path, ResourceResolver resourceResolver) throws AjoException {
        try {
            Map<String, Object> params = new HashMap<>();
            HttpServletRequest req = requestResponseFactory.createRequest("GET", path + ".html", params);
            WCMMode.DISABLED.toRequest(req);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            HttpServletResponse response = requestResponseFactory.createResponse(out);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            requestProcessor.processRequest(req, response, resourceResolver);
            return stylesInlinerService.getHtmlWithInlineStyles(resourceResolver, out.toString(StandardCharsets.UTF_8.name()));
        } catch (ServletException | IOException e) {
            throw new AjoException("Error while rendering HTML", e);
        }
    }
}
