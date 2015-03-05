package org.opendaylight.protocol.bgp.rib.impl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerRole;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

final class PeerPolicyTracker extends AbstractPeerRoleTracker {
    private final Map<PeerId, AbstractImportPolicy> policies = new ConcurrentHashMap<>();

    protected PeerPolicyTracker(final DOMDataTreeChangeService service, final YangInstanceIdentifier ribId) {
        super(service, ribId);
    }

    @Override
    protected void peerRoleChanged(final PeerId peer, final PeerRole role) {
        if (role != null) {
            // Lookup policy based on role
            final AbstractImportPolicy policy = AbstractImportPolicy.forRole(role);

            // Update lookup map
            policies.put(peer, policy);
        } else {
            policies.remove(peer);
        }
    }

    AbstractImportPolicy policyFor(final PeerId peerId) {
        return policies.get(peerId);
    }
}