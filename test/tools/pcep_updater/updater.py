"""Multithreaded utility for rapid LSP updating.

This utility updates LSPs whose name correspond
to the ones created by pcc-mock tool.
The new state is hardcoded to contain one configurable hop
and final hop of "1.1.1.1/32".
AuthStandalone library is used to handle session and restconf authentication.

Number of workers is configurable, each worker
issues blocking restconf requests.
Work is distributed beforehand in round-robin fashion.
The utility waits for the last worker to finish, or for time to run off.

The responses are checked for status and content,
results are written to collections.Counter and printed at exit.
If collections does not contain Counter, "import Counter" is attempted.

It is advised to pin the python process to single CPU for optimal performance,
as Global Interpreter Lock prevents true utilization on more CPUs
(while overhead of context switching remains).

Remark: For early implementations, master process CPU suffered from overhead
of Queue suitable for multiprocessing, which put performance down
even when workers had more CPU for them.
But that may not be true for more mature implementation.
"""

# Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html

import argparse
import collections  # For deque and Counter.
import ipaddr
import threading
import time

from collections import Counter
import AuthStandalone


__author__ = "Vratko Polak"
__copyright__ = "Copyright(c) 2015, Cisco Systems, Inc."
__license__ = "Eclipse Public License v1.0"
__email__ = "vrpolak@cisco.com"


def str2bool(text):
    """Utility converter, based on http://stackoverflow.com/a/19227287"""
    return text.lower() in ("yes", "true", "y", "t", "1")


# Note: JSON data contains '"', so using "'" to quote Pythons strings.
parser = argparse.ArgumentParser()
parser.add_argument("--pccs", default="1", type=int, help="number of PCCs to simulate")
parser.add_argument(
    "--lsps", default="1", type=int, help="number of LSPs pre PCC to update"
)
parser.add_argument(
    "--workers", default="1", type=int, help="number of blocking https threads to use"
)
parser.add_argument(
    "--hop", default="2.2.2.2/32", help="ipv4 prefix (including /32) of hop to use"
)
parser.add_argument(
    "--timeout", default="300", type=float, help="seconds to bail out after"
)  # FIXME: grammar
parser.add_argument(
    "--refresh",
    default="0.1",
    type=float,
    help="seconds to sleep in main thread if nothing to do",
)
parser.add_argument(
    "--pccaddress", default="127.0.0.1", help="IP address of the first simulated PCC"
)
parser.add_argument(
    "--odladdress", default="127.0.0.1", help="IP address of ODL acting as PCE"
)
parser.add_argument(
    "--user", default="admin", help="Username for restconf authentication"
)
parser.add_argument(
    "--password", default="admin", help="Password for restconf authentication"
)
parser.add_argument("--scope", default="sdn", help="Scope for restconf authentication")
parser.add_argument(
    "--reuse",
    default="True",
    type=str2bool,
    help="Should single requests session be re-used",
)
parser.add_argument(
    "--delegate",
    default="true",
    help='should delegate the lsp or set "false" if lsp not be delegated',
)
parser.add_argument("--pccip", default=None, help="IP address of the simulated PCC")
parser.add_argument(
    "--tunnelnumber", default=None, help="Tunnel Number for the simulated PCC"
)
args = parser.parse_args()  # arguments are read

expected = """{"output":{}}"""

payload_list_data = [
    "{",
    '   "input":{',
    '       "node":"pcc://',
    "",
    '",',
    '       "name":"pcc_',
    "",
    "_tunnel_",
    "",
    '",',
    '       "network-topology-ref":"/network-topology:network-topology/network-topology:topology',
    '[network-topology:topology-id=\\"pcep-topology\\"]",',
    '       "arguments":{',
    '           "lsp":{',
    '           "delegate":',
    "",
    '           ,"administrative":true',
    "},",
    '"ero":{',
    '   "subobject":[',
    "       {",
    '           "loose":false,',
    '           "ip-prefix":{',
    '                "ip-prefix":',
    '"',
    "",
    '"',
    "           }",
    "       },",
    "       {",
    '           "loose":false,',
    '           "ip-prefix":{',
    '                "ip-prefix":"1.1.1.1/32"',
    "            }",
    "        }",
    "        ]",
    "       }",
    "   }",
    " }",
    "}",
]


class CounterDown(object):
    """Counter which also knows how many items are left to be added."""

    def __init__(self, tasks):
        self.counter = Counter()
        self.opened = tasks

    def add(self, result):
        self.counter[result] += 1
        self.opened -= 1


def iterable_msg(pccs, lsps, workers, hop, delegate):
    """Generator yielding tuple of worker number and kwargs to post."""
    first_pcc_int = int(ipaddr.IPv4Address(args.pccaddress))
    # Headers are constant, but it is easier to add them to kwargs in this generator.
    headers = {"Content-Type": "application/json"}
    # TODO: Perhaps external text file with Template? May affect performance.
    list_data = payload_list_data
    for lsp in range(1, lsps + 1):
        str_lsp = str(lsp)
        list_data[8] = str_lsp  # Replaces with new pointer.
        for pcc in range(pccs):
            pcc_ip = str(ipaddr.IPv4Address(first_pcc_int + pcc))
            list_data[3] = pcc_ip
            list_data[6] = pcc_ip
            list_data[15] = delegate
            list_data[25] = hop
            whole_data = "".join(list_data)
            worker = (lsp * pccs + pcc) % workers
            post_kwargs = {"data": whole_data, "headers": headers}
            yield worker, post_kwargs


def generate_payload_for_single_pcc(hop, delegate, pccip, tunnel_no):
    """Generator yielding single kwargs to post."""
    first_pcc_int = int(ipaddr.IPv4Address(args.pccaddress))
    # Headers are constant, but it is easier to add them to kwargs in this generator.
    headers = {"Content-Type": "application/json"}
    # TODO: Perhaps external text file with Template? May affect performance.
    list_data = payload_list_data
    if tunnel_no == "None":
        str_lsp = str(1)
    else:
        str_lsp = str(tunnel_no)
    list_data[8] = str_lsp  # Replaces with new pointer.
    if pccip == "None":
        pcc_ip = str(ipaddr.IPv4Address(first_pcc_int))
    else:
        pcc_ip = pccip
    list_data[3] = pcc_ip
    list_data[6] = pcc_ip
    list_data[15] = delegate
    list_data[25] = hop
    whole_data = "".join(list_data)
    worker = 0
    post_kwargs = {"data": whole_data, "headers": headers}
    print(post_kwargs)
    yield worker, post_kwargs


def queued_send(session, queue_messages, queue_responses):
    """Pop from queue, Post and append result; repeat until empty."""
    uri = "operations/network-topology-pcep:update-lsp"
    while 1:
        try:
            post_kwargs = queue_messages.popleft()
        except IndexError:  # nothing more to send
            return
        response = AuthStandalone.Post_Using_Session(session, uri, **post_kwargs)
        # The response perhaps points to some data stored in session,
        # and the session implementation may explicitly call close() to free that data.
        # To be sure, we clone information before further processing.
        status = int(response.status_code)
        content = response.content.decode()
        resp_tuple = (status, content)
        queue_responses.append(resp_tuple)


def classify(resp_tuple):
    """Return 'pass' or a reason what is wrong with response."""
    prepend = ""
    status = resp_tuple[0]
    if (status != 200) and (status != 204):
        prepend = f"status: {status}"
    content = resp_tuple[1]
    if prepend or (content != expected and content != ""):
        return f"{prepend} content: {content}"
    return "pass"


# Main.
list_q_msg = [collections.deque() for _ in range(args.workers)]
if args.pccip == "None":
    for worker, post_kwargs in iterable_msg(
        args.pccs, args.lsps, args.workers, args.hop, args.delegate
    ):
        list_q_msg[worker].append(post_kwargs)
else:
    for worker, post_kwargs in generate_payload_for_single_pcc(
        args.hop, args.delegate, args.pccip, args.tunnelnumber
    ):
        list_q_msg[worker].append(post_kwargs)
queue_responses = collections.deque()  # thread safe
threads = []
for worker in range(args.workers):
    session = AuthStandalone.Init_Session(
        args.odladdress, args.user, args.password, args.scope, args.reuse
    )
    queue_messages = list_q_msg[worker]
    thread_args = (session, queue_messages, queue_responses)
    thread = threading.Thread(target=queued_send, args=thread_args)
    thread.daemon = True
    threads.append(thread)
tasks = sum(map(len, list_q_msg))  # fancy way of counting, should equal to pccs*lsps.
counter = CounterDown(tasks)
print("work is going to start with %s tasks" % tasks)
time_start = time.time()
for thread in threads:
    thread.start()
# debug_list = []
# time_result = time_start
while 1:
    """Main loop for reading and classifying responses, sleeps when there is nothing to process."""
    timedelta_left = time_start + args.timeout - time.time()
    if timedelta_left > 0:
        while counter.opened > 0:
            try:
                resp_tuple = queue_responses.popleft()  # thread safe
            except IndexError:
                break
            result = classify(resp_tuple)
            counter.add(result)
            # time_now = time.time()
            # timedelta_fromlast = time_now - time_result
            # debug_msg = 'DEBUG: opened: ' + str(counter.opened)
            # debug_msg += ' fromlast: ' + str(timedelta_fromlast)
            # debug_list.append(debug_msg)
            # time_result = time_now
        if counter.opened > 0:
            # debug_list.append('DEBUG: sleep ' + str(args.refresh))
            time.sleep(args.refresh)
            continue
        left = len(queue_responses)
        if left:
            print("error: more responses left inqueue", left)
    else:
        print("Time is up!")
        left = len(queue_responses)  # can be still increasing
        for _ in range(left):
            resp_tuple = queue_responses.popleft()  # thread safe
            result = classify(resp_tuple)
            counter.add(result)
    break  # may leave late items in queue_reponses
time_stop = time.time()
timedelta_duration = time_stop - time_start
print("took", timedelta_duration)
print(repr(counter.counter))
# for message in debug_list:
#     print message
