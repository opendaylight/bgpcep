/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.bgp.topology.provider;

import com.google.common.base.Optional;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.protocol.bgp.rib.RibReference;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.route.Attributes;
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
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.TopologyTypesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.igp.node.attributes.IgpNodeAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.igp.node.attributes.igp.node.attributes.Prefix;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.igp.node.attributes.igp.node.attributes.PrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.igp.node.attributes.igp.node.attributes.PrefixKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
abstract class AbstractReachabilityTopologyBuilder<T extends Route> extends AbstractTopologyBuilder<T> {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractReachabilityTopologyBuilder.class);

    protected AbstractReachabilityTopologyBuilder(final DataBroker dataProvider, final RibReference locRibReference,
            final TopologyId topologyId, final Class<T> idClass) {
        super(dataProvider, locRibReference, topologyId, new TopologyTypesBuilder().build(), idClass);
    }

    private NodeId advertizingNode(final Attributes attrs) {
        final CNextHop nh = attrs.getCNextHop();
        if (nh instanceof Ipv4NextHopCase) {
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

    private static <T extends DataObject> T read(final ReadTransaction t, final InstanceIdentifier<T> id) {
        final Optional<T> o;
        try {
            o = t.read(LogicalDatastoreType.OPERATIONAL, id).get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("Failed to read {}, assuming non-existent", id, e);
            return null;
        }

        return o.orNull();
    }

    private InstanceIdentifier<Node1> ensureNodePresent(final ReadWriteTransaction trans, final NodeId ni) {
        final KeyedInstanceIdentifier<Node, NodeKey> nii = nodeInstanceId(ni);
        LOG.debug("Looking for pre-existing node at {}", nii);

        final InstanceIdentifier<Node1> ret = nii.augmentation(Node1.class);
        if (read(trans, ret) == null) {
            LOG.debug("Create a new node at {}", nii);
            trans.put(LogicalDatastoreType.OPERATIONAL, nii, new NodeBuilder().setKey(nii.getKey()).setNodeId(ni)
                .addAugmentation(Node1.class, new Node1Builder().setIgpNodeAttributes(
                    new IgpNodeAttributesBuilder().setPrefix(new ArrayList<Prefix>()).build()).build()).build());
        }

        return ret;
    }

    private void removeEmptyNode(final ReadWriteTransaction trans, final InstanceIdentifier<Node> nii) {
        final Node1 node = read(trans, nii.augmentation(Node1.class));
        if (node != null && node.getIgpNodeAttributes().getPrefix().isEmpty()) {
            trans.delete(LogicalDatastoreType.OPERATIONAL, nii);
        }
    }

    protected abstract Attributes getAttributes(final T value);

    protected abstract IpPrefix getPrefix(final T value);

    @Override
    protected final void createObject(final ReadWriteTransaction trans, final InstanceIdentifier<T> id, final T value) {
        final NodeId ni = advertizingNode(getAttributes(value));
        final InstanceIdentifier<Node1> nii = ensureNodePresent(trans, ni);

        final IpPrefix prefix = getPrefix(value);
        final PrefixKey pk = new PrefixKey(prefix);

        trans.put(LogicalDatastoreType.OPERATIONAL,
                nii.child(
                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.igp.node.attributes.IgpNodeAttributes.class).child(
                                Prefix.class, pk), new PrefixBuilder().setKey(pk).setPrefix(prefix).build());
    }

    @Override
    protected final void removeObject(final ReadWriteTransaction trans, final InstanceIdentifier<T> id, final T value) {
        final NodeId ni = advertizingNode(getAttributes(value));
        final InstanceIdentifier<Node> nii = nodeInstanceId(ni);

        final IpPrefix prefix = getPrefix(value);
        final PrefixKey pk = new PrefixKey(prefix);

        trans.delete(LogicalDatastoreType.OPERATIONAL, nii.augmentation(Node1.class).child(
                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.igp.node.attributes.IgpNodeAttributes.class).child(
                        Prefix.class, pk));

        removeEmptyNode(trans, nii);
    }

}
