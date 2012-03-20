/* Copyright 2011 predic8 GmbH, www.predic8.com

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License. */

package com.predic8.membrane.core.interceptor.cbr;

import junit.framework.TestCase;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.predic8.membrane.core.Router;
import com.predic8.membrane.core.http.Header;
import com.predic8.membrane.core.http.MimeType;

public class XPathCBRInterceptorIntegrationTest extends TestCase {
		
	private Router router;
	
	@Before
	public void setUp() throws Exception {		
		router = Router.init("cbr-monitor-beans.xml");
		router.getConfigurationManager().loadConfiguration("classpath:/cbr.proxies.xml");
	}
	
	@Test
	public void testRouting() throws Exception {
		PostMethod post = createPostMethod();
		new HttpClient().executeMethod(post);
		System.out.println(post.getResponseBodyAsString());
	}

	@After
	public void tearDown() throws Exception {
		router.getTransport().closeAll();
	}
	
	private PostMethod createPostMethod() {
		PostMethod post = new PostMethod("http://localhost:5000/");
		post.setRequestEntity(new InputStreamRequestEntity(this.getClass().getResourceAsStream("/cbr.xml"))); 
		post.setRequestHeader(Header.CONTENT_TYPE, MimeType.TEXT_XML_UTF8);
		post.setRequestHeader(Header.SOAP_ACTION, "");
		return post;
	}
}
