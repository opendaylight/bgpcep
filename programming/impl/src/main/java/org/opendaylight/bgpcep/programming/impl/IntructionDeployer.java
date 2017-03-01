/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.programming.impl;

import javax.annotation.Nonnull;

/**
 * Instruction Scheduler Deployer
 */
public interface IntructionDeployer {
    /**
     * Creates Instruction Scheduler
     * @param instructionId Instruction Scheduler Id
     */
    void createInstruction(@Nonnull String instructionId);

    /**
     * Remove Instruction Scheduler
     * @param instructionId Instruction Scheduler Id
     */
    void removeInstruction(@Nonnull String instructionId);
}
