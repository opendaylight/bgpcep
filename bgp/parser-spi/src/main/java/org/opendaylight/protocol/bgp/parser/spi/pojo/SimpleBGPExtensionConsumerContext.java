/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi.pojo;

import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionConsumerContext;

class SimpleBGPExtensionConsumerContext implements BGPExtensionConsumerContext {
    private final SimpleAddressFamilyRegistry afiReg = new SimpleAddressFamilyRegistry();
    private final SimpleAttributeRegistry attrReg = new SimpleAttributeRegistry();
    private final SimpleCapabilityRegistry capReg = new SimpleCapabilityRegistry();
    private final SimpleMessageRegistry msgReg = new SimpleMessageRegistry();
    private final SimpleSubsequentAddressFamilyRegistry safiReg = new SimpleSubsequentAddressFamilyRegistry();
    private final SimpleParameterRegistry paramReg = new SimpleParameterRegistry();
    private final SimpleNextHopRegistry nextHopReg = new SimpleNextHopRegistry();
    private final SimpleNlriRegistry nlriReg = new SimpleNlriRegistry(this.afiReg, this.safiReg, nextHopReg);

    @Override
    public final SimpleAddressFamilyRegistry getAddressFamilyRegistry() {
        return this.afiReg;
    }

    @Override
    public final SimpleAttributeRegistry getAttributeRegistry() {
        return this.attrReg;
    }

    @Override
    public final SimpleCapabilityRegistry getCapabilityRegistry() {
        return this.capReg;
    }

    @Override
    public final SimpleMessageRegistry getMessageRegistry() {
        return this.msgReg;
    }

    @Override
    public final SimpleNlriRegistry getNlriRegistry() {
        return this.nlriReg;
    }

    @Override
    public final SimpleParameterRegistry getParameterRegistry() {
        return this.paramReg;
    }

    @Override
    public final SimpleSubsequentAddressFamilyRegistry getSubsequentAddressFamilyRegistry() {
        return this.safiReg;
    }

    @Override
    public SimpleNextHopRegistry getNextHopRegistry() {
        return this.nextHopReg;
    }
}
