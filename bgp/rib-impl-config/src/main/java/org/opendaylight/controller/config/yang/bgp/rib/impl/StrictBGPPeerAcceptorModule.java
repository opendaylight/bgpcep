package org.opendaylight.controller.config.yang.bgp.rib.impl;
/**
* BGP peer acceptor that permits only one BGP connection between 2 peers.
*/
public class StrictBGPPeerAcceptorModule extends org.opendaylight.controller.config.yang.bgp.rib.impl.AbstractStrictBGPPeerAcceptorModule {
    public StrictBGPPeerAcceptorModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public StrictBGPPeerAcceptorModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.bgp.rib.impl.StrictBGPPeerAcceptorModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        // TODO:implement
        throw new java.lang.UnsupportedOperationException();
    }

}
