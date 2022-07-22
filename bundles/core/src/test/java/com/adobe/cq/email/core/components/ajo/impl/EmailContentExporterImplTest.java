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
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicStatusLine;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.adobe.cq.email.core.components.ajo.AjoException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailContentExporterImplTest {

    public static final String API_KEY = "key";
    public static final String IMS_USER_TOKEN = "token";
    public static final String ORG_ID = "org";
    public static final String SANDBOX_NAME = "sandbox";

    public static final String TEMPLATE_HTML = "html";
    public static final String TEMPLATE_NAME = "template name";
    public static final String TEMPLATE_DESCRIPTION = "template description";

    public static final String TEMPLATE_PAYLOAD = String.format(
        "{\"template\":{\"html\":\"%s\"},\"name\":\"%s\",\"description\":\"%s\",\"type\":\"email_html\"}",
        TEMPLATE_HTML, TEMPLATE_NAME, TEMPLATE_DESCRIPTION);

    @Mock
    CloseableHttpClient httpClient;

    @Mock
    CloseableHttpResponse httpResponse;

    @Mock
    EmailContentExporterImpl.Config config;

    @Captor
    ArgumentCaptor<HttpPost> httpPostArgumentCaptor;

    EmailContentExporterImpl sut;

    @BeforeEach
    void setUp() {
        lenient().when(config.apiKey()).thenReturn(API_KEY);
        lenient().when(config.imsUserToken()).thenReturn(IMS_USER_TOKEN);
        lenient().when(config.orgId()).thenReturn(ORG_ID);
        lenient().when(config.sandboxName()).thenReturn(SANDBOX_NAME);

        sut = new EmailContentExporterImpl(config);
        sut.setHttpClient(httpClient);
    }

    @Test
    void export() throws AjoException, IOException, JSONException {
        when(httpResponse.getStatusLine()).thenReturn(new BasicStatusLine(
            new ProtocolVersion("HTTP", 1, 1), HttpStatus.SC_OK, ""));
        when(httpClient.execute(any())).thenReturn(httpResponse);

        sut.export(TEMPLATE_HTML, TEMPLATE_NAME, TEMPLATE_DESCRIPTION);

        verify(httpClient).execute(httpPostArgumentCaptor.capture());

        HttpPost httpPost = httpPostArgumentCaptor.getValue();

        assertEquals(EmailContentExporterImpl.CONTENT_TYPE, httpPost.getFirstHeader(HttpHeaders.CONTENT_TYPE).getValue());
        assertEquals("Bearer " + config.imsUserToken(), httpPost.getFirstHeader(HttpHeaders.AUTHORIZATION).getValue());

        assertEquals(config.apiKey(), httpPost.getFirstHeader(EmailContentExporterImpl.API_KEY_HEADER).getValue());
        assertEquals(config.orgId(), httpPost.getFirstHeader(EmailContentExporterImpl.ORG_ID_HEADER).getValue());
        assertEquals(config.sandboxName(), httpPost.getFirstHeader(EmailContentExporterImpl.SANDBOX_NAME_HEADER).getValue());

        String payload = IOUtils.toString(httpPost.getEntity().getContent(), StandardCharsets.UTF_8);
        assertEquals(payload, TEMPLATE_PAYLOAD);
    }

    @Test
    void exportReturnsBadStatusCode() throws IOException {
        when(httpResponse.getStatusLine()).thenReturn(new BasicStatusLine(
            new ProtocolVersion("HTTP", 1, 1), HttpStatus.SC_INTERNAL_SERVER_ERROR, ""));
        when(httpClient.execute(any())).thenReturn(httpResponse);
        assertThrows(AjoException.class, () -> {
            sut.export(TEMPLATE_HTML, TEMPLATE_NAME, TEMPLATE_DESCRIPTION);
        });
    }

    @Test
    void exportThrowsException() throws IOException {
        doThrow(new IOException()).when(httpClient).execute(any());
        assertThrows(AjoException.class, () -> {
            sut.export(TEMPLATE_HTML, TEMPLATE_NAME, TEMPLATE_DESCRIPTION);
        });
    }

    @Test
    void deactivate() throws IOException {
        sut.deactivate();
        verify(httpClient).close();
    }
}
