/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.bgp.rib.impl;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.util.List;

import javax.management.InstanceAlreadyExistsException;
import javax.management.ObjectName;

import org.junit.Test;
import org.opendaylight.controller.config.api.IdentityAttributeRef;
import org.opendaylight.controller.config.api.ValidationException;
import org.opendaylight.controller.config.api.jmx.CommitStatus;
import org.opendaylight.controller.config.spi.ModuleFactory;
import org.opendaylight.controller.config.util.ConfigTransactionJMXClient;
import org.opendaylight.controller.config.yang.bgp.rib.spi.RIBExtensionsImplModuleFactory;
import org.opendaylight.controller.config.yang.md.sal.binding.impl.DataBrokerImplModuleFactory;
import org.opendaylight.controller.config.yang.md.sal.dom.impl.DomBrokerImplModuleFactory;
import org.opendaylight.controller.config.yang.md.sal.dom.impl.HashMapDataStoreModuleFactory;
import org.opendaylight.controller.config.yang.netty.eventexecutor.GlobalEventExecutorModuleFactory;
import org.opendaylight.controller.config.yang.reconnectstrategy.TimedReconnectStrategyModuleFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.MplsLabeledVpnSubsequentAddressFamily;
import org.opendaylight.yangtools.yang.data.impl.codec.CodecRegistry;
import org.opendaylight.yangtools.yang.data.impl.codec.IdentityCodec;

import com.google.common.collect.Lists;

public class BGPPeerModuleTest extends RIBImplModuleTest {

	private final String instanceName = "bgp-impl1";

	public static ObjectName createBgpPeerInstance(final ConfigTransactionJMXClient transaction,
			final String instanceName, final String host, final Integer port) throws Exception {
		final ObjectName nameCreated = transaction.createModule(BGPPeerModuleFactory.NAME, instanceName);
		final BGPPeerModuleMXBean mxBean = transaction.newMXBeanProxy(nameCreated, BGPPeerModuleMXBean.class);

		// FIXME JMX crashes if union was not created via artificial constructor
		// annotated for JMX as value
		// IpAddress host1 = new IpAddress(new Ipv4Address(host));
		mxBean.setHost(host == null ? null : new IpAddress(host.toCharArray()));
		mxBean.setPort(port==null ? null : new PortNumber(port));
		mxBean.setAdvertizedTable(Lists.newArrayList(createAdvertisedTable(transaction, BGPTableTypeImplModuleFactory.NAME)));
		mxBean.setRib(createInstance(transaction, RIBImplModuleFactory.NAME, "ribImpl",
				DataBrokerImplModuleFactory.NAME, TimedReconnectStrategyModuleFactory.NAME,
				GlobalEventExecutorModuleFactory.NAME, BGPDispatcherImplModuleFactory.NAME,
				RIBExtensionsImplModuleFactory.NAME, DomBrokerImplModuleFactory.NAME,
				HashMapDataStoreModuleFactory.NAME));
		mxBean.setBgpIdentifier(new Ipv4Address("127.0.0.1"));
		return nameCreated;
	}

	private static ObjectName createAdvertisedTable(final ConfigTransactionJMXClient transaction, final String tableTypeModuleName) throws InstanceAlreadyExistsException {
		String instanceName = "table-type";
		final ObjectName nameCreated = transaction.createModule(tableTypeModuleName, instanceName);
		BGPTableTypeImplModuleMXBean mxBean = transaction.newMXBeanProxy(nameCreated, BGPTableTypeImplModuleMXBean.class);

		mxBean.setAfi(new IdentityAttributeRef(Ipv4AddressFamily.QNAME.toString()));
		mxBean.setSafi(new IdentityAttributeRef(MplsLabeledVpnSubsequentAddressFamily.QNAME.toString()));
		return nameCreated;
	}

	@Override
	protected List<ModuleFactory> getModuleFactories() {
		List<ModuleFactory> moduleFactories = super.getModuleFactories();
		moduleFactories.add(new BGPPeerModuleFactory());
		moduleFactories.add(new BGPTableTypeImplModuleFactory());
		return moduleFactories;
	}

	@Test
	public void testValidationExceptionPortNotSet() throws Exception {
		final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
		try {
			createBgpPeerInstance(transaction, this.instanceName, "127.0.0.1", null);
			transaction.validateConfig();
			fail();
		} catch (final ValidationException e) {
			transaction.abortConfig();
			assertTrue(e.getMessage().contains("Port value is not set."));
		}
	}

	@Test
	public void testValidationExceptionHostNotSet() throws Exception {
		final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
		try {
			createBgpPeerInstance(transaction, this.instanceName, null, null);
			transaction.validateConfig();
			fail();
		} catch (final ValidationException e) {
			transaction.abortConfig();
			assertTrue(e.getMessage().contains("Host value is not set."));
		}
	}

	@Override
	@Test
	public void testCreateBean() throws Exception {
		final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
		createBgpPeerInstance(transaction, this.instanceName, "127.0.0.1", 1);
		transaction.validateConfig();
		final CommitStatus status = transaction.commit();
		assertBeanCount(1, BGPPeerModuleFactory.NAME);
		assertStatus(status, 15, 0, 0);
	}

	@Override
	protected CodecRegistry getCodecRegistry() {
		IdentityCodec<?> idCodec = mock(IdentityCodec.class);
		doReturn(Ipv4AddressFamily.class).when(idCodec).deserialize(Ipv4AddressFamily.QNAME);
		doReturn(MplsLabeledVpnSubsequentAddressFamily.class).when(idCodec).deserialize(MplsLabeledVpnSubsequentAddressFamily.QNAME);

		CodecRegistry codecReg = super.getCodecRegistry();
		doReturn(idCodec).when(codecReg).getIdentityCodec();
		return codecReg;
	}

	@Test
	public void testReusingOldInstance() throws Exception {
		ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
		createBgpPeerInstance(transaction, this.instanceName, "127.0.0.1", 1);
		transaction.validateConfig();
		CommitStatus status = transaction.commit();
		transaction = this.configRegistryClient.createTransaction();
		assertBeanCount(1, BGPPeerModuleFactory.NAME);
		status = transaction.commit();
		assertBeanCount(1, BGPPeerModuleFactory.NAME);
		assertStatus(status, 0, 0, 15);
	}

	@Test
	public void testReconfigure() throws Exception {
		ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
		createBgpPeerInstance(transaction, this.instanceName, "127.0.0.1", 1);
		transaction.validateConfig();
		CommitStatus status = transaction.commit();
		transaction = this.configRegistryClient.createTransaction();
		assertBeanCount(1, BGPPeerModuleFactory.NAME);
		final BGPPeerModuleMXBean mxBean = transaction.newMXBeanProxy(
				transaction.lookupConfigBean(BGPPeerModuleFactory.NAME, this.instanceName), BGPPeerModuleMXBean.class);
		mxBean.setPort(new PortNumber(10));
		status = transaction.commit();
		assertBeanCount(1, BGPPeerModuleFactory.NAME);
		assertStatus(status, 0, 1, 14);
	}
}
