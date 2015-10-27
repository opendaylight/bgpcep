"""Utility for running several play.py utilities in parallel.

Only a subset of play.py functionality is fully supported.
Notably, listening play.py is not tested yet.

Only one hardcoded strategy is there to distinguish peers.
File play.py is to be present in current working directory.

It needs to be run with sudo-able user when you want to use ports below 1024
as --myip.
"""

# Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html

__author__ = "Vratko Polak"
__copyright__ = "Copyright(c) 2015, Cisco Systems, Inc."
__license__ = "Eclipse Public License v1.0"
__email__ = "vrpolak@cisco.com"


import argparse
import ipaddr
import multiprocessing
import subprocess


def main():
    """Use argparse to get arguments, return mgr_args object."""

    parser = argparse.ArgumentParser()
    # TODO: Should we use --argument-names-with-spaces?
    str_help = "Autonomous System number use in the stream (current default as in ODL: 64496)."
    parser.add_argument("--asnumber", default=64496, type=int, help=str_help)
    # FIXME: We are acting as iBGP peer, we should mirror AS number from peer's open message.
    str_help = "Amount of IP prefixes to generate. Negative number is taken as an overflown positive."
    parser.add_argument("--amount", default="1", type=int, help=str_help)
    str_help = "The first IPv4 prefix to announce, given as numeric IPv4 address."
    parser.add_argument("--firstprefix", default="8.0.1.0", type=ipaddr.IPv4Address, help=str_help)
    str_help = "If present, this tool will be listening for connection, instead of initiating it."
    parser.add_argument("--listen", action="store_true", help=str_help)
    str_help = "How many play.py utilities are to be started."
    parser.add_argument("--multiplicity", default="1", type=int, help=str_help)
    str_help = "Numeric IP Address to bind to and derive BGP ID from, for the first player."
    parser.add_argument("--myip", default="0.0.0.0", type=ipaddr.IPv4Address, help=str_help)
    str_help = "TCP port to bind to when listening or initiating connection."
    parser.add_argument("--myport", default="0", type=int, help=str_help)
    str_help = "The IP of the next hop to be placed into the update messages."
    parser.add_argument("--nexthop", default="192.0.2.1", type=ipaddr.IPv4Address, dest="nexthop", help=str_help)
    str_help = "Numeric IP Address to try to connect to. Currently no effect in listening mode."
    parser.add_argument("--peerip", default="127.0.0.2", type=ipaddr.IPv4Address, help=str_help)
    str_help = "TCP port to try to connect to. No effect in listening mode."
    parser.add_argument("--peerport", default="179", type=int, help=str_help)
    # TODO: The step between IP prefixes is currently hardcoded to 16. Should we make it configurable?
    # Yes, the argument list above is sorted alphabetically.
    mgr_args = parser.parse_args()
    # TODO: Are sanity checks (such as asnumber>=0) required?

    if mgr_args.multiplicity < 1:
        print "Multiplicity", mgr_args.multiplicity, "is not positive."
        raise SystemExit(1)
    amount_left = mgr_args.amount
    utils_left = mgr_args.multiplicity
    prefix_current = mgr_args.firstprefix
    myip_current = mgr_args.myip
    processes = []
    while 1:
        amount_per_util = (amount_left - 1) / utils_left + 1  # round up
        amount_left -= amount_per_util
        utils_left -= 1
        util_args = ["python", "play.py"]
        util_args.extend(["--asnumber", str(mgr_args.asnumber)])
        util_args.extend(["--amount", str(amount_per_util)])
        util_args.extend(["--firstprefix", str(prefix_current)])
        if mgr_args.listen:
            util_args.append("--listen")
        util_args.extend(["--myip", str(myip_current)])
        util_args.extend(["--myport", str(mgr_args.myport)])
        util_args.extend(["--nexthop", str(mgr_args.nexthop)])
        util_args.extend(["--peerip", str(mgr_args.peerip)])
        util_args.extend(["--peerport", str(mgr_args.peerport)])
        process = multiprocessing.Process(target=subprocess.call, args=[util_args])
        # No shell, inherited std* file descriptors, same process group, so gets SIGINT.
        processes.append(process)
        if not utils_left:
            break
        prefix_current += amount_per_util * 16
        myip_current += 1
    for process in processes:
        process.start()
    # Usually processes run until SIGINT, but if an error happens we want to exit quickly.
    for process in processes:
        process.join()


if __name__ == "__main__":
    main()
