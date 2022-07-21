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
package com.adobe.cq.email.core.components.ajo;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HtmlResponse;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.commons.servlets.HtmlStatusResponseHelper;
import com.day.cq.i18n.I18n;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.commands.WCMCommand;
import com.day.cq.wcm.api.commands.WCMCommandContext;

@Component(service = WCMCommand.class, immediate = true)
public class ExportToAjoCommand implements WCMCommand {
    public static final String PATH_PARAM = "path";
    public static final String TEMPLATE_NAME_PARAM = "templateName";
    public static final String TEMPLATE_DESCRIPTION_PARAM = "templateDescription";
    private final Logger log = LoggerFactory.getLogger(ExportToAjoCommand.class);

    @Reference
    private transient EmailContentRenderer emailContentRenderer;

    @Reference
    private transient EmailContentExporter emailContentExporter;

    public String getCommandName() {
        return "exportToAjo";
    }

    public HtmlResponse performCommand(WCMCommandContext ctx,
                                       SlingHttpServletRequest request,
                                       SlingHttpServletResponse response,
                                       PageManager pageManager) {
        try {
            String path = request.getParameter(PATH_PARAM);
            String html = emailContentRenderer.render(path, request.getResourceResolver());

            String name = request.getParameter(TEMPLATE_NAME_PARAM);
            String description = request.getParameter(TEMPLATE_DESCRIPTION_PARAM);
            emailContentExporter.export(html, name, description);

            return HtmlStatusResponseHelper.createStatusResponse(true, "Success!");

        } catch (AjoException e) {
            log.error("Error during export", e);
            return HtmlStatusResponseHelper.createStatusResponse(false, I18n.get(request, e.getMessage()));
        }
    }
}
