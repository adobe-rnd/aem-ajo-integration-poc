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
import java.io.IOException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.engine.SlingRequestProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.adobe.cq.email.core.components.ajo.AjoException;
import com.adobe.cq.email.core.components.services.StylesInlinerService;
import com.day.cq.contentsync.handler.util.RequestResponseFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailContentRendererImplTest {

    private static final String PATH = "/path1/path2";
    private static final String HTML_OUTPUT = "html";

    @Mock
    RequestResponseFactory requestResponseFactory;

    @Mock
    SlingRequestProcessor requestProcessor;

    @Mock
    StylesInlinerService stylesInlinerService;

    @Mock
    SlingHttpServletRequest slingHttpServletRequest;

    @Mock
    SlingHttpServletResponse slingHttpServletResponse;

    @Mock
    ResourceResolver resourceResolver;

    EmailContentRendererImpl sut;

    @BeforeEach
    void setUp() {
        when(requestResponseFactory.createRequest(eq("GET"), eq(PATH + ".html"), anyMap())).thenReturn(slingHttpServletRequest);
        when(requestResponseFactory.createResponse(any())).thenReturn(slingHttpServletResponse);

        sut = new EmailContentRendererImpl(requestResponseFactory, requestProcessor, stylesInlinerService);
    }

    @Test
    void render() throws AjoException, ServletException, IOException {
        when(stylesInlinerService.getHtmlWithInlineStyles(eq(resourceResolver), any())).thenReturn(HTML_OUTPUT);
        String output = sut.render(PATH, resourceResolver);
        assertEquals(HTML_OUTPUT, output);
        verify(requestProcessor).processRequest(slingHttpServletRequest, slingHttpServletResponse, resourceResolver);
    }

    @Test
    void throwsException() throws ServletException, IOException {
        doThrow(new ServletException()).when(requestProcessor).processRequest(any(), any(), any());
        assertThrows(AjoException.class, () -> {
            sut.render(PATH, resourceResolver);
        });
    }
}
