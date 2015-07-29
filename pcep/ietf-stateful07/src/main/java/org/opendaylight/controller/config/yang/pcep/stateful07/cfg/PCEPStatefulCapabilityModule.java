package org.opendaylight.controller.config.yang.pcep.stateful07.cfg;

import com.google.common.base.Preconditions;
import java.net.InetSocketAddress;
import org.opendaylight.controller.config.api.JmxAttributeValidationException;
import org.opendaylight.protocol.pcep.PCEPCapability;
import org.opendaylight.protocol.pcep.ietf.stateful07.PCEPStatefulCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.open.TlvsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PCEPStatefulCapabilityModule extends org.opendaylight.controller.config.yang.pcep.stateful07.cfg.AbstractPCEPStatefulCapabilityModule {
    private static final Logger LOG = LoggerFactory.getLogger(PCEPStatefulCapabilityModule.class);

    private static final String VALUE_IS_NOT_SET = "value is not set.";

    public PCEPStatefulCapabilityModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public PCEPStatefulCapabilityModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
            final PCEPStatefulCapabilityModule oldModule, final java.lang.AutoCloseable oldInstance) {

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
        final PCEPStatefulCapability capabilityImpl = new PCEPStatefulCapability(getStateful(), getActive(), getInitiated(),
            getTriggeredInitialSync(), getTriggeredResync(), getDeltaLspSyncCapability(), getIncludeDbVersion());
        return new PCEPCapabilityImplCloseable(capabilityImpl);
    }

    private static final class PCEPCapabilityImplCloseable implements PCEPCapability, AutoCloseable {
        private final PCEPStatefulCapability innerCapabilityImpl;
        public PCEPCapabilityImplCloseable(final PCEPStatefulCapability capabilityImpl) {
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
