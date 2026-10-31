/*
 * Copyright (c) 2018, hiwepy (https://github.com/hiwepy).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.http.spring.boot.client.handler;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.StatusLine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {{ @link ObjectResponseHandler }}.
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 */
@DisplayName("ObjectResponseHandler Tests")
class ObjectResponseHandlerTest {

    @Test
    @DisplayName("Instance can be created via constructor")
    void testInstantiation() {
        ObjectResponseHandler instance = new ObjectResponseHandler();
        assertThat(instance).isNotNull();
    }

    @Test
    void readsBuiltInStringFromSuccessfulResponse() throws Exception {
        assertThat(new ObjectResponseHandler().handleResponse(response("<string>hello</string>")))
                .isEqualTo("hello");
    }

    @Test
    void rejectsUnlistedApplicationTypeByDefault() throws Exception {
        String xml = "<org.apache.http.spring.boot.client.handler.ObjectResponseHandlerTest_-Payload>"
                + "<value>hello</value></org.apache.http.spring.boot.client.handler.ObjectResponseHandlerTest_-Payload>";
        assertThatThrownBy(() -> new ObjectResponseHandler().handleResponse(response(xml)))
                .isInstanceOf(com.thoughtworks.xstream.security.ForbiddenClassException.class);
    }

    @Test
    void acceptsExplicitlyAllowedApplicationType() throws Exception {
        String xml = "<org.apache.http.spring.boot.client.handler.ObjectResponseHandlerTest_-Payload>"
                + "<value>hello</value></org.apache.http.spring.boot.client.handler.ObjectResponseHandlerTest_-Payload>";
        ObjectResponseHandler handler = new ObjectResponseHandler();
        handler.xstream.allowTypes(new Class<?>[] {Payload.class});
        assertThat(((Payload) handler.handleResponse(response(xml))).value).isEqualTo("hello");
    }

    private static HttpMethodBase response(String xml) throws Exception {
        HttpMethodBase method = mock(HttpMethodBase.class);
        StatusLine statusLine = mock(StatusLine.class);
        when(method.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(200);
        when(method.getResponseBodyAsStream()).thenReturn(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        return method;
    }

    static class Payload {
        String value;
    }
}
