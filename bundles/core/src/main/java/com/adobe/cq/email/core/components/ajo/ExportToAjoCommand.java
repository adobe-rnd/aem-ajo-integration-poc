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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.felix.scr.annotations.Service;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HtmlResponse;
import org.apache.sling.engine.SlingRequestProcessor;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.email.core.components.services.StylesInlinerService;
import com.day.cq.commons.servlets.HtmlStatusResponseHelper;
import com.day.cq.contentsync.handler.util.RequestResponseFactory;
import com.day.cq.i18n.I18n;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.WCMMode;
import com.day.cq.wcm.api.commands.WCMCommand;
import com.day.cq.wcm.api.commands.WCMCommandContext;

@Component(service = WCMCommand.class, immediate = true)
@Designate(ocd = ExportToAjoCommand.Config.class)
public class ExportToAjoCommand implements WCMCommand {
    private final Logger log = LoggerFactory.getLogger(ExportToAjoCommand.class);

    private static final String EXPORT_AS_TEMPLATE = "template";

    @ObjectClassDefinition(
        name = "Export To AJO",
        description = "Export To AJO"
    )
    @interface Config {
        @AttributeDefinition(
            name = "API Key",
            description = "API Key"
        )
        String apiKey() default "cjm-authoring-ui";

        @AttributeDefinition(
            name = "Organization ID",
            description = "Organization ID"
        )
        String orgId() default "745F37C35E4B776E0A49421B@AdobeOrg";

        @AttributeDefinition(
            name = "Sandbox name",
            description = "Sandbox name"
        )
        String sandboxName() default "cjm-mr";

        @AttributeDefinition(
            name = "IMS User Token",
            description = "Sandbox name"
        )
        String imsUserToken() default "";
    }

    @Reference
    private transient RequestResponseFactory requestResponseFactory;

    @Reference
    private transient SlingRequestProcessor requestProcessor;

    @Reference
    private transient StylesInlinerService stylesInlinerService;

    private Config config;

    public String getCommandName() {
        return "exportToAjo";
    }

    public HtmlResponse performCommand(WCMCommandContext ctx,
                                       SlingHttpServletRequest request,
                                       SlingHttpServletResponse response,
                                       PageManager pageManager) {
        try {

            String type = request.getParameter("type");
            String path = request.getParameter("path");
            String html = renderHtml(path, request.getResourceResolver());

            if (EXPORT_AS_TEMPLATE.equals(type)) {
                exportAsTemplate(html, request.getParameterMap());
            } else {
                exportAsMessage(html, request.getParameterMap());
            }

            return HtmlStatusResponseHelper.createStatusResponse(true, "Success!");

        } catch (ServletException | IOException | JSONException e) {
            log.error("Error during export", e);
            return HtmlStatusResponseHelper.createStatusResponse(false, I18n.get(request, e.getMessage()));
        }
    }

    @Activate
    protected void activate(Config config) {
        this.config = config;
    }

    private String renderHtml(String pagePath, ResourceResolver resourceResolver) throws ServletException, IOException
    {
        Map<String, Object> params = new HashMap<>();
        HttpServletRequest req = requestResponseFactory.createRequest("GET", pagePath + ".html", params);
        WCMMode.DISABLED.toRequest(req);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        HttpServletResponse response = requestResponseFactory.createResponse(out);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        requestProcessor.processRequest(req, response, resourceResolver);
        return stylesInlinerService.getHtmlWithInlineStyles(resourceResolver, out.toString(StandardCharsets.UTF_8.name()));
    }

    private void exportAsTemplate(String html, Map<String, String[]> requestParams) throws JSONException, IOException
    {
        String name = requestParams.get("templateName")[0];
        String description = requestParams.get("templateDescription")[0];

        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        HttpPost post = new HttpPost("https://platform-stage.adobe.io/journey/authoring/message/templates");

        post.setHeader(HttpHeaders.CONTENT_TYPE,"application/vnd.adobe.cjm.template.v1+json");
        post.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + config.imsUserToken());

        post.setHeader("x-api-key", config.apiKey());
        post.setHeader("x-gw-ims-org-id",config.orgId());
        post.setHeader("x-sandbox-name",config.sandboxName());

        JSONObject templateHtml = new JSONObject();
        templateHtml.put("html", html);

        JSONObject template = new JSONObject();
        template.put("name", name);
        template.put("description", description);
        template.put("type", "email_html");
        template.put("template", templateHtml);

        StringEntity params = new StringEntity(template.toString(), "UTF-8");
        post.setEntity(params);

        HttpResponse res = httpClient.execute(post);

        log.info(EntityUtils.toString(res.getEntity(), "UTF-8"));

        httpClient.close();
    }

    private void exportAsMessage(String html, Map<String, String[]> requestParams) throws JSONException, IOException
    {
        String messageName = requestParams.get("messageName")[0];
        String senderName = requestParams.get("senderName")[0];
        String senderAddress = requestParams.get("senderAddress")[0];
        String subject = requestParams.get("subject")[0];

        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        HttpPost post = new HttpPost("https://platform-stage.adobe.io/journey/authoring/message/messages");

        post.setHeader(HttpHeaders.CONTENT_TYPE,"application/json");
        post.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + config.imsUserToken());

        post.setHeader("x-api-key", config.apiKey());
        post.setHeader("x-gw-ims-org-id",config.orgId());
        post.setHeader("x-sandbox-name",config.sandboxName());

        JSONObject emailHtml = new JSONObject();
        emailHtml.put("body", html);

        JSONObject emailVariant = new JSONObject();
        emailVariant.put("name", "default");
        emailVariant.put("subject", subject);
        emailVariant.put("senderName", senderName);
        emailVariant.put("senderAddress", senderAddress);
        emailVariant.put("html", emailHtml);

        JSONObject emailChannel = new JSONObject();
        emailChannel.put("variants", new JSONArray(Arrays.asList(emailVariant)));

        JSONObject channels = new JSONObject();
        channels.put("email", emailChannel);

        JSONObject message = new JSONObject();
        message.put("name", messageName);
        message.put("brandingPresetId", "56376772-9294-4d87-a781-ed27434ad64c");
        message.put("channels", channels);

        StringEntity params = new StringEntity(message.toString(), "UTF-8");
        post.setEntity(params);

        HttpResponse res = httpClient.execute(post);

        log.info(EntityUtils.toString(res.getEntity(), "UTF-8"));

        httpClient.close();
    }
}
