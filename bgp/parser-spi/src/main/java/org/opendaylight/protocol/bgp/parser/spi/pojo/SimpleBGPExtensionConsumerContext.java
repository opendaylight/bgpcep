/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi.pojo;

import org.opendaylight.protocol.bgp.parser.spi.AddressFamilyRegistry;
import org.opendaylight.protocol.bgp.parser.spi.AttributeRegistry;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionConsumerContext;
import org.opendaylight.protocol.bgp.parser.spi.CapabilityRegistry;
import org.opendaylight.protocol.bgp.parser.spi.MessageRegistry;
import org.opendaylight.protocol.bgp.parser.spi.NlriRegistry;
import org.opendaylight.protocol.bgp.parser.spi.ParameterRegistry;
import org.opendaylight.protocol.bgp.parser.spi.SubsequentAddressFamilyRegistry;

class SimpleBGPExtensionConsumerContext implements BGPExtensionConsumerContext {
    protected final SimpleAddressFamilyRegistry afiReg = new SimpleAddressFamilyRegistry();
    protected final SimpleAttributeRegistry attrReg = new SimpleAttributeRegistry();
    protected final SimpleCapabilityRegistry capReg = new SimpleCapabilityRegistry();
    protected final SimpleMessageRegistry msgReg = new SimpleMessageRegistry();
    protected final SimpleSubsequentAddressFamilyRegistry safiReg = new SimpleSubsequentAddressFamilyRegistry();
    protected final SimpleParameterRegistry paramReg = new SimpleParameterRegistry();
    protected final SimpleNlriRegistry nlriReg = new SimpleNlriRegistry(this.afiReg, this.safiReg);

    @Override
    public final AddressFamilyRegistry getAddressFamilyRegistry() {
        return this.afiReg;
    }

    @Override
    public final AttributeRegistry getAttributeRegistry() {
        return this.attrReg;
    }

    @Override
    public final CapabilityRegistry getCapabilityRegistry() {
        return this.capReg;
    }

    @Override
    public final MessageRegistry getMessageRegistry() {
        return this.msgReg;
    }

    @Override
    public final NlriRegistry getNlriRegistry() {
        return this.nlriReg;
    }

    @Override
    public final ParameterRegistry getParameterRegistry() {
        return this.paramReg;
    }

    @Override
    public final SubsequentAddressFamilyRegistry getSubsequentAddressFamilyRegistry() {
        return this.safiReg;
    }
}
