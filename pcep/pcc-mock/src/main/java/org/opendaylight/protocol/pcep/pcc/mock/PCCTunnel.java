/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.pcc.mock;

import io.netty.util.Timeout;
import org.opendaylight.protocol.pcep.pcc.mock.api.LspType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.pcrpt.message.pcrpt.message.reports.Path;

final class PCCTunnel {

    private final byte[] pathName;
    private final LspType type;
    private int delegationHolder;
    private Path lspState;
    private Timeout redelegationTimeout;
    private Timeout stateTimeout;

    public PCCTunnel(final byte[] pathName, final int delegationHolder, final LspType type, final Path lspState) {
        if (pathName != null) {
            this.pathName = pathName.clone();
        } else {
            this.pathName = null;
        }
        this.delegationHolder = delegationHolder;
        this.type = type;
        this.lspState = lspState;
    }

    public byte[] getPathName() {
        return this.pathName;
    }

    public int getDelegationHolder() {
        return this.delegationHolder;
    }

    public void setDelegationHolder(final int delegationHolder) {
        this.delegationHolder = delegationHolder;
    }

    public LspType getType() {
        return this.type;
    }

    public Path getLspState() {
        return this.lspState;
    }

    public void setLspState(final Path lspState) {
        this.lspState = lspState;
    }

    public void setRedelegationTimeout(final Timeout redelegationTimeout) {
        this.redelegationTimeout = redelegationTimeout;
    }

    public void setStateTimeout(final Timeout stateTimeout) {
        this.stateTimeout = stateTimeout;
    }

    public void cancelTimeouts() {
        if (this.redelegationTimeout != null) {
            this.redelegationTimeout.cancel();
        }
        if (this.stateTimeout != null) {
            this.stateTimeout.cancel();
        }
    }
}
