package org.opendaylight.controller.config.yang.bgp.rib.impl;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import javax.management.InstanceAlreadyExistsException;
import javax.management.ObjectName;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.config.api.ConflictingVersionException;
import org.opendaylight.controller.config.api.IdentityAttributeRef;
import org.opendaylight.controller.config.api.ValidationException;
import org.opendaylight.controller.config.api.jmx.CommitStatus;
import org.opendaylight.controller.config.manager.impl.AbstractConfigTest;
import org.opendaylight.controller.config.manager.impl.factoriesresolver.HardcodedModuleFactoriesResolver;
import org.opendaylight.controller.config.util.ConfigTransactionJMXClient;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.MplsLabeledVpnSubsequentAddressFamily;
import org.opendaylight.yangtools.yang.data.impl.codec.CodecRegistry;
import org.opendaylight.yangtools.yang.data.impl.codec.IdentityCodec;

public class BGPTableTypeImplModuleTest extends AbstractConfigTest {

    private static final String INSTANCE_NAME = "bgp-table-type-impl";
    private static final String FACTORY_NAME = BGPTableTypeImplModuleFactory.NAME;

    private IdentityAttributeRef afiRef = new IdentityAttributeRef(Ipv4AddressFamily.QNAME.toString());
    private IdentityAttributeRef safiRef = new IdentityAttributeRef(MplsLabeledVpnSubsequentAddressFamily.QNAME.toString());

    @Before
    public void setUp() throws Exception {
        super.initConfigTransactionManagerImpl(new HardcodedModuleFactoriesResolver(mockedContext, new BGPTableTypeImplModuleFactory()));
    }

    @Override
    protected CodecRegistry getCodecRegistry() {
        IdentityCodec<?> idCodec = mock(IdentityCodec.class);
        doReturn(Ipv4AddressFamily.class).when(idCodec).deserialize(Ipv4AddressFamily.QNAME);
        doReturn(MplsLabeledVpnSubsequentAddressFamily.class).when(idCodec).deserialize(MplsLabeledVpnSubsequentAddressFamily.QNAME);
        doReturn(Ipv6AddressFamily.class).when(idCodec).deserialize(Ipv6AddressFamily.QNAME);

        CodecRegistry codecReg = super.getCodecRegistry();
        doReturn(idCodec).when(codecReg).getIdentityCodec();
        return codecReg;
    }

    @Test
    public void testValidationExceptionAfiNotSet() throws InstanceAlreadyExistsException, ConflictingVersionException {
        try {
            createInstance(null, safiRef);
            fail();
        } catch (ValidationException e) {
            assertTrue(e.getMessage().contains("Afi value is not set."));
        }
    }

    @Test
    public void testValidationExceptionSafiNotSet() throws InstanceAlreadyExistsException, ConflictingVersionException {
        try {
            createInstance(afiRef, null);
            fail();
        } catch (ValidationException e) {
            assertTrue(e.getMessage().contains("Safi value is not set."));
        }
    }

    @Test
    public void testCreateBean() throws Exception {
        final CommitStatus status = createInstance();
        assertBeanCount(1, FACTORY_NAME);
        assertStatus(status, 1, 0, 0);
    }

    @Test
    public void testReusingOldInstance() throws Exception {
        createInstance();
        final ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();
        assertBeanCount(1, FACTORY_NAME);
        final CommitStatus status = transaction.commit();
        assertBeanCount(1, FACTORY_NAME);
        assertStatus(status, 0, 0, 1);
    }

    @Test
    public void testReconfigure() throws Exception {
        createInstance();
        final ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();
        assertBeanCount(1, FACTORY_NAME);
        final BGPTableTypeImplModuleMXBean mxBean = transaction.newMXBeanProxy(transaction.lookupConfigBean(FACTORY_NAME, INSTANCE_NAME),
                BGPTableTypeImplModuleMXBean.class);
        mxBean.setAfi(new IdentityAttributeRef(Ipv6AddressFamily.QNAME.toString()));
        final CommitStatus status = transaction.commit();
        assertBeanCount(1, FACTORY_NAME);
        assertStatus(status, 0, 1, 0);
    }

    private CommitStatus createInstance() throws Exception {
        return createInstance(afiRef, safiRef);
    }

    private CommitStatus createInstance(final IdentityAttributeRef afiRef, final IdentityAttributeRef safiRef)
            throws ConflictingVersionException, ValidationException, InstanceAlreadyExistsException {
        ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();
        createTableInstance(transaction, afiRef, safiRef);
        return transaction.commit();
    }

    public static ObjectName createTableInstance(final ConfigTransactionJMXClient transaction, final IdentityAttributeRef afiRef,
            final IdentityAttributeRef safiRef) throws InstanceAlreadyExistsException {
        final ObjectName nameCreated = transaction.createModule(FACTORY_NAME, INSTANCE_NAME);
        BGPTableTypeImplModuleMXBean mxBean = transaction.newMXBeanProxy(nameCreated, BGPTableTypeImplModuleMXBean.class);

        mxBean.setAfi(afiRef);
        mxBean.setSafi(safiRef);
        return nameCreated;
    }

}
