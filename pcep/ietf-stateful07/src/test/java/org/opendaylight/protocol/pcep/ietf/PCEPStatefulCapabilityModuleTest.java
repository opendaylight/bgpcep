package org.opendaylight.protocol.pcep.ietf;

import static org.junit.Assert.assertFalse;
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
import org.opendaylight.controller.config.yang.pcep.stateful07.cfg.PCEPStatefulCapabilityModuleFactory;
import org.opendaylight.controller.config.yang.pcep.stateful07.cfg.PCEPStatefulCapabilityModuleMXBean;

public class PCEPStatefulCapabilityModuleTest extends AbstractConfigTest {

    private static final String INSTANCE_NAME = "stateful-capability";
    private static final String FACTORY_NAME = PCEPStatefulCapabilityModuleFactory.NAME;

    @Before
    public void setUp() throws Exception {
        super.initConfigTransactionManagerImpl(new HardcodedModuleFactoriesResolver(this.mockedContext, new PCEPStatefulCapabilityModuleFactory()));
    }

    @Test
    public void testValidationExceptionStatefulNotSet() throws Exception {
        try {
            createInstance(null, false, false, false, false, false, false);
            fail();
        } catch (final ValidationException e) {
            assertTrue(e.getMessage().contains("Stateful value is not set"));
        }
    }

    @Test
    public void testValidationExceptionActiveNotSet() throws Exception {
        try {
            createInstance(true, null, true, true, true, true, true);
            fail();
        } catch (final ValidationException e) {
            assertTrue(e.getMessage().contains("Active value is not set"));
        }
    }

    @Test
    public void testValidationExceptionInstantiatedNotSet() throws Exception {
        try {
            createInstance(true, true, null, true, true, true, true);
            fail();
        } catch (final ValidationException e) {
            assertTrue(e.getMessage().contains("Initiated value is not set"));
        }
    }

    @Test
    public void testValidationExceptionTriggeredSyncNotSet() throws Exception {
        try {
            createInstance(true, true, true, null, true, true, true);
            fail();
        } catch (final ValidationException e) {
            assertTrue(e.getMessage().contains("TriggeredInitialSync value is not set"));
        }
    }

    @Test
    public void testValidationExceptionTriggeredResyncNotSet() throws Exception {
        try {
            createInstance(true, true, true, true, null, true, true);
            fail();
        } catch (final ValidationException e) {
            assertTrue(e.getMessage().contains("TriggeredResync value is not set"));
        }
    }

    @Test
    public void testValidationExceptionDeltaLspSyncNotSet() throws Exception {
        try {
            createInstance(true, true, true, true, true, null, true);
            fail();
        } catch (final ValidationException e) {
            assertTrue(e.getMessage().contains("DeltaLspSyncCapability value is not set"));
        }
    }

    @Test
    public void testValidationExceptionIncludeDBVersionNotSet() throws Exception {
        try {
            createInstance(true, true, true, true, true, true, null);
            fail();
        } catch (final ValidationException e) {
            assertTrue(e.getMessage().contains("IncludeDbVersion value is not set"));
        }
    }

    @Test
    public void testStatefulAfterCommitted() throws Exception {
        createInstance(false, true, true, true, false, true, true);
        final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
        final PCEPStatefulCapabilityModuleMXBean mxBean = transaction.newMXBeanProxy(transaction.lookupConfigBean(
                FACTORY_NAME, INSTANCE_NAME), PCEPStatefulCapabilityModuleMXBean.class);
        assertTrue(mxBean.getStateful());
    }

    @Test
    public void testNotStatefulAfterCommitted() throws Exception {
        createInstance(false, false, false, false, false, false, false);
        final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
        final PCEPStatefulCapabilityModuleMXBean mxBean = transaction.newMXBeanProxy(transaction.lookupConfigBean(
                FACTORY_NAME, INSTANCE_NAME), PCEPStatefulCapabilityModuleMXBean.class);
        assertFalse(mxBean.getStateful());
    }

    @Test
    public void testIncludeDbVersionAfterCommitted() throws Exception {
        createInstance(false, false, false, false, false, true, false);
        final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
        final PCEPStatefulCapabilityModuleMXBean mxBean = transaction.newMXBeanProxy(transaction.lookupConfigBean(
                FACTORY_NAME, INSTANCE_NAME), PCEPStatefulCapabilityModuleMXBean.class);
        assertTrue(mxBean.getIncludeDbVersion());
    }

    @Test
    public void testNotIncludeDbVersionAfterCommitted() throws Exception {
        createInstance(false, false, false, false, false, false, false);
        final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
        final PCEPStatefulCapabilityModuleMXBean mxBean = transaction.newMXBeanProxy(transaction.lookupConfigBean(
                FACTORY_NAME, INSTANCE_NAME), PCEPStatefulCapabilityModuleMXBean.class);
        assertFalse(mxBean.getIncludeDbVersion());
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
        final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
        assertBeanCount(1, FACTORY_NAME);
        final CommitStatus status = transaction.commit();
        assertBeanCount(1, FACTORY_NAME);
        assertStatus(status, 0, 0, 1);
    }

    @Test
    public void testReconfigure() throws Exception {
        createInstance();
        final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
        assertBeanCount(1, FACTORY_NAME);
        transaction.newMXBeanProxy(transaction.lookupConfigBean(FACTORY_NAME, INSTANCE_NAME),
            PCEPStatefulCapabilityModuleMXBean.class);
        final CommitStatus status = transaction.commit();
        assertBeanCount(1, FACTORY_NAME);
        assertStatus(status, 0, 0, 1);
    }

    private CommitStatus createInstance(final Boolean stateful, final Boolean active, final Boolean instant,
        final Boolean triggeredInitialSync, final Boolean triggeredResync, final Boolean deltaLspSyncCapability,
        final Boolean includeDbVersion) throws Exception {
        final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
        createInstance(transaction, stateful, active, instant, triggeredInitialSync, triggeredResync, deltaLspSyncCapability, includeDbVersion);
        return transaction.commit();
    }

    private CommitStatus createInstance() throws Exception {
        final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
        createPCEPCapabilityInstance(transaction);
        return transaction.commit();
    }

    public static ObjectName createPCEPCapabilityInstance(final ConfigTransactionJMXClient transaction) throws Exception {
        return createInstance(transaction, true, true, true, true, true, true, true);
    }

    private static ObjectName createInstance(final ConfigTransactionJMXClient transaction, final Boolean stateful, final Boolean active, final Boolean instant,
            final Boolean triggeredInitialSync, final Boolean triggeredResync, final Boolean deltaLspSyncCapability, final Boolean includeDbVersion) throws InstanceAlreadyExistsException {
        final ObjectName nameCreated = transaction.createModule(FACTORY_NAME, INSTANCE_NAME);
        final PCEPStatefulCapabilityModuleMXBean mxBean = transaction.newMXBeanProxy(nameCreated,
            PCEPStatefulCapabilityModuleMXBean.class);
        mxBean.setActive(active);
        mxBean.setInitiated(instant);
        mxBean.setStateful(stateful);
        mxBean.setTriggeredInitialSync(triggeredInitialSync);
        mxBean.setTriggeredResync(triggeredResync);
        mxBean.setDeltaLspSyncCapability(deltaLspSyncCapability);
        mxBean.setIncludeDbVersion(includeDbVersion);
        return nameCreated;
    }
}
