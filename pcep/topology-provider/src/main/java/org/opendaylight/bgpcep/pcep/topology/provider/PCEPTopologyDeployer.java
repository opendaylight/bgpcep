package org.opendaylight.bgpcep.pcep.topology.provider;

import java.net.InetAddress;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.bgpcep.programming.spi.InstructionScheduler;
import org.opendaylight.controller.config.yang.pcep.topology.provider.Client;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;

public interface PCEPTopologyDeployer {
    void createTopologyProvider(@Nonnull TopologyId topologyId, @Nonnull final InetAddress address, final int port,
        final short rpcTimeout, @Nullable final List<Client> client, @Nonnull final InstructionScheduler schedulerDependency);

    void removeTopologyProvider(@Nonnull final TopologyId topologyID);
}
