/* Copyright 2012 predic8 GmbH, www.predic8.com

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License. */
package com.predic8.membrane.core.rules;

import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.predic8.membrane.annot.Required;

import com.google.common.collect.Lists;
import com.predic8.membrane.annot.MCAttribute;
import com.predic8.membrane.annot.MCElement;
import com.predic8.membrane.core.Constants;
import com.predic8.membrane.core.Router;
import com.predic8.membrane.core.config.security.SSLParser;
import com.predic8.membrane.core.interceptor.Interceptor;
import com.predic8.membrane.core.interceptor.WSDLInterceptor;
import com.predic8.membrane.core.interceptor.rewrite.RewriteInterceptor;
import com.predic8.membrane.core.interceptor.server.WSDLPublisherInterceptor;
import com.predic8.membrane.core.interceptor.soap.WebServiceExplorerInterceptor;
import com.predic8.membrane.core.resolver.ResourceRetrievalException;
import com.predic8.membrane.core.resolver.HTTPSchemaResolver;
import com.predic8.membrane.core.resolver.ResolverMap;
import com.predic8.membrane.core.transport.http.client.HttpClientConfiguration;
import com.predic8.membrane.core.util.URLUtil;
import com.predic8.membrane.core.ws.relocator.Relocator.PathRewriter;
import com.predic8.wsdl.AbstractBinding;
import com.predic8.wsdl.Definitions;
import com.predic8.wsdl.Port;
import com.predic8.wsdl.Service;
import com.predic8.wsdl.WSDLParser;
import com.predic8.wsdl.WSDLParserContext;

import javax.xml.namespace.QName;

import static com.predic8.membrane.core.Constants.*;

//PV
import com.predic8.membrane.core.transport.ssl.StaticSSLContext;
import com.predic8.membrane.core.RuleManager;
import java.util.ArrayList;

/**
 * @description <p>
 *              A SOAP proxy can be deployed on front of a SOAP Web Service. It conceals the server and offers the same
 *              interface as the target server to its clients.
 *              </p>
 * @explanation If the WSDL referenced by the <i>wsdl</i> attribute is not available at startup, the &lt;soapProxy&gt;
 *              will become inactive. Through the admin console, reinitialization attempts can be triggered and, by
 *              default, the {@link Router} also periodically triggers such attempts.
 * @topic 2. Proxies
 */
@MCElement(name="soapProxy")
public class SOAPProxy extends AbstractServiceProxy {
	private static final Logger log = LoggerFactory.getLogger(SOAPProxy.class.getName());
	private static final Pattern relativePathPattern = Pattern.compile("^./[^/?]*\\?");

	// configuration attributes
	protected String wsdl;
	protected String portName;
	protected String targetPath;
	protected HttpClientConfiguration httpClientConfig;

	//PV
        // List containing all service SOAPProxies defined in the WSDL
	protected List<SOAPProxy> proxies = new ArrayList();

	// set during initialization
	protected ResolverMap resolverMap;

	public SOAPProxy() {
		this.key = new ServiceProxyKey(80);
	}

	@Override
	protected AbstractProxy getNewInstance() {
		return new SOAPProxy();
	}

	private void parseWSDL() throws Exception {
		WSDLParserContext ctx = new WSDLParserContext();
		ctx.setInput(ResolverMap.combine(router.getBaseLocation(), wsdl));
		try {
			WSDLParser wsdlParser = new WSDLParser();
			wsdlParser.setResourceResolver(resolverMap.toExternalResolver().toExternalResolver());

			Definitions definitions = wsdlParser.parse(ctx);

			List<Service> services = definitions.getServices();
			/** PV
                         * A WSDL has 1 or more services. Process the first service as usual.
                         * Code isolated as 'configureSOAPProxy()' function because we might need it for other services.
                         */
			configureSOAPProxy(this, services.get(0), definitions, null);

			// Check if there are more services in the WSDL
			if (services.size() > 1) {
				// Already processed the first service
				services.remove(0);
			
				for (Service service: services) {
					// Create instance of ourselves (SOAPProxy)
					SOAPProxy item = (SOAPProxy)getNewInstance();
					item.key = new ServiceProxyKey(this.key);
					item.key.setPath(null);

                                        // now process each service
					configureSOAPProxy(item, service, definitions, this);
				}
			}
		} catch (Exception e) {
			Throwable f = e;
			while (f.getCause() != null && ! (f instanceof ResourceRetrievalException))
				f = f.getCause();
			if (f instanceof ResourceRetrievalException rre) {
				if (rre.getStatus() >= 400)
					throw rre;
				Throwable cause = rre.getCause();
				if (cause != null) {
					if (cause instanceof UnknownHostException)
						throw (UnknownHostException) cause;
					else if (cause instanceof ConnectException)
						throw (ConnectException) cause;
				}
			}
			throw new IllegalArgumentException("Could not download the WSDL '" + wsdl + "'.", e);
		}
	}

	/** PV
         * First call to this function is with the 'real' SOAPProxy object created by the Spring context. Possible next calls
         * are for the other SOAPProxy objects created by services defined in the WSDL, with a reference to the 'real' SOAPProxy object.
         */
	private void configureSOAPProxy(SOAPProxy proxy, Service service, Definitions definitions, SOAPProxy firstRule) throws Exception {
		if (StringUtils.isEmpty(proxy.name))
			proxy.name = StringUtils.isEmpty(service.getName()) ? definitions.getName() : service.getName();

		List<Port> ports = service.getPorts();
		Port port = selectPort(ports, portName);

		String location = port.getAddress().getLocation();
		if (location == null)
			throw new IllegalArgumentException("In the WSDL, there is no @location defined on the port.");
		try {
			URL url = new URL(location);
			if (wsdl.startsWith("service:")) {
				proxy.target.setUrl(wsdl.substring(0, wsdl.indexOf('/')));
			} else {
				proxy.target.setHost(url.getHost());
				if (url.getPort() != -1)
					proxy.target.setPort(url.getPort());
				else
					proxy.target.setPort(url.getDefaultPort());
			}
			if (proxy.key.getPath() == null) {
				proxy.key.setUsePathPattern(true);
				proxy.key.setPathRegExp(false);
				proxy.key.setPath(url.getPath());
			} else {
				String query = "";
				if(url.getQuery() != null){
					query = "?" + url.getQuery();
				}
				proxy.targetPath = url.getPath()+ query;
			}
			if(location.startsWith("https")){
				proxy.target.setSslParser(firstRule == null ? new SSLParser() : firstRule.getTargetSSL());
			}
			((ServiceProxyKey)proxy.key).setMethod("*");
			proxies.add(proxy);
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException("WSDL endpoint location '"+location+"' is not an URL.", e);
		}
	}

	public static Port selectPort(List<Port> ports, String portName) {
		if (portName != null) {
			for (Port port : ports)
				if (portName.equals(port.getName()))
					return port;
			throw new IllegalArgumentException("No port with name '" + portName + "' found.");
		}
		return getPort(ports);
	}

	private static Port getPort(List<Port> ports) {
		Port port = getPortByNamespace(ports, WSDL_SOAP11_NS);
		if (port == null)
			port = getPortByNamespace(ports, WSDL_SOAP12_NS);
		if (port == null)
			throw new IllegalArgumentException("No SOAP/1.1 or SOAP/1.2 ports found in WSDL.");
		return port;
	}

	private static Port getPortByNamespace(List<Port> ports, String namespace) {
		for (Port port : ports) {
			try {
				if (port.getBinding() == null)
					continue;
				if (port.getBinding().getBinding() == null)
					continue;
				AbstractBinding binding = port.getBinding().getBinding();
				if (!"http://schemas.xmlsoap.org/soap/http".equals(binding.getProperty("transport")))
					continue;
				if (!namespace.equals(((QName)binding.getElementName()).getNamespaceURI()))
					continue;
				return port;
			} catch (Exception e) {
				log.warn("Error inspecting WSDL port binding.", e);
			}
		}
		return null;
	}

	private int automaticallyAddedInterceptorCount;

	public void configure() throws Exception {

		parseWSDL();

		// Process each SOAPProxy as usual 
		for (SOAPProxy proxy: proxies) {
			// remove previously added interceptors
			for(; proxy.automaticallyAddedInterceptorCount > 0; proxy.automaticallyAddedInterceptorCount--)
				proxy.interceptors.remove(0);

			// add interceptors (in reverse order) to position 0.

			WebServiceExplorerInterceptor sui = new WebServiceExplorerInterceptor();
			sui.setWsdl(wsdl);
			sui.setPortName(proxy.portName);
			proxy.interceptors.add(0, sui);
			proxy.automaticallyAddedInterceptorCount++;

			boolean hasPublisher = proxy.getInterceptorOfType(WSDLPublisherInterceptor.class) != null;
			if (!hasPublisher) {
				WSDLPublisherInterceptor wp = new WSDLPublisherInterceptor();
				wp.setWsdl(wsdl);
				proxy.interceptors.add(0, wp);
				proxy.automaticallyAddedInterceptorCount++;
			}

			WSDLInterceptor wsdlInterceptor = proxy.getInterceptorOfType(WSDLInterceptor.class);
			boolean hasRewriter = wsdlInterceptor != null;
			if (!hasRewriter) {
				wsdlInterceptor = new WSDLInterceptor();
				proxy.interceptors.add(0, wsdlInterceptor);
				proxy.automaticallyAddedInterceptorCount++;
			}

			if (proxy.key.getPath() != null) {
				final String keyPath = proxy.key.getPath();
				final String name = URLUtil.getName(router.getUriFactory(), keyPath);
				wsdlInterceptor.setPathRewriter(path2 -> {
					try {
						if (path2.contains("://")) {
							return new URL(new URL(path2), keyPath).toString();
						} else {
							Matcher m = relativePathPattern.matcher(path2);
							return m.replaceAll("./" + name + "?");
						}
					} catch (MalformedURLException e) {
						log.error("Cannot parse URL " + path2);
					}
					return path2;
				});
			}

			if (hasRewriter && !hasPublisher)
				log.warn("A <soapProxy> contains a <wsdlRewriter>, but no <wsdlPublisher>. Probably you want to insert a <wsdlPublisher> just after the <wsdlRewriter>. (Or, if this is a valid use case, please notify us at " + PRODUCT_CONTACT_EMAIL + ".)");

			if (proxy.targetPath != null) {
				RewriteInterceptor ri = new RewriteInterceptor();
				ri.setMappings(Lists.newArrayList(new RewriteInterceptor.Mapping("^" + Pattern.quote(proxy.key.getPath()), Matcher.quoteReplacement(targetPath), "rewrite")));
				proxy.interceptors.add(0, ri);
				proxy.automaticallyAddedInterceptorCount++;
			}
		}
	}

	@Override
	public void init() throws Exception {
		if (wsdl == null) {
			return;
		}

		resolverMap = router.getResolverMap();
		if (httpClientConfig != null) {
			HTTPSchemaResolver httpSR = new HTTPSchemaResolver(router.getHttpClientFactory());
			httpSR.setHttpClientConfig(httpClientConfig);
			resolverMap = resolverMap.clone();
			resolverMap.addSchemaResolver(httpSR);
		}

		configure();
		super.init();

		/** PV
                 * Here, the 'real' SOAPProxy is initialized, including its SSLContext. Since all other SOAPProxies
                 * are in the same WSDL, they share the same endpoint. So, copy the SSLContext to all other proxies
                 * and initialize.
                 */
                StaticSSLContext sslc = ((StaticSSLContext)getSslInboundContext());
		SSLParser sslParser = sslc != null ? sslc.getSslParser() : this.target.getSslParser();

		for (SOAPProxy proxy: proxies) {
			if (proxy == this)
				continue;
			proxy.target.setSslParser(sslParser);
			proxy.init(this.router);
			proxy.initSsl();
			// This can potentially cause ConcurrentModificationException
			proxy.router.getRuleManager().addProxy(proxy, RuleManager.RuleDefinitionSource.SPRING);
		}
	}

	@SuppressWarnings("unchecked")
	private <T extends Interceptor> T getInterceptorOfType(Class<T> class1) {
		for (Interceptor i : interceptors)
			if (class1.isInstance(i))
				return (T) i;
		return null;
	}

	public String getWsdl() {
		return wsdl;
	}

	/**
     * @description The WSDL of the SOAP service.
     * @example <a href="http://predic8.de/my.wsdl">http://predic8.de/my.wsdl</a> <i>or</i> file:my.wsdl
     */
	@Required
	@MCAttribute
	public void setWsdl(String wsdl) {
		this.wsdl = wsdl;
	}

	public String getPortName() {
		return portName;
	}

	@MCAttribute
	public void setPortName(String portName) {
		this.portName = portName;
	}

	public HttpClientConfiguration getWsdlHttpClientConfig() {
		return httpClientConfig;
	}

	@MCAttribute
	public void setWsdlHttpClientConfig(HttpClientConfiguration httpClientConfig) {
		this.httpClientConfig = httpClientConfig;
	}

}
