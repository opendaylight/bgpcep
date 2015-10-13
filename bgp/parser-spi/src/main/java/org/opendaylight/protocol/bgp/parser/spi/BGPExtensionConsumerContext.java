/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi;

/**
 * A single instance of a collection of extensions for use by consumers. This provides access to the various BGP-related
 * registries. The registries are read-only and are populated by extension producers.
 */
public interface BGPExtensionConsumerContext {
    AddressFamilyRegistry getAddressFamilyRegistry();

    AttributeRegistry getAttributeRegistry();

    CapabilityRegistry getCapabilityRegistry();

    MessageRegistry getMessageRegistry();

    NlriRegistry getNlriRegistry();

    ParameterRegistry getParameterRegistry();

    SubsequentAddressFamilyRegistry getSubsequentAddressFamilyRegistry();

    NextHopRegistry getNextHopRegistry();
}
