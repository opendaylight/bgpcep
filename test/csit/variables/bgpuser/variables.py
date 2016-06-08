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

import os
import string


__author__ = "Jozef Behran"
__copyright__ = "Copyright(c) 2015, Cisco Systems, Inc."
__license__ = "Eclipse Public License v1.0"
__email__ = "jbehran@cisco.com"


def _get_stream_specific_dict(mininet_ip, stream):
    """Returns the dict which will be used for the data substitution."""
    def _get_topology_types(stream):
        if stream in ['stable-lithium', 'beryllium']:
            return '{}'
        else:
            return '{"odl-bgp-topology-types:bgp-ipv4-reachability-topology": {}}'

    subs_dict = {"IP": mininet_ip}
    subs_dict["TOPOLOGY_TYPES"] = _get_topology_types(stream)
    return subs_dict


def get_variables(mininet_ip, stream):
    """Return dict of variables keyed by the (dot-less) names of files.

    Directory where data files are located is the same as where this file is located.
    Every dot in file name is replaced by underscore, so that
    name of the variable is not interpreted as attribute access.
    Replacements may create collisions, so detect them."""
    variables = {}
    subs_dict = _get_stream_specific_dict(mininet_ip, stream)
    this_dir = os.path.dirname(os.path.abspath(__file__))
    filename_list = ["empty.json", "filled.json"]
    for file_basename in filename_list:
        variable_name = file_basename.replace('.', '_')
        if variable_name in variables:
            raise KeyError("Variable " + variable_name + " already exists.")
        file_fullname = this_dir + "/" + file_basename
        data_template = string.Template(open(file_fullname).read())
        variables[variable_name] = data_template.substitute(subs_dict)
    return variables
