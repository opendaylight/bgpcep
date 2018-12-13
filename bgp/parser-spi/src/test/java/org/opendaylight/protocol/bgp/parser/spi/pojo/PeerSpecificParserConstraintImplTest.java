/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi.pojo;

import java.util.Collections;
import java.util.Optional;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.bgp.parser.spi.MultiPathSupport;
import org.opendaylight.protocol.bgp.parser.spi.PeerSpecificParserConstraintProvider;

public class PeerSpecificParserConstraintImplTest {

    private static final MultiPathSupport ADD_PATH_CONSTRAINT = MultiPathSupportImpl.createParserMultiPathSupport(
        Collections.emptyList());

    private PeerSpecificParserConstraintProvider constraints;

    @Before
    public void setUp() {
        this.constraints = new PeerSpecificParserConstraintImpl();
    }

    @Test
    public void testProviderSucess() {
        Assert.assertTrue(this.constraints.addPeerConstraint(MultiPathSupport.class, ADD_PATH_CONSTRAINT));
    }

    @Test
    public void testProviderAlreadyPresent() {
        this.constraints.addPeerConstraint(MultiPathSupport.class, ADD_PATH_CONSTRAINT);
        Assert.assertFalse(this.constraints.addPeerConstraint(MultiPathSupport.class, ADD_PATH_CONSTRAINT));
    }

    @Test(expected = NullPointerException.class)
    public void testProviderNullInput() {
        this.constraints.addPeerConstraint(MultiPathSupport.class, null);
    }

    @Test
    public void testGetPeerConstraintSuccess() {
        this.constraints.addPeerConstraint(MultiPathSupport.class, ADD_PATH_CONSTRAINT);
        final Optional<MultiPathSupport> peerConstraint = this.constraints.getPeerConstraint(MultiPathSupport.class);
        Assert.assertTrue(peerConstraint.isPresent());
    }

    @Test
    public void testGetPeerConstraintNonExisting() {
        final Optional<MultiPathSupport> peerConstraint = this.constraints.getPeerConstraint(MultiPathSupport.class);
        Assert.assertFalse(peerConstraint.isPresent());
    }
}
