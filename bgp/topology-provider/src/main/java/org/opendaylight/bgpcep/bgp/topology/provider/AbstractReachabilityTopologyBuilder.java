/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.bgp.topology.provider;

import static java.util.Objects.requireNonNull;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.protocol.bgp.rib.RibReference;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.SubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.CNextHop;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv4NextHopCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv6NextHopCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.ipv4.next.hop._case.Ipv4NextHop;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.ipv6.next.hop._case.Ipv6NextHop;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.TopologyTypes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.igp.node.attributes.IgpNodeAttributes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.igp.node.attributes.IgpNodeAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.igp.node.attributes.igp.node.attributes.Prefix;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.igp.node.attributes.igp.node.attributes.PrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.igp.node.attributes.igp.node.attributes.PrefixKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AbstractReachabilityTopologyBuilder<T extends Route> extends AbstractTopologyBuilder<T> {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractReachabilityTopologyBuilder.class);
    private final Map<NodeId, NodeUsage> nodes = new HashMap<>();

    private static final class NodeUsage {
        private final InstanceIdentifier<IgpNodeAttributes> attrId;
        private int useCount = 1;

        NodeUsage(final InstanceIdentifier<IgpNodeAttributes> attrId) {
            this.attrId = requireNonNull(attrId);
        }
    }

    protected AbstractReachabilityTopologyBuilder(final DataBroker dataProvider, final RibReference locRibReference,
            final TopologyId topologyId, final TopologyTypes topologyTypes, final Class<? extends AddressFamily> afi,
            final Class<? extends SubsequentAddressFamily> safi) {
        super(dataProvider, locRibReference, topologyId, topologyTypes, afi, safi);
    }

    private static NodeId advertizingNode(final Attributes attrs) {
        final CNextHop nh = attrs.getCNextHop();
        if (nh == null) {
            LOG.warn("Next hop value is null");
            return null;
        } else if (nh instanceof Ipv4NextHopCase) {
            final Ipv4NextHop ipv4 = ((Ipv4NextHopCase) nh).getIpv4NextHop();

            return new NodeId(ipv4.getGlobal().getValue());
        } else if (nh instanceof Ipv6NextHopCase) {
            final Ipv6NextHop ipv6 = ((Ipv6NextHopCase) nh).getIpv6NextHop();

            return new NodeId(ipv6.getGlobal().getValue());
        } else {
            LOG.warn("Unhandled next hop class {}", nh.getImplementedInterface());
            return null;
        }
    }

    private KeyedInstanceIdentifier<Node, NodeKey> nodeInstanceId(final NodeId ni) {
        return getInstanceIdentifier().child(Node.class, new NodeKey(ni));
    }

    private static <T extends DataObject> T read(final ReadTransaction rt, final InstanceIdentifier<T> id) {
        final Optional<T> optional;
        try {
            optional = rt.read(LogicalDatastoreType.OPERATIONAL, id).get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("Failed to read {}, assuming non-existent", id, e);
            return null;
        }

        return optional.orNull();
    }

    private InstanceIdentifier<IgpNodeAttributes> ensureNodePresent(final ReadWriteTransaction trans, final NodeId ni) {
        final NodeUsage present = this.nodes.get(ni);
        if (present != null) {
            return present.attrId;
        }

        final KeyedInstanceIdentifier<Node, NodeKey> nii = nodeInstanceId(ni);
        final InstanceIdentifier<IgpNodeAttributes> ret = nii.builder().augmentation(Node1.class)
                .child(IgpNodeAttributes.class).build();

        trans.merge(LogicalDatastoreType.OPERATIONAL, nii, new NodeBuilder().setKey(nii.getKey()).setNodeId(ni)
            .addAugmentation(Node1.class, new Node1Builder().setIgpNodeAttributes(
                new IgpNodeAttributesBuilder().setPrefix(Collections.emptyList()).build()).build()).build());

        this.nodes.put(ni, new NodeUsage(ret));
        return ret;
    }

    protected abstract Attributes getAttributes(T value);

    protected abstract IpPrefix getPrefix(T value);

    @Override
    protected final void createObject(final ReadWriteTransaction trans, final InstanceIdentifier<T> id, final T value) {
        final NodeId ni = advertizingNode(getAttributes(value));
        if (ni == null) {
            return;
        }
        final InstanceIdentifier<IgpNodeAttributes> nii = ensureNodePresent(trans, ni);

        final IpPrefix prefix = getPrefix(value);
        final PrefixKey pk = new PrefixKey(prefix);

        trans.put(LogicalDatastoreType.OPERATIONAL,
                nii.child(Prefix.class, pk), new PrefixBuilder().setKey(pk).setPrefix(prefix).build());
    }

    @Override
    protected final void removeObject(final ReadWriteTransaction trans, final InstanceIdentifier<T> id, final T value) {
        if (value == null) {
            LOG.error("Empty before-data received in delete data change notification for instance id {}", id);
            return;
        }

        final NodeId ni = advertizingNode(getAttributes(value));
        if (ni == null) {
            return;
        }
        final NodeUsage present = this.nodes.get(ni);
        Preconditions.checkState(present != null, "Removing prefix from non-existent node %s", present);

        final PrefixKey pk = new PrefixKey(getPrefix(value));
        trans.delete(LogicalDatastoreType.OPERATIONAL, present.attrId.child(Prefix.class, pk));

        /*
         * This is optimization magic: we are reading a list and we want to remove it once it
         * hits zero. We may be in a transaction, so the read is costly, especially since we
         * have just modified the list.
         *
         * Once we have performed the read, though, we can check the number of nodes, and reuse
         * it for that number of removals. Note that since we do not track data and thus have
         * no understanding about the difference between replace and add, we do not ever increase
         * the life of this in createObject().
         */
        present.useCount--;
        if (present.useCount == 0) {
            final IgpNodeAttributes attrs = read(trans, present.attrId);
            if (attrs != null) {
                present.useCount = attrs.getPrefix().size();
                if (present.useCount == 0) {
                    trans.delete(LogicalDatastoreType.OPERATIONAL, nodeInstanceId(ni));
                    this.nodes.remove(ni);
                }
            }
        }
    }

    @Override
    protected void clearTopology() {
        this.nodes.clear();
    }
}
