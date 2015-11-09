package org.opendaylight.controller.config.yang.bgp.rib.impl;

import com.google.common.base.MoreObjects;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.rib.impl.StrictBGPPeerRegistry;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPPeerRegistry;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionPreferences;
import org.opendaylight.protocol.bgp.rib.impl.spi.ReusableBGPPeer;
import org.opendaylight.protocol.bgp.rib.spi.BGPSessionListener;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Open;

/**
* Registry of BGP peers that allows only one connection per 2 peers
*/
public class StrictBgpPeerRegistryModule extends org.opendaylight.controller.config.yang.bgp.rib.impl.AbstractStrictBgpPeerRegistryModule {
    public StrictBgpPeerRegistryModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier, final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public StrictBgpPeerRegistryModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier, final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, final org.opendaylight.controller.config.yang.bgp.rib.impl.StrictBgpPeerRegistryModule oldModule, final java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        return new GlobalBGPPeerRegistryWrapper(StrictBGPPeerRegistry.GLOBAL);
    }

    // TODO backwards compatibility, peer-registry has to be mandatory attribute for peers
    /**
     * Wrapper for BGPPeerRegistry that prevents from executing close method
      */
    private static final class GlobalBGPPeerRegistryWrapper implements BGPPeerRegistry, AutoCloseable {
        private final StrictBGPPeerRegistry global;

        public GlobalBGPPeerRegistryWrapper(final StrictBGPPeerRegistry global) {
            this.global = global;
        }

        @Override
        public BGPSessionPreferences getPeerPreferences(final IpAddress ip) {
            return this.global.getPeerPreferences(ip);
        }

        @Override
        public BGPSessionListener getPeer(final IpAddress ip, final Ipv4Address sourceId, final Ipv4Address remoteId, final AsNumber asNumber, final Open open)
                throws BGPDocumentedException {
            return this.global.getPeer(ip, sourceId, remoteId, asNumber, open);
        }

        @Override
        public boolean isPeerConfigured(final IpAddress ip) {
            return this.global.isPeerConfigured(ip);
        }

        @Override
        public void removePeer(final IpAddress ip) {
            this.global.removePeer(ip);
        }

        @Override
        public void removePeerSession(final IpAddress ip) {
            if (ip.getIpv6Address() != null) {
                this.global.removePeerSession(new IpAddress(new Ipv6Address(Ipv6Util.getFullForm(ip.getIpv6Address()))));
            } else {
                this.global.removePeerSession(ip);
            }
        }

        @Override
        public void addPeer(final IpAddress ip, final ReusableBGPPeer peer, final BGPSessionPreferences preferences) {
            if (ip.getIpv6Address() != null) {
                this.global.addPeer(new IpAddress(new Ipv6Address(Ipv6Util.getFullForm(ip.getIpv6Address()))), peer, preferences);
            } else {
                this.global.addPeer(ip, peer, preferences);
            }
        }

        @Override
        public void close() {
            // DO nothing, do not close the global instance
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("peers", this.global)
                    .toString();
        }
    }
}
