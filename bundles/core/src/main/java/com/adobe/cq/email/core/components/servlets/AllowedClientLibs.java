/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2019 Adobe
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
package com.adobe.cq.email.core.components.servlets;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.SyntheticResource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Component;

import com.adobe.granite.ui.components.Value;
import com.adobe.granite.ui.components.ds.DataSource;
import com.adobe.granite.ui.components.ds.SimpleDataSource;
import com.day.cq.wcm.api.policies.ContentPolicy;
import com.day.cq.wcm.api.policies.ContentPolicyManager;

@Component(
        service = {Servlet.class},
        property = {
                "sling.servlet.resourceTypes=" + AllowedClientLibs.RESOURCE_TYPE,
                "sling.servlet.methods=GET",
                "sling.servlet.extensions=html"
        }
)
public class AllowedClientLibs extends SlingSafeMethodsServlet {

    protected static final String RESOURCE_TYPE = "core/email/components/commons/datasources/clientlibrarycategories/v1";
    protected static final String PN_ALLOWED_CLIENT_LIBS_PATH = "clientlibs";
    protected static final String PN_ALLOWED_CLIENT_LIBS_NAME = "clientLibname";
    protected static final String PN_ALLOWED_CLIENT_LIBS = "clientlibs";

    @Override
    protected void doGet(@NotNull SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response)
            throws ServletException, IOException {
        SimpleDataSource allowedClientLibsDataSource = new SimpleDataSource(getAllowedClientLibs(request).iterator());
        request.setAttribute(DataSource.class.getName(), allowedClientLibsDataSource);
    }

    protected List<Resource> getAllowedClientLibs(@NotNull SlingHttpServletRequest request) {
        List<Resource> clientLibs = new ArrayList<>();
        ResourceResolver resolver = request.getResourceResolver();
        Resource contentResource = resolver.getResource((String) request.getAttribute(Value.CONTENTPATH_ATTRIBUTE));
        if (contentResource == null) {
            return Collections.emptyList();
        }
        ContentPolicyManager policyMgr = resolver.adaptTo(ContentPolicyManager.class);
        if (policyMgr != null) {
            ContentPolicy policy = policyMgr.getPolicy(contentResource);
            if (policy != null) {
                Resource clientLibsRes = resolver.getResource(policy.adaptTo(Resource.class), PN_ALLOWED_CLIENT_LIBS);
                if (clientLibsRes == null) {
                    return Collections.emptyList();
                }
                Iterable<Resource> children = clientLibsRes.getChildren();
                for (Resource child : children) {
                    ValueMap valueMap = child.getValueMap();
                    String clientLibName = valueMap.get(PN_ALLOWED_CLIENT_LIBS_NAME, String.class);
                    String clientLibsValue = valueMap.get(PN_ALLOWED_CLIENT_LIBS, String.class);
                    clientLibs.add(new ElementResource(clientLibName, clientLibsValue, resolver));
                }
            }
        }
        return clientLibs;
    }

    private static class ElementResource extends SyntheticResource {

        private final String elementName;
        private final String elementValue;
        private ValueMap valueMap;

        ElementResource(String headingElement, String elementValue, ResourceResolver resourceResolver) {
            super(resourceResolver, StringUtils.EMPTY, RESOURCE_TYPE_NON_EXISTING);
            this.elementName = headingElement;
            this.elementValue = elementValue;
        }

        public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
            if (type == ValueMap.class) {
                if (this.valueMap == null) {
                    this.initValueMap();
                }

                return (AdapterType) this.valueMap;
            } else {
                return super.adaptTo(type);
            }
        }

        private void initValueMap() {
            this.valueMap = new ValueMapDecorator(new HashMap());
            this.valueMap.put("value", this.getValue());
            this.valueMap.put("text", this.getText());
            this.valueMap.put("selected", this.getSelected());
        }


        public String getText() {
            return elementName;
        }

        public String getValue() {
            return elementValue;
        }

        public boolean getSelected() {
            return false;
        }
    }
}
