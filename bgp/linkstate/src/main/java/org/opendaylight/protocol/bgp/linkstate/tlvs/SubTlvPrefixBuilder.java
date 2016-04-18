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
 * Builder interface for creating Descriptor object for Prefix Nlri TLV types.
 */
public interface SubTlvPrefixBuilder {

    void buildPrefixDescriptor (Object subTlvObject, Builder<?> tlvBuilder);
}
