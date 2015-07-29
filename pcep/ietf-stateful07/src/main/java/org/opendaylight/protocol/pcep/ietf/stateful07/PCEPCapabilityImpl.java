package org.opendaylight.protocol.pcep.ietf.stateful07;

import java.net.InetSocketAddress;
import org.opendaylight.protocol.pcep.PCEPCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev150714.Tlvs3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev150714.Tlvs3Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev150714.speaker.entity.id.tlv.SpeakerEntityIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev131126.Stateful1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev131126.Stateful1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.Tlvs1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.Tlvs1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.stateful.capability.tlv.StatefulBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.open.TlvsBuilder;

public class PCEPCapabilityImpl implements PCEPCapability {

    private final boolean stateful, active, instant, triggeredSync, triggeredResync, deltaLspSync, includeDbVersion;

    public PCEPCapabilityImpl(final boolean stateful, final boolean active, final boolean instant,
        final boolean triggeredSync, final boolean triggeredResync, final boolean deltaLspSync, final boolean includeDbVersion){
        this.stateful = stateful;
        this.active = active;
        this.instant = instant;
        this.triggeredSync = triggeredSync;
        this.triggeredResync = triggeredResync;
        this.deltaLspSync = deltaLspSync;
        this.includeDbVersion = includeDbVersion;
    }

    @Override
    public void setCapabilityProposal(final InetSocketAddress address, final TlvsBuilder builder) {
        if (this.stateful) {
            builder.addAugmentation(
                    Tlvs1.class,
                    new Tlvs1Builder()
                        .setStateful( new StatefulBuilder().setLspUpdateCapability(this.active)
                            .addAugmentation(Stateful1.class, new Stateful1Builder().setInitiation(this.instant).build())
                            .addAugmentation(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev150714.Stateful1.class, new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev150714.Stateful1Builder()
                                .setTriggeredInitialSync(this.triggeredSync)
                                .setTriggeredResync(this.triggeredResync)
                                .setDeltaLspSyncCapability(this.deltaLspSync)
                                .setIncludeDbVersion(this.includeDbVersion)
                                .build())
                            .build())
                        .build())
                    .addAugmentation(Tlvs3.class, new Tlvs3Builder() // move to peerProposal Impl
                        .setLspDbVersion(null) // TODO add db version number for peer with this address
                        .setSpeakerEntityId(new SpeakerEntityIdBuilder().setSpeakerEntityIdValue(null).build()) // TODO convert address to byte[]
                        .build())
                    .build();
        }
    }

    public boolean isStateful() {
        return this.stateful;
    }

    public boolean isActive() {
        return this.active;
    }

    public boolean isInstant() {
        return this.instant;
    }

    public boolean isTriggeredSync() {
        return this.triggeredSync;
    }

    public boolean isTriggeredResync() {
        return this.triggeredResync;
    }

    public boolean isDeltaLspSync() {
        return this.deltaLspSync;
    }

    public boolean isIncludeDbVersion() {
        return this.includeDbVersion;
    }
}
