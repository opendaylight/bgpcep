#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# SPDX-License-Identifier: EPL-1.0
##############################################################################
# Copyright (c) 2018 The Linux Foundation and others.
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html
##############################################################################

from docs_conf.conf import *

linkcheck_ignore = [
    'http://localhost',
    # Ignore jenkins because it's often slow to respond.
    'https://jenkins.opendaylight.org/releng',
    'https://jenkins.opendaylight.org/sandbox',
    # The '#' in the path makes sphinx think it's an anchor
    'https://git.opendaylight.org/gerrit/#/admin/projects/releng/builder',
    'https://git.opendaylight.org/gerrit/#/c/',
    'https://git.opendaylight.org/gerrit/gitweb',
    # URL returns a 403 Forbidden
    'https://www.osgi.org',
    # Ignore anchors on github.com because linkcheck fails on them
    '^https?://github.com/.*#',
    # Ignore IETF URLs often not reachable from Jenkins minions
    # because of hosting connectivity issues
    '^https?://tools.ietf.org/html/.*',
]
linkcheck_timeout = 300
