/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.netconf.it;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.mockito.internal.util.Checks.checkNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.*;
import java.util.concurrent.TimeUnit;

import javax.management.ObjectName;
import javax.net.ssl.SSLContext;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.config.manager.impl.AbstractConfigTest;
import org.opendaylight.controller.config.manager.impl.factoriesresolver.HardcodedModuleFactoriesResolver;
import org.opendaylight.controller.config.manager.impl.jmx.BaseJMXRegistrator;
import org.opendaylight.controller.config.manager.impl.jmx.RootRuntimeBeanRegistratorImpl;
import org.opendaylight.controller.config.spi.ModuleFactory;
import org.opendaylight.controller.config.util.ConfigTransactionJMXClient;
import org.opendaylight.controller.config.yang.store.api.YangStoreException;
import org.opendaylight.controller.config.yang.store.impl.HardcodedYangStoreService;
import org.opendaylight.controller.config.yang.test.impl.*;
import org.opendaylight.netconf.api.NetconfMessage;
import org.opendaylight.netconf.client.NetconfClient;
import org.opendaylight.netconf.confignetconfconnector.osgi.NetconfOperationServiceFactoryImpl;
import org.opendaylight.netconf.impl.*;
import org.opendaylight.netconf.impl.osgi.NetconfOperationServiceFactoryListenerImpl;
import org.opendaylight.netconf.util.test.XmlFileLoader;
import org.opendaylight.netconf.util.xml.Xml;
import org.opendaylight.netconf.util.xml.XmlElement;
import org.opendaylight.protocol.util.SSLUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import io.netty.channel.ChannelFuture;
import io.netty.util.HashedWheelTimer;

public class NetconfITTest extends AbstractConfigTest {

	private static final Logger logger = LoggerFactory.getLogger(NetconfITTest.class);

	private static final InetSocketAddress tcpAddress = new InetSocketAddress("127.0.0.1", 12023);
	private static final InetSocketAddress tlsAddress = new InetSocketAddress("127.0.0.1", 12024);

	private NetconfMessage getConfig, getConfigCandidate, editConfig, closeSession;
	private DefaultCommitNotificationProducer commitNot;
	private NetconfServerDispatcher dispatch, dispatchS;

	@Before
	public void setUp() throws Exception {
		super.initConfigTransactionManagerImpl(new HardcodedModuleFactoriesResolver(getModuleFactories().toArray(new ModuleFactory[0])));

		loadMessages();

		NetconfOperationServiceFactoryListenerImpl factoriesListener = new NetconfOperationServiceFactoryListenerImpl();
		factoriesListener.onAddNetconfOperationServiceFactory(new NetconfOperationServiceFactoryImpl(getYangStore()));

		commitNot = new DefaultCommitNotificationProducer(ManagementFactory.getPlatformMBeanServer());

		dispatch = createDispatcher(Optional.<SSLContext> absent(), factoriesListener);
		ChannelFuture s = dispatch.createServer(tcpAddress);
		s.await();

		dispatchS = createDispatcher(Optional.of(getSslContext()), factoriesListener);
		s = dispatchS.createServer(tlsAddress);
		s.await();
	}

	private NetconfServerDispatcher createDispatcher(Optional<SSLContext> sslC, NetconfOperationServiceFactoryListenerImpl factoriesListener) {
		SessionIdProvider idProvider = new SessionIdProvider();
		NetconfServerSessionNegotiatorFactory serverNegotiatorFactory = new NetconfServerSessionNegotiatorFactory(new HashedWheelTimer(5000, TimeUnit.MILLISECONDS), factoriesListener, idProvider);

		NetconfServerSessionListenerFactory listenerFactory = new NetconfServerSessionListenerFactory(factoriesListener, commitNot, idProvider);

		return new NetconfServerDispatcher(sslC, serverNegotiatorFactory, listenerFactory);
	}

	@After
	public void tearDown() throws Exception {
		commitNot.close();
		dispatch.close();
		dispatchS.close();
	}

	private SSLContext getSslContext() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException,
			UnrecoverableKeyException, KeyManagementException {
		final InputStream keyStore = getClass().getResourceAsStream("/keystore.jks");
		final InputStream trustStore = getClass().getResourceAsStream("/keystore.jks");
		SSLContext sslContext = SSLUtil.initializeSecureContext("password", keyStore, trustStore, "SunX509");
		keyStore.close();
		trustStore.close();
		return sslContext;
	}

	private void loadMessages() throws IOException, SAXException, ParserConfigurationException {
		this.editConfig = XmlFileLoader.xmlFileToNetconfMessage("netconfMessages/edit_config.xml");
		this.getConfig = XmlFileLoader.xmlFileToNetconfMessage("netconfMessages/getConfig.xml");
		this.getConfigCandidate = XmlFileLoader.xmlFileToNetconfMessage("netconfMessages/getConfig_candidate.xml");
		this.closeSession = XmlFileLoader.xmlFileToNetconfMessage("netconfMessages/closeSession.xml");
	}

	private HardcodedYangStoreService getYangStore() throws YangStoreException, IOException {
		final Collection<InputStream> yangDependencies = getBasicYangs();
		return new HardcodedYangStoreService(yangDependencies);
	}

	private Collection<InputStream> getBasicYangs() throws IOException {
		List<String> paths = Arrays.asList("/META-INF/yang/config.yang", "/META-INF/yang/rpc-context.yang",
				"/META-INF/yang/config-test.yang", "/META-INF/yang/config-test-impl.yang", "/META-INF/yang/ietf-inet-types.yang");
		final Collection<InputStream> yangDependencies = new ArrayList<>();
		for (String path : paths) {
			final InputStream is = checkNotNull(getClass().getResourceAsStream(path), path + " not found");
			yangDependencies.add(is);
		}
		return yangDependencies;
	}

	protected List<ModuleFactory> getModuleFactories() {
		return getModuleFactoriesS();
	}

	static List<ModuleFactory> getModuleFactoriesS() {
		return Lists.newArrayList(new TestImplModuleFactory(), new DepTestImplModuleFactory(), new NetconfTestImplModuleFactory());
	}

	@Test
	public void testTwoSessions() throws Exception {
		try (NetconfClient netconfClient = new NetconfClient("1", tcpAddress, 4000)) {
			try (NetconfClient netconfClient2 = new NetconfClient("2", tcpAddress, 4000)) {
			}
		}
	}

	@Test
	public void testSecure() throws Exception {
		try (NetconfClient netconfClient = new NetconfClient("1", tlsAddress, 4000, Optional.of(getSslContext()))) {

		}
	}

	@Ignore
	@Test
	public void waitingTest() throws Exception {
		final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
		transaction.createModule(DepTestImplModuleFactory.NAME, "eb");
		transaction.commit();
		Thread.currentThread().suspend();
	}

	@Test
	public void rpcReplyContainsAllAttributesTest() throws Exception {
		try (NetconfClient netconfClient = createSession(tcpAddress, "1")) {
			final String rpc = "<rpc message-id=\"5\" a=\"a\" b=\"44\" xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">" + "<get/>"
					+ "</rpc>";
			final Document doc = Xml.readXmlToDocument(rpc);
			final NetconfMessage message = netconfClient.sendMessage(new NetconfMessage(doc));
			assertNotNull(message);
			final NamedNodeMap expectedAttributes = doc.getDocumentElement().getAttributes();
			final NamedNodeMap returnedAttributes = message.getDocument().getDocumentElement().getAttributes();

			assertSameAttributes(expectedAttributes, returnedAttributes);
		}
	}

	private void assertSameAttributes(final NamedNodeMap expectedAttributes, final NamedNodeMap returnedAttributes) {
		assertNotNull("Expecting 4 attributes", returnedAttributes);
		assertEquals(expectedAttributes.getLength(), returnedAttributes.getLength());

		for (int i = 0; i < expectedAttributes.getLength(); i++) {
			final Node expAttr = expectedAttributes.item(i);
			final Node attr = returnedAttributes.item(i);
			assertEquals(expAttr.getNodeName(), attr.getNodeName());
			assertEquals(expAttr.getNamespaceURI(), attr.getNamespaceURI());
			assertEquals(expAttr.getTextContent(), attr.getTextContent());
		}
	}

	@Test
	public void rpcReplyErrorContainsAllAttributesTest() throws Exception {
		try (NetconfClient netconfClient = createSession(tcpAddress, "1")) {
			final String rpc = "<rpc message-id=\"1\" a=\"adada\" b=\"4\" xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">" + "<commit/>"
					+ "</rpc>";
			final Document doc = Xml.readXmlToDocument(rpc);
			final NetconfMessage message = netconfClient.sendMessage(new NetconfMessage(doc));
			final NamedNodeMap expectedAttributes = doc.getDocumentElement().getAttributes();
			final NamedNodeMap returnedAttributes = message.getDocument().getDocumentElement().getAttributes();

			assertSameAttributes(expectedAttributes, returnedAttributes);
		}
	}

	@Test
	public void rpcOutputContainsCorrectNamespace() throws Exception {
		final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
		ObjectName dep = transaction.createModule(DepTestImplModuleFactory.NAME, "instanceD");
		ObjectName impl = transaction.createModule(NetconfTestImplModuleFactory.NAME, "instance");
		NetconfTestImplModuleMXBean proxy = configRegistryClient.newMXBeanProxy(impl, NetconfTestImplModuleMXBean.class);
		proxy.setTestingDep(dep);
		registerRuntimeBean();

		transaction.commit();

		try (NetconfClient netconfClient = createSession(tcpAddress, "1")) {
			final String expectedNamespace = "urn:opendaylight:params:xml:ns:yang:controller:test:impl";

			final String rpc = "<rpc message-id=\"5\" xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">" + "<no-arg xmlns=\""
					+ expectedNamespace + "\">    "
					+ "<context-instance>/data/modules/module[name='impl-netconf']/instance[name='instance']</context-instance>"
					+ "<arg1>argument1</arg1>" + "</no-arg>" + "</rpc>";
			final Document doc = Xml.readXmlToDocument(rpc);
			final NetconfMessage message = netconfClient.sendMessage(new NetconfMessage(doc));

			final Element rpcReply = message.getDocument().getDocumentElement();
			final XmlElement resultElement = XmlElement.fromDomElement(rpcReply).getOnlyChildElement();
			assertEquals("result", resultElement.getName());
			final String namespace = resultElement.getNamespaceAttribute();
			assertEquals(expectedNamespace, namespace);
		}
	}

	private void registerRuntimeBean() {
		BaseJMXRegistrator baseJMXRegistrator = new BaseJMXRegistrator(ManagementFactory.getPlatformMBeanServer());
		RootRuntimeBeanRegistratorImpl runtimeBeanRegistrator = baseJMXRegistrator.createRuntimeBeanRegistrator(new ModuleIdentifier(NetconfTestImplModuleFactory.NAME, "instance"));
		NetconfTestImplRuntimeRegistrator reg = new NetconfTestImplRuntimeRegistrator(runtimeBeanRegistrator);
		reg.register(new NetconfTestImplRuntimeMXBean() {
			@Override
			public Asdf getAsdf() {
				return null;
			}

			@Override
			public Long getCreatedSessions() {
				return null;
			}

			@Override
			public String noArg(String arg1) {
				return "from no arg";
			}
		});
	}

	@Test
	public void testCloseSession() throws Exception {
		try (NetconfClient netconfClient = createSession(tcpAddress, "1")) {

			// edit config
			Document rpcReply = netconfClient.sendMessage(this.editConfig).getDocument();
			assertIsOK(rpcReply);

			rpcReply = netconfClient.sendMessage(this.closeSession).getDocument();

			assertIsOK(rpcReply);
		}
	}

	@Test
	public void testEditConfig() throws Exception {
		try (NetconfClient netconfClient = createSession(tcpAddress, "1")) {
			// send edit_config.xml
			final Document rpcReply = netconfClient.sendMessage(this.editConfig).getDocument();
			assertIsOK(rpcReply);
		}
	}

	@Test
	public void testValidate() throws Exception {
		try (NetconfClient netconfClient = createSession(tcpAddress, "1")) {
			// begin transaction
			Document rpcReply = netconfClient.sendMessage(getConfigCandidate).getDocument();
			assertEquals("data", XmlElement.fromDomDocument(rpcReply).getOnlyChildElement().getName());

			// validate empty
			rpcReply = netconfClient.sendMessage(XmlFileLoader.xmlFileToNetconfMessage("netconfMessages/validate.xml")).getDocument();
			assertIsOK(rpcReply);
		}
	}

	private void assertIsOK(final Document rpcReply) {
		assertEquals("rpc-reply", rpcReply.getDocumentElement().getTagName());
		assertEquals("ok", XmlElement.fromDomDocument(rpcReply).getOnlyChildElement().getName());
	}

	@Ignore
	@Test
	// TODO can only send NetconfMessage - it must be valid xml
	public void testClientHelloWithAuth() throws Exception {
		final String fileName = "netconfMessages/client_hello_with_auth.xml";
		// final InputStream resourceAsStream = AbstractListenerTest.class.getResourceAsStream(fileName);
		// assertNotNull(resourceAsStream);
		try (NetconfClient netconfClient = new NetconfClient("test", tcpAddress, 5000)) {
			// IOUtils.copy(resourceAsStream, netconfClient.getStream());
			// netconfClient.getOutputStream().write(NetconfMessageFactory.endOfMessage);
			// server should not write anything back
			// assertEquals(null, netconfClient.readMessage());
			assertGetConfigWorks(netconfClient);
		}
	}

	private Document assertGetConfigWorks(final NetconfClient netconfClient) throws InterruptedException {
		return assertGetConfigWorks(netconfClient, this.getConfig);
	}

	private Document assertGetConfigWorks(final NetconfClient netconfClient, final NetconfMessage getConfigMessage)
			throws InterruptedException {
		final NetconfMessage rpcReply = netconfClient.sendMessage(getConfigMessage);
		assertNotNull(rpcReply);
		assertEquals("data", XmlElement.fromDomDocument(rpcReply.getDocument()).getOnlyChildElement().getName());
		return rpcReply.getDocument();
	}

	@Test
	public void testGetConfig() throws Exception {
		try (NetconfClient netconfClient = createSession(tcpAddress, "1")) {
			assertGetConfigWorks(netconfClient);
		}
	}

	@Test
	public void createYangTestBasedOnYuma() throws Exception {
		try (NetconfClient netconfClient = createSession(tcpAddress, "1")) {
			Document rpcReply = netconfClient.sendMessage(
					XmlFileLoader.xmlFileToNetconfMessage("netconfMessages/editConfig_merge_yang-test.xml")).getDocument();
			assertEquals("rpc-reply", rpcReply.getDocumentElement().getTagName());
			assertIsOK(rpcReply);
			assertGetConfigWorks(netconfClient, this.getConfigCandidate);
			rpcReply = netconfClient.sendMessage(XmlFileLoader.xmlFileToNetconfMessage("netconfMessages/commit.xml")).getDocument();
			assertIsOK(rpcReply);

			final ObjectName on = new ObjectName("org.opendaylight.controller:instanceName=impl-dep-instance,type=Module,moduleFactoryName=impl-dep");
			Set<ObjectName> cfgBeans = configRegistryClient.lookupConfigBeans();
			assertEquals(cfgBeans, Sets.newHashSet(on));
		}
	}

	private NetconfClient createSession(final InetSocketAddress address, final String expected) throws Exception {
		final NetconfClient netconfClient = new NetconfClient("test " + address.toString(), address, 5000);

		assertEquals(expected, Long.toString(netconfClient.getSessionId()));

		return netconfClient;
	}

}
