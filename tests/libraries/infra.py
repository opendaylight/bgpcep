#
# Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#

import logging
import subprocess
import threading
import urllib
from queue import Queue
from typing import List

import paramiko
import psutil
import controller_testlib.infra
import controller_testlib.karaf

from libraries import ssh_utils
from libraries import utils
from libraries.RemoteSSHSessionHandler import RemoteSSHSessionHandler

log = logging.getLogger(__name__)

# Re-exported so existing "infra.shell(...)" call sites keep working, but the
# origin of each is explicit here rather than hidden behind a bare-name import.
copy_dir = controller_testlib.infra.copy_dir
copy_file = controller_testlib.infra.copy_file
count_port_occurrences = controller_testlib.infra.count_port_occurrences
get_file_content = controller_testlib.infra.get_file_content
is_process_still_running = controller_testlib.infra.is_process_still_running
save_text_to_a_file = controller_testlib.infra.save_text_to_a_file
shell = controller_testlib.infra.shell
stop_process_by_pid = controller_testlib.infra.stop_process_by_pid

execute_karaf_command = controller_testlib.karaf.execute_karaf_command
is_datastore_ready = controller_testlib.karaf.is_datastore_ready
is_karaf_feature_installed = controller_testlib.karaf.is_karaf_feature_installed
log_message_to_karaf = controller_testlib.karaf.log_message_to_karaf
start_odl_with_features = controller_testlib.karaf.start_odl_with_features


def read_until(process: subprocess.Popen, expected_text: str, timeout: int = 10) -> str:
    """Reads process stdout until expected string is found.

    This exepcted text must be within one line, it can not spread across
    mutliple lines. Command needs to finish within specific time otherwise
    it would time out.

    Args:
        process (str): Process which stdout should be observerd.
        expected_text (str): Pattern to be read until.
        timeout (int): Timeout in seconds.

    Returns:
        str: Process stdout until expected string occurence.
    """
    found_event = threading.Event()
    output_lines = []

    def threaded_read(stream, expected_text, queue):
        while True:
            line = stream.readline()
            output_lines.append(line)
            # whole_text += line
            if line and expected_text in line:
                found_event.set()
                return

    queue = Queue()
    thread = threading.Thread(
        target=threaded_read, args=(process.stdout, expected_text, queue), daemon=True
    )
    thread.start()
    text_wasfound = found_event.wait(timeout=timeout)

    whole_text = "\n".join(output_lines)

    if not text_wasfound:
        raise AssertionError(
            (
                f"Unable to find expected text:' {expected_text}' withing {timeout} "
                f"seconds. Captured output: '{whole_text}'"
            )
        )

    log.warn(whole_text)

    return whole_text


def get_children_processes_pids(
    process: subprocess.Popen, command: str = ""
) -> List[int]:
    """Returns pids of a child processes which are running certain command.

    Args:
        process (str): Parent process handler.
        command (str): Command which child process is running.

    Returns:
        List[int]: List of matching children processes pids.
    """
    process = psutil.Process(process.pid)
    children = process.children(recursive=True)
    log.warn(process)
    log.warn([child.name() for child in children])
    matching_pids = [
        child.pid for child in children if child.name().startswith(command)
    ]

    return matching_pids


def wait_for_string_in_file(
    retry_count: int, interval: int, string: str, file_name: str, threshold: int = 1
) -> int:
    """Repeatedly reads text file until it finds specific substring.

    Args:
        retry_count (int): Maximum number of retries
        interval (int): Number of seconds to wait until next retry.
        string (str): Subsring expected to be present in text file.
        file_name (str): Name of the text file to be checked for
            presence of substring.
        threshold (int): Minimum number of occurences of the expected
            substring.

    Returns:
        int: number of substring occurences in text file
    """
    validator = lambda result: result[0] == 0 and int(result[1].strip()) >= threshold
    rc, output = utils.wait_until_function_returns_value_with_custom_value_validator(
        retry_count, interval, validator, shell, f"grep -c '{string}' '{file_name}'"
    )
    return int(output.strip())


def get_string_occurence_count_in_file(string: str, file_name: str) -> int:
    """Counts number of occurences of specific string in a text file.

    Args:
        string (str): Pattern to be matched.
        file_name (str): Name of the text file to be searched.

    Returns:
        int: Number of occurences.
    """
    rc, output = shell(f"grep -c '{string}' '{file_name}'")

    return int(output)


def verify_string_occurence_count_in_file(
    string: str, file_name: str, count: int, exact: bool = True
):
    """Verifies number of occurences of specific string in a text file.

    Args:
        string (str): Pattern to be matched.
        file_name (str): Name of the text file to be searched.
        count (int): Expected number of string occurences.
        exact (bool): If set, number of occurences must exactly match
            the expected count, otherwise must be greater then or equal to
            the expected count.

    Returns:
        None
    """
    found_occurences = get_string_occurence_count_in_file(string, file_name)
    if exact:
        assert (
            found_occurences == count
        ), f"Did not find {count} times str: {string} in {file_name}"
    else:
        assert (
            found_occurences >= count
        ), f"Did not find at least {count} times str: {string} in {file_name}"


def stop_process(process: subprocess.Popen, gracefully=True):
    """Stop process by sending proper signal, but print stdout first.

    Args:
        process (subprocess.Popen): Process handler.
        gracefully (bool): Determines which signal should be sent for
            stopping process.

    Returns:
        None
    """
    output = process.stdout
    log.debug(f"Process output: {output=}")
    stop_process_by_pid(process.pid, gracefully=gracefully)


def download_file(url: str):
    """Download file from specified url.

    Stores downloaded file under tmp folder.

    Args:
        url (str): File URL location.

    Returns:
        None.
    """
    file_name = url.split("/")[-1]
    urllib.request.urlretrieve(url, f"tmp/{file_name}")


def backup_file(
    src_file_name: str,
    target_file_name: str | None = None,
    src_dir: str = "tmp",
    dst_dir: str = "results",
):
    """Backup test file by copiyng it to the destination directory

    Files from tmp/ folder are removed during start of the test, to keep
    these files persistantly they need to be moved to the results folder.

     Args:
        tmp_file_name (str): Path to the tmp file, which should be
            persistantly stored.
        target_file_name (str): Optional target file name, set if the copied
            file needs to be stored under different name in results folder.
            By default it keeps the file name.
        src_dir (str): Source file directory in which it is located.
        dst_dir (str): Destination directory where the file should be copied.

    Returns:
        None
    """
    if src_file_name == "":
        raise ValueError("tmp_file_name value can not be empty string")
    if target_file_name == "":
        raise ValueError("target_file_name value can not be empty string")
    if target_file_name is None:
        target_file_name = src_file_name
    shell(f"cp {src_dir}/{src_file_name} {dst_dir}/{target_file_name}")
