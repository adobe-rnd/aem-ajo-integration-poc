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

import java.io.IOException;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import com.adobe.cq.email.core.components.ajo.AjoException;
import com.adobe.cq.email.core.components.ajo.EmailContentExporter;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@Component(service = EmailContentExporter.class)
@Designate(ocd = EmailContentExporterImpl.Config.class)
public class EmailContentExporterImpl implements EmailContentExporter {

    public static final String TEMPLATES_ENDPOINT = "https://platform-stage.adobe.io/journey/authoring/message/templates";

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

    private Config config;

    @SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE")
    public void export(String html, String name, String description) throws AjoException {

        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            HttpPost post = new HttpPost(TEMPLATES_ENDPOINT);

            post.setHeader(HttpHeaders.CONTENT_TYPE, "application/vnd.adobe.cjm.template.v1+json");
            post.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + config.imsUserToken());

            post.setHeader("x-api-key", config.apiKey());
            post.setHeader("x-gw-ims-org-id", config.orgId());
            post.setHeader("x-sandbox-name", config.sandboxName());

            JSONObject templateHtml = new JSONObject();
            templateHtml.put("html", html);

            JSONObject template = new JSONObject();
            template.put("name", name);
            template.put("description", description);
            template.put("type", "email_html");
            template.put("template", templateHtml);

            StringEntity entiry = new StringEntity(template.toString(), "UTF-8");
            post.setEntity(entiry);

            try (CloseableHttpResponse res = httpClient.execute(post)) {
                if (res.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                    throw new AjoException("Bad response received from AJO: " + res.getStatusLine().getReasonPhrase());
                }
            }
        } catch (JSONException | IOException e) {
            throw new AjoException("Error while exporting to AJO", e);
        }
    }

    @Activate
    protected void activate(Config config) {
        this.config = config;
    }
}
