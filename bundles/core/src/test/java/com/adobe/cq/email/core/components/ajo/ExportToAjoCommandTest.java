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

import org.apache.http.HttpStatus;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.servlets.HtmlResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExportToAjoCommandTest {

    private static final String PATH = "/path";
    public static final String TEMPLATE_HTML = "html";
    public static final String TEMPLATE_NAME = "template name";
    public static final String TEMPLATE_DESCRIPTION = "template description";

    @Mock
    EmailContentRenderer emailContentRenderer;

    @Mock
    EmailContentExporter emailContentExporter;

    @Mock
    SlingHttpServletRequest slingHttpServletRequest;

    ExportToAjoCommand sut;

    @BeforeEach
    void setUp() {
        sut = new ExportToAjoCommand(emailContentRenderer, emailContentExporter);
    }

    @Test
    void getCommandName() {
        assertEquals(ExportToAjoCommand.WCM_COMMAND_NAME, sut.getCommandName());
    }

    @Test
    void performCommand() throws AjoException {
        when(slingHttpServletRequest.getParameter(ExportToAjoCommand.PATH_PARAM)).thenReturn(PATH);
        when(slingHttpServletRequest.getParameter(ExportToAjoCommand.TEMPLATE_NAME_PARAM)).thenReturn(TEMPLATE_NAME);
        when(slingHttpServletRequest.getParameter(ExportToAjoCommand.TEMPLATE_DESCRIPTION_PARAM)).thenReturn(TEMPLATE_DESCRIPTION);

        when(emailContentRenderer.render(eq(PATH), any())).thenReturn(TEMPLATE_HTML);

        HtmlResponse response = sut.performCommand(null, slingHttpServletRequest, null, null);

        assertEquals(HttpStatus.SC_OK, response.getStatusCode());

        verify(emailContentRenderer).render(eq(PATH), any());
        verify(emailContentExporter).export(eq(TEMPLATE_HTML), eq(TEMPLATE_NAME), eq(TEMPLATE_DESCRIPTION));
    }

    @Test
    void performCommandReturnsBadStatus() throws AjoException {
        doThrow(new AjoException()).when(emailContentRenderer).render(any(), any());
        HtmlResponse response = sut.performCommand(null, slingHttpServletRequest, null, null);
        assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatusCode());
    }
}
