/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.bgpcep.pcep.tunnel.provider;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.Ero;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.EroBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.Subobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.SubobjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev171025.FailureType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev171025.OperationResult;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.p2p.rev130819.tunnel.p2p.path.cfg.attributes.ExplicitHops;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.rev130820.ExplicitHops1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.rev130820.SupportingNode1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.node.attributes.SupportingNode;
import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class TunelProgrammingUtil {
    public static final ListenableFuture<OperationResult> RESULT = Futures.immediateFuture(new OperationResult() {
        @Override
        public Class<? extends DataContainer> getImplementedInterface() {
            return OperationResult.class;
        }

        @Override
        public FailureType getFailure() {
            return FailureType.Unsent;
        }

        @Override
        public List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev171025
                .operation.result.Error> getError() {
            return Collections.emptyList();
        }
    });
    private static final Logger LOG = LoggerFactory.getLogger(TunelProgrammingUtil.class);

    private TunelProgrammingUtil() {
        throw new UnsupportedOperationException();
    }

    public static Ero buildEro(final List<ExplicitHops> explicitHops) {
        final EroBuilder b = new EroBuilder();

        if (!explicitHops.isEmpty()) {
            final List<Subobject> subobjs = new ArrayList<>(explicitHops.size());
            for (final ExplicitHops h : explicitHops) {

                final ExplicitHops1 h1 = h.getAugmentation(ExplicitHops1.class);
                if (h1 != null) {
                    final SubobjectBuilder sb = new SubobjectBuilder();
                    sb.fieldsFrom(h1);
                    sb.setLoose(h.isLoose());
                    subobjs.add(sb.build());
                } else {
                    LOG.debug("Ignoring unhandled explicit hop {}", h);
                }
            }
            b.setSubobject(subobjs);
        }
        return b.build();
    }

    public static NodeId supportingNode(final Node node) {
        for (final SupportingNode n : node.getSupportingNode()) {
            final SupportingNode1 n1 = n.getAugmentation(SupportingNode1.class);
            if (n1 != null && n1.getPathComputationClient().isControlling()) {
                return n.getKey().getNodeRef();
            }
        }

        return null;
    }

    public static Optional<Node> sourceNode(final ReadTransaction rt, final InstanceIdentifier<Topology> topology,
            final Link link) throws ReadFailedException {
        return rt.read(LogicalDatastoreType.OPERATIONAL,
                topology.child(Node.class, new NodeKey(link.getSource().getSourceNode()))).checkedGet();
    }
}
