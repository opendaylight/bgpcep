package org.opendaylight.controller.config.yang.bgp.topology.provider;

import org.opendaylight.bgpcep.bgp.topology.provider.spi.BgpTopologyDeployer;
import org.opendaylight.bgpcep.bgp.topology.provider.spi.BgpTopologyProvider;
import org.opendaylight.bgpcep.bgp.topology.provider.spi.TopologyReferenceSingletonService;
import org.opendaylight.bgpcep.topology.TopologyReference;
import org.opendaylight.controller.config.yang.bgp.rib.impl.AbstractRIBImplModuleTest;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yangtools.concepts.AbstractRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public abstract class AbstractTopologyBuilderModuleTest extends AbstractRIBImplModuleTest {
    @Override
    public final void setUp() throws Exception {
        super.setUp();
        setupMockService(TopologyReference.class, new TopologyReference() {
            @Override
            public InstanceIdentifier<Topology> getInstanceIdentifier() {
                return null;
            }
        });
        setupMockService(BgpTopologyDeployer.class, new BgpTopologyDeployer() {
            @Override
            public AbstractRegistration registerTopologyProvider(final BgpTopologyProvider topologyBuilder) {
                return null;
            }
            @Override
            public DataBroker getDataBroker() {
                return null;
            }
            @Override
            public AbstractRegistration registerService(final TopologyReferenceSingletonService topologyProviderService) {
                return null;
            }

            @Override
            public void createInstance(final Topology topology) {

            }

            @Override
            public void removeInstance(final Topology topology) {
                return;
            }

            @Override
            public InstanceIdentifier<Topology> getInstanceIdentifier() {
                return null;
            }
        });
    }
}
