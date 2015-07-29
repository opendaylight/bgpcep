package org.opendaylight.controller.config.yang.pcep.stateful07.cfg;

import com.google.common.base.Preconditions;
import java.net.InetSocketAddress;
import org.opendaylight.controller.config.api.JmxAttributeValidationException;
import org.opendaylight.protocol.pcep.PCEPCapability;
import org.opendaylight.protocol.pcep.ietf.stateful07.PCEPCapabilityImpl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.open.TlvsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PCEPCapabilityImplModule extends org.opendaylight.controller.config.yang.pcep.stateful07.cfg.AbstractPCEPCapabilityImplModule {
    private static final Logger LOG = LoggerFactory.getLogger(PCEPCapabilityImplModule.class);

    private static final String VALUE_IS_NOT_SET = "value is not set.";

    public PCEPCapabilityImplModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public PCEPCapabilityImplModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
            final PCEPCapabilityImplModule oldModule, final java.lang.AutoCloseable oldInstance) {

        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    protected void customValidation() {
        JmxAttributeValidationException.checkNotNull(getActive(), VALUE_IS_NOT_SET, activeJmxAttribute);
        JmxAttributeValidationException.checkNotNull(getInitiated(), VALUE_IS_NOT_SET, initiatedJmxAttribute);
        JmxAttributeValidationException.checkNotNull(getTriggeredInitialSync(), VALUE_IS_NOT_SET, triggeredInitialSyncJmxAttribute);
        JmxAttributeValidationException.checkNotNull(getTriggeredResync(), VALUE_IS_NOT_SET, triggeredResyncJmxAttribute);
        JmxAttributeValidationException.checkNotNull(getDeltaLspSyncCapability(), VALUE_IS_NOT_SET, deltaLspSyncCapabilityJmxAttribute);
        JmxAttributeValidationException.checkNotNull(getIncludeDbVersion(), VALUE_IS_NOT_SET, includeDbVersionJmxAttribute);
        if ((getActive() || getTriggeredInitialSync() || getTriggeredResync() || getDeltaLspSyncCapability() || getIncludeDbVersion()) && !getStateful()) {
            setStateful(true);
        }
        if ((getTriggeredInitialSync() || getDeltaLspSyncCapability()) && !getIncludeDbVersion()) {
            setIncludeDbVersion(true);
        }
        JmxAttributeValidationException.checkNotNull(getStateful(), VALUE_IS_NOT_SET, statefulJmxAttribute);
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        final PCEPCapabilityImpl capabilityImpl = new PCEPCapabilityImpl(getStateful(), getActive(), getInitiated(),
            getTriggeredInitialSync(), getTriggeredResync(), getDeltaLspSyncCapability(), getIncludeDbVersion());
        return new PCEPCapabilityImplCloseable(capabilityImpl);
    }

    private static final class PCEPCapabilityImplCloseable implements PCEPCapability, AutoCloseable {
        private final PCEPCapabilityImpl innerCapabilityImpl;
        public PCEPCapabilityImplCloseable(final PCEPCapabilityImpl capabilityImpl) {
            this.innerCapabilityImpl = Preconditions.checkNotNull(capabilityImpl);
        }
        @Override
        public void close() {
        }
        @Override
        public void setCapabilityProposal(final InetSocketAddress address, final TlvsBuilder builder) {
            this.innerCapabilityImpl.setCapabilityProposal(address, builder);
        }
    }
}
