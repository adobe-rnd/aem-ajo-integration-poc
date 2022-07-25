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
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.email.core.components.ajo.AjoException;
import com.adobe.cq.email.core.components.ajo.services.AjoConnector;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@Component(service = AjoConnector.class)
@Designate(ocd = AjoConnectorImpl.Config.class)
public class AjoConnectorImpl implements AjoConnector {
    private final Logger log = LoggerFactory.getLogger(AjoConnectorImpl.class);

    public static final String TEMPLATES_ENDPOINT = "https://platform-stage.adobe.io/journey/authoring/message/templates";
    public static final String TEMPLATES_CONTENT_TYPE = "application/vnd.adobe.cjm.template.v1+json";

    public static final String API_KEY_HEADER = "x-api-key";
    public static final String ORG_ID_HEADER = "x-gw-ims-org-id";
    public static final String SANDBOX_NAME_HEADER = "x-sandbox-name";

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

    private CloseableHttpClient httpClient;

    private Config config;

    @Activate
    public AjoConnectorImpl(Config config) {
        this.config = config;
    }

    @Override
    @SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE")
    public void createTemplate(String name, String description, String content) throws AjoException {

        try (CloseableHttpClient httpClient = getHttpClient()) {
            HttpPost post = new HttpPost(TEMPLATES_ENDPOINT);

            post.setHeader(HttpHeaders.CONTENT_TYPE, TEMPLATES_CONTENT_TYPE);
            post.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + config.imsUserToken());

            post.setHeader(API_KEY_HEADER, config.apiKey());
            post.setHeader(ORG_ID_HEADER, config.orgId());
            post.setHeader(SANDBOX_NAME_HEADER, config.sandboxName());

            JSONObject templateHtml = new JSONObject();
            templateHtml.put("html", content);

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

    public CloseableHttpClient getHttpClient() {
        if (httpClient == null) {
            httpClient = HttpClientBuilder.create().build();
        }
        return httpClient;
    }

    public void setHttpClient(CloseableHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Deactivate
    public void deactivate() {
        try {
            httpClient.close();
        } catch (IOException e) {
            log.warn("Error while closing HttpClient", e);
        }
    }
}
