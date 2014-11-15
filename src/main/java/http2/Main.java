/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package http2;

import java.util.EnumSet;

import javax.servlet.DispatcherType;

import org.eclipse.jetty.alpn.ALPN;
import org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.NegotiatingServerConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlets.PushCacheFilter;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

public class Main {


	public static void main(String[] args) throws Exception {

		AnnotationConfigWebApplicationContext cxt = new AnnotationConfigWebApplicationContext();
		cxt.register(WebConfig.class);
		DispatcherServlet dispatcherServlet = new DispatcherServlet(cxt);

		Server server = new Server();

		ServletContextHandler context = new ServletContextHandler();
		context.setResourceBase("src/main/webapp");
		context.addServlet(new ServletHolder("default", DefaultServlet.class), "");
		context.addServlet(new ServletHolder(dispatcherServlet), "/");
		context.addFilter(new FilterHolder(new PushCacheFilter()), "/*", EnumSet.of(DispatcherType.REQUEST));
		server.setHandler(context);

		HttpConfiguration httpConfig = new HttpConfiguration();
		httpConfig.setSecureScheme("https");
		httpConfig.setSecurePort(8443);
		httpConfig.setSendXPoweredBy(true);
		httpConfig.setSendServerVersion(true);

		HttpConfiguration httpsConfig = new HttpConfiguration(httpConfig);
		httpsConfig.addCustomizer(new SecureRequestCustomizer());

		ServerConnector httpConnector = new ServerConnector(server, new HttpConnectionFactory(httpConfig));
		httpConnector.setPort(8080);
		server.addConnector(httpConnector);

		ServerConnector http2Connector = createHttp2Connector(server, httpsConfig);
		http2Connector.setPort(8443);
		server.addConnector(http2Connector);

		ALPN.debug=true;

		server.start();
		server.dumpStdErr();
		server.join();
	}

	private static ServerConnector createHttp2Connector(Server server, HttpConfiguration httpsConfig) {

		HTTP2ServerConnectionFactory http2Factory = new HTTP2ServerConnectionFactory(httpsConfig);

		String http2Protocol = http2Factory.getProtocol();
		String defaultProtocol = HttpVersion.HTTP_1_1.asString();
		ALPNServerConnectionFactory alpnFactory = new ALPNServerConnectionFactory(http2Protocol, defaultProtocol);
		alpnFactory.setDefaultProtocol(defaultProtocol);

		SslContextFactory sslContextFactory = new SslContextFactory();
		sslContextFactory.setKeyStorePath("src/main/config/etc/keystore");
		sslContextFactory.setKeyStorePassword("OBF:1vny1zlo1x8e1vnw1vn61x8g1zlu1vn4");
		sslContextFactory.setKeyManagerPassword("OBF:1u2u1wml1z7s1z7a1wnl1u2g");
		SslConnectionFactory sslFactory = new SslConnectionFactory(sslContextFactory, alpnFactory.getProtocol());

		return new ServerConnector(server, sslFactory, alpnFactory, http2Factory, new HttpConnectionFactory(httpsConfig));
	}

}
