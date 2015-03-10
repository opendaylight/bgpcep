# PCE and its handlers

# Copyright (c) 2012,2013,2015 Cisco Systems, Inc. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html

# Workaround for tox missing parameter 'skipsdist=true' which was not
# introduced until tox 1.6.0

import setuptools

setuptools.setup(name='pcepy')
