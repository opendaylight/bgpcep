/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.tlvs;

import org.opendaylight.yangtools.concepts.Builder;

/**
 * Builder interface for creating Node Descriptor object for Link Nlri TLV types.
 */
public interface SubTlvLinkDescBuilder {

    void buildLocalNodeDescriptor (Object subTlvObject, Builder<?> tlvBuilder);

    void buildRemoteNodeDescriptor (Object subTlvObject, Builder<?> tlvBuilder);
}
