/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.programming.impl;

/**
 * Instruction Scheduler Deployer
 */
public interface IntructionDeployer {
    /**
     * Write instruction configuration on DS
     * @param instructionId Instruction Scheduler Id
     */
    void writeConfiguration(String instructionId);

    /**
     * Remove instruction configuration on DS
     * @param instructionId Instruction Scheduler Id
     */
    void removeConfiguration(String instructionId);
}
