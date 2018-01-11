/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.condition;

import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.BgpMatchConditions;
import org.opendaylight.yangtools.yang.binding.ChildOf;

/**
 * Condition Policy: Check if route matches defined condition.
 */
public interface BgpConditionsPolicy<T extends ChildOf<BgpMatchConditions>> extends ConditionsPolicy<T> {
}