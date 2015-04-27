/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.pcep.impl;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.management.InstanceAlreadyExistsException;
import javax.management.ObjectName;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.config.api.ValidationException;
import org.opendaylight.controller.config.api.jmx.CommitStatus;
import org.opendaylight.controller.config.manager.impl.AbstractConfigTest;
import org.opendaylight.controller.config.manager.impl.factoriesresolver.HardcodedModuleFactoriesResolver;
import org.opendaylight.controller.config.util.ConfigTransactionJMXClient;
import org.opendaylight.controller.config.yang.netty.threadgroup.NettyThreadgroupModuleFactory;
import org.opendaylight.controller.config.yang.netty.threadgroup.NettyThreadgroupModuleMXBean;
import org.opendaylight.controller.config.yang.pcep.spi.SimplePCEPExtensionProviderContextModuleFactory;
import org.opendaylight.controller.config.yang.pcep.spi.SimplePCEPExtensionProviderContextModuleMXBean;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.impl.rev130627.PathType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.impl.rev130627.StoreType;

public class PCEPDispatcherImplModuleTest extends AbstractConfigTest {

    private static final String INSTANCE_NAME = "pcep-dispatcher-impl";
    private static final String FACTORY_NAME = PCEPDispatcherImplModuleFactory.NAME;

    private static final String THREADGROUP_FACTORY_NAME = NettyThreadgroupModuleFactory.NAME;
    private static final String BOSS_TG_INSTANCE_NAME = "boss-group";
    private static final String WORKER_TG_INSTANCE_NAME = "worker-group";

    private static final String EXTENSION_INSTANCE_NAME = "pcep-extension-privider";
    private static final String EXTENSION_FACTORYNAME = SimplePCEPExtensionProviderContextModuleFactory.NAME;

    private static final String STORE_PASSWORD = "opendaylight";
    private static final String KEYSTORE = "ctl.jks";
    private static final String TRUSTSTORE = "truststore.jks";

    @Before
    public void setUp() throws Exception {
        super.initConfigTransactionManagerImpl(new HardcodedModuleFactoriesResolver(this.mockedContext, new PCEPDispatcherImplModuleFactory(), new PCEPSessionProposalFactoryImplModuleFactory(), new NettyThreadgroupModuleFactory(), new SimplePCEPExtensionProviderContextModuleFactory()));
    }

    @Test
    public void testValidationExceptionMaxUnknownMessagesNotSet() throws Exception {
        try {
            createDispatcherInstance(null);
            fail();
        } catch (final ValidationException e) {
            assertTrue(e.getMessage().contains("MaxUnknownMessages value is not set"));
        }
    }

    @Test
    public void testValidationExceptionMaxUnknownMessagesMinValue() throws Exception {
        try {
            createDispatcherInstance(0);
            fail();
        } catch (final ValidationException e) {
            assertTrue(e.getMessage().contains("must be greater than 0"));
        }
    }

    @Test
    public void testValidationExceptionCertificatePassword() throws Exception {
        try {
            createDispatcherInstance(1, createTls(null, KEYSTORE, STORE_PASSWORD, PathType.PATH, StoreType.JKS, TRUSTSTORE, STORE_PASSWORD,
                    PathType.PATH, StoreType.JKS));
            fail();
        } catch (final ValidationException e) {
            assertTrue(e.getMessage().contains("certificate password"));
        }
    }

    @Test
    public void testValidationExceptionKeystoreLocation() throws Exception {
        try {
            createDispatcherInstance(1, createTls(STORE_PASSWORD, null, STORE_PASSWORD, PathType.PATH, StoreType.JKS, TRUSTSTORE, STORE_PASSWORD,
                    PathType.PATH, StoreType.JKS));
            fail();
        } catch (final ValidationException e) {
            assertTrue(e.getMessage().contains("keystore location"));
        }
    }

    @Test
    public void testValidationExceptionKeystorePassword() throws Exception {
        try {
            createDispatcherInstance(1, createTls(STORE_PASSWORD, KEYSTORE, null, PathType.PATH, StoreType.JKS, TRUSTSTORE, STORE_PASSWORD,
                    PathType.PATH, StoreType.JKS));
            fail();
        } catch (final ValidationException e) {
            assertTrue(e.getMessage().contains("keystore password"));
        }
    }

    @Test
    public void testValidationExceptionKeystorePathType() throws Exception {
        try {
            createDispatcherInstance(1, createTls(STORE_PASSWORD, KEYSTORE, STORE_PASSWORD, null, StoreType.JKS, TRUSTSTORE, STORE_PASSWORD,
                    PathType.PATH, StoreType.JKS));
            fail();
        } catch (final ValidationException e) {
            assertTrue(e.getMessage().contains("keystore path type"));
        }
    }

    @Test
    public void testValidationExceptionKeystoreType() throws Exception {
        try {
            createDispatcherInstance(1, createTls(STORE_PASSWORD, KEYSTORE, STORE_PASSWORD, PathType.PATH, null, TRUSTSTORE, STORE_PASSWORD,
                    PathType.PATH, StoreType.JKS));
            fail();
        } catch (final ValidationException e) {
            assertTrue(e.getMessage().contains("keystore type"));
        }
    }

    @Test
    public void testValidationExceptionTruststoreLocation() throws Exception {
        try {
            createDispatcherInstance(1, createTls(STORE_PASSWORD, KEYSTORE, STORE_PASSWORD, PathType.PATH, StoreType.JKS, null, STORE_PASSWORD,
                    PathType.PATH, StoreType.JKS));
            fail();
        } catch (final ValidationException e) {
            assertTrue(e.getMessage().contains("truststore location"));
        }
    }

    @Test
    public void testValidationExceptionTruststorePassword() throws Exception {
        try {
            createDispatcherInstance(1, createTls(STORE_PASSWORD, KEYSTORE, STORE_PASSWORD, PathType.PATH, StoreType.JKS, TRUSTSTORE, null,
                    PathType.PATH, StoreType.JKS));
            fail();
        } catch (final ValidationException e) {
            assertTrue(e.getMessage().contains("truststore password"));
        }
    }

    @Test
    public void testValidationExceptionTruststorePathType() throws Exception {
        try {
            createDispatcherInstance(1, createTls(STORE_PASSWORD, KEYSTORE, STORE_PASSWORD, PathType.PATH, StoreType.JKS, TRUSTSTORE, STORE_PASSWORD,
                    null, StoreType.JKS));
            fail();
        } catch (final ValidationException e) {
            assertTrue(e.getMessage().contains("truststore path type"));
        }
    }

    @Test
    public void testValidationExceptionTruststoreType() throws Exception {
        try {
            createDispatcherInstance(1, createTls(STORE_PASSWORD, KEYSTORE, STORE_PASSWORD, PathType.PATH, StoreType.JKS, TRUSTSTORE, STORE_PASSWORD,
                    PathType.PATH, null));
            fail();
        } catch (final ValidationException e) {
            assertTrue(e.getMessage().contains("truststore type"));
        }
    }

    @Test
    public void testCreateBean() throws Exception {
        final CommitStatus status = createDispatcherInstance(5);
        assertBeanCount(1, FACTORY_NAME);
        assertStatus(status, 5, 0, 0);
    }

    @Test
    public void testReusingOldInstance() throws Exception {
        createDispatcherInstance(5);
        final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
        assertBeanCount(1, FACTORY_NAME);
        final CommitStatus status = transaction.commit();
        assertBeanCount(1, FACTORY_NAME);
        assertStatus(status, 0, 0, 5);
    }

    @Test
    public void testReconfigure() throws Exception {
        createDispatcherInstance(5);
        final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
        assertBeanCount(1, FACTORY_NAME);
        final PCEPDispatcherImplModuleMXBean mxBean = transaction.newMXBeanProxy(transaction.lookupConfigBean(FACTORY_NAME, INSTANCE_NAME),
                PCEPDispatcherImplModuleMXBean.class);
        mxBean.setMaxUnknownMessages(10);
        final CommitStatus status = transaction.commit();
        assertBeanCount(1, FACTORY_NAME);
        assertStatus(status, 0, 1, 4);
    }

    @Test
    public void testCreateBeanWithTls() throws Exception {
        final CommitStatus status = createDispatcherInstance(5, createTls(STORE_PASSWORD, KEYSTORE, STORE_PASSWORD, PathType.PATH, StoreType.JKS,
                TRUSTSTORE, STORE_PASSWORD, PathType.PATH, StoreType.JKS));
        assertBeanCount(1, FACTORY_NAME);
        assertStatus(status, 5, 0, 0);
    }

    private CommitStatus createDispatcherInstance(final Integer maxUnknownMessages) throws Exception {
        final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
        createDispatcherInstance(transaction, maxUnknownMessages);
        return transaction.commit();
    }

    private CommitStatus createDispatcherInstance(final Integer maxUnknownMessages, final Tls tls) throws Exception {
        final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
        createDispatcherInstance(transaction, maxUnknownMessages, tls);
        return transaction.commit();
    }

    public static ObjectName createDispatcherInstance(final ConfigTransactionJMXClient transaction, final Integer maxUnknownMessages)
            throws Exception {
        final ObjectName nameCreated = transaction.createModule(FACTORY_NAME, INSTANCE_NAME);
        final PCEPDispatcherImplModuleMXBean mxBean = transaction.newMXBeanProxy(nameCreated, PCEPDispatcherImplModuleMXBean.class);
        mxBean.setPcepSessionProposalFactory(PCEPSessionProposalFactoryImplModuleTest.createSessionInstance(transaction));
        mxBean.setMaxUnknownMessages(maxUnknownMessages);
        mxBean.setBossGroup(createThreadGroupInstance(transaction, 10, BOSS_TG_INSTANCE_NAME));
        mxBean.setWorkerGroup(createThreadGroupInstance(transaction, 10, WORKER_TG_INSTANCE_NAME));
        mxBean.setPcepExtensions(createExtensionsInstance(transaction));
        return nameCreated;
    }

    private static ObjectName createDispatcherInstance(final ConfigTransactionJMXClient transaction, final Integer maxUnknownMessages,
            final Tls tls) throws Exception {
        final ObjectName objName = createDispatcherInstance(transaction, maxUnknownMessages);
        final PCEPDispatcherImplModuleMXBean mxBean = transaction.newMXBeanProxy(objName, PCEPDispatcherImplModuleMXBean.class);
        mxBean.setTls(tls);
        return objName;
    }

    private static ObjectName createExtensionsInstance(final ConfigTransactionJMXClient transaction) throws InstanceAlreadyExistsException {
        final ObjectName nameCreated = transaction.createModule(EXTENSION_FACTORYNAME, EXTENSION_INSTANCE_NAME);
        transaction.newMXBeanProxy(nameCreated, SimplePCEPExtensionProviderContextModuleMXBean.class);

        return nameCreated;
    }

    private static ObjectName createThreadGroupInstance(final ConfigTransactionJMXClient transaction, final Integer threadCount,
            final String instanceName) throws InstanceAlreadyExistsException {
        final ObjectName nameCreated = transaction.createModule(THREADGROUP_FACTORY_NAME, instanceName);
        final NettyThreadgroupModuleMXBean mxBean = transaction.newMXBeanProxy(nameCreated, NettyThreadgroupModuleMXBean.class);
        mxBean.setThreadCount(threadCount);
        return nameCreated;
    }

    private static Tls createTls(final String certificatePassword, final String keystore, final String keystorePassword,
            final PathType keystorePathType, final StoreType keystoreType, final String truststore,
            final String truststorePassword, final PathType truststorePathType, final StoreType truststoreType) {
        final Tls tls = new Tls();
        tls.setCertificatePassword(certificatePassword);
        tls.setKeystore(keystore);
        tls.setKeystorePassword(keystorePassword);
        tls.setKeystorePathType(keystorePathType);
        tls.setKeystoreType(keystoreType);
        tls.setTruststore(truststore);
        tls.setTruststorePassword(truststorePassword);
        tls.setTruststorePathType(truststorePathType);
        tls.setTruststoreType(truststoreType);
        return tls;
    }

}
