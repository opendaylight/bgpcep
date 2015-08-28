"""Variables file for data loaded from a file.

Stuff like JSON topology outputs and the like
are fairly long, therefore to improve clarity these
are moved out of the testcase file to their own files.
This module then allows the robot framework suite to
read the file contents and access it as values of variables."""
# Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html

__author__ = "Jozef Behran"
__copyright__ = "Copyright(c) 2015, Cisco Systems, Inc."
__license__ = "Eclipse Public License v1.0"
__email__ = "jbehran@cisco.com"

import os
import string


def get_variables(mininet_ip):
    """Return dict of variables keyed by the (dot-less) names of files.

    Directory where data files are located is the same as where this file is located.
    Every dot in file name is replaced by underscore, so that
    name of the variable is not interpreted as attribute access.
    Replacements may create collisions, so detect them."""
    variables = {}
    this_dir = os.path.dirname(os.path.abspath(__file__))
    filename_list = ["empty.json", "filled.json"]
    for file_basename in filename_list:
        variable_name = file_basename.replace('.', '_')
        if variable_name in variables:
            raise KeyError("Variable " + variable_name + " already exists.")
        file_fullname = this_dir + "/" + file_basename
        data_template = string.Template(open(file_fullname).read())
        variables[variable_name] = data_template.substitute({"IP": mininet_ip})
    return variables
