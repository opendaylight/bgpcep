#
# Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#

import logging
import signal
import subprocess
import threading
import urllib
from queue import Queue
from typing import List

import paramiko
import psutil

from libraries import ssh_utils
from libraries import utils
from libraries.KarafShell import KarafShell
from libraries.RemoteSSHSessionHandler import RemoteSSHSessionHandler

KARAF_SHELL_INSTANCE = None

log = logging.getLogger(__name__)


def shell(
    command: str | list | tuple,
    joiner="; ",
    cwd: str | None = None,
    use_shell=True,
    run_in_background: bool = False,
    timeout: int = None,
    check_rc=False,
):
    """Runs single or multiple shell commands.

    Multiple shell command are concatenated together by using joiner.
    It provides mutliple options on how to run the command.

    Args:
        command (str | list | tuple): Shell command(s) to be run.
        joiner (str): Joiner for concatenating multiple commands.
        cwd (str): Current working directory from where the command
            needs to be executed.
        run_in_backgroud (bool): If the command should be started as background
            process without tty.
        timeotu (int): Timeout in seconds for the foreground command.

    Returns:
        tuple[int, str] | subprocess.Popen :
            For foreground process it returns final return code and stdout,
            for backgroud process it returns process handler.
    """
    exec_command = command
    if isinstance(command, (list, tuple)):
        exec_command = joiner.join(command)

    try:
        log.info(exec_command)
        if run_in_background:
            if use_shell:
                process = subprocess.Popen(
                    f"exec {exec_command}",
                    shell=True,
                    text=True,
                    stdout=subprocess.PIPE,
                    stderr=subprocess.STDOUT,
                    stdin=subprocess.DEVNULL,
                    bufsize=1,
                    cwd=cwd,
                )
            else:
                process = subprocess.Popen(
                    exec_command.split(" "), shell=False, text=True, cwd=cwd
                )
            return process
        else:
            result = subprocess.run(
                exec_command,
                shell=shell,
                check=True,
                capture_output=True,
                text=True,
                timeout=timeout,
                cwd=cwd,
            )
            log.debug(f"{result.returncode:3d} |--| {result.stdout}")
            if check_rc and result.returncode != 0:
                return AssertionError(f"Expected command {exec_command} to pass")
            return result.returncode, result.stdout
    except subprocess.CalledProcessError as e:
        std_error = e.stderr.strip()
        log.error(
            f"ERROR while command execution '{exec_command}'"
            f"{':\n' + std_error if std_error else ''}"
        )
        return e.returncode, e.stdout
    except FileNotFoundError:
        log.error(f"ERROR command not found: {exec_command}")
        return None, None


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


def retry_shell_command(retry_count: int, interval: int, *args, **kwargs):
    """Repeatedly runs a shell command until the return code is 0.

    Args:
        retry_count (int): Maximum number of retries
        interval (int): Number of seconds to wait until next retry.
        *args: shell positional argments
        **kwargs: shell keyword arguments

    Returns:
        tuple[int, str]: Return code and standart output of successful run
            of the command.
    """
    validator = lambda result: result[0] == 0
    rc, output = utils.wait_until_function_returns_value_with_custom_value_validator(
        retry_count, interval, validator, shell, *args, **kwargs
    )
    return rc, output


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


def count_port_occurences(port: int, state: str, name: str):
    """Counts number of occurences of specific port types.

    Args:
        port (str): Port number.
        state (str): Port state.
        name (str): Name of the program using the port.

    Returns:
        int: Number of port occurences.
    """
    rc, stdout = shell(
        f'ss -punta 2> /dev/null | grep -E "{state} .+:{port} .+{name}" | wc -l'
    )
    assert rc == 0, f"Failed to check number of occurences for {port=} {state=} {name=}"
    return int(stdout)


def start_odl_with_features(features: tuple[str], timeout: int = 60):
    """Starts ODL with installed provided features.

    Args:
        features (tuple[str]): Features to be installed in ODL.
        timeout (int): Timeout within which it needs to start ODL, otherwise fail.

    Returns:
        None
    """
    # set config with the required features
    shell(
        f"sed -ie 's/\(featuresBoot=\|featuresBoot =\)/featuresBoot = "
        f"{",".join(features)},/g' etc/org.apache.karaf.features.cfg",
        cwd="opendaylight",
    )

    shell(
        "sed -ie 's/memory-mapped = true/memory-mapped = false/g' "
        "system/org/opendaylight/controller/sal-clustering-config/*/"
        "sal-clustering-config-*-factorypekkoconf.xml",
        cwd="opendaylight",
    )

    # start ODL
    shell("JAVA_OPTS=-Xmx8g ./bin/start", cwd="opendaylight")

    # wait for proper message with timeout
    interval = 5
    retry_shell_command(
        timeout // interval,
        interval,
        "grep 'org.opendaylight.infrautils.*System ready' data/log/karaf.log",
        cwd="opendaylight",
    )


def execute_karaf_command(command: str) -> tuple[str, str]:
    """Executed specific command using ODL karaf CLI console

    It usses ssh connection to connect to karaf CLI.

    Args:
        command (str): Command to be executed.

    Returns:
        tuple[str, str]: Stdout from karaf CLI, stderr from karaf CLI.
    """
    global KARAF_SHELL_INSTANCE

    log.info(f"Executing command '{command}' on karaf console.")

    if KARAF_SHELL_INSTANCE is None:
        KARAF_SHELL_INSTANCE = KarafShell(host="127.0.0.1", port=8101)

    try:
        stdout = KARAF_SHELL_INSTANCE.execute(command)
        log.info(f"Command Output:\n{stdout}")

        return stdout, ""

    except Exception as e:
        log.error(f"Failed to execute karaf command: {e}")
        return "", str(e)


def log_message_to_karaf(message: str):
    """Log specific mesage to ODL karaf

    It usses ssh connection to connect to karaf CLI.

    Args:
        message (str): Message to be logged.

    Returns:
        None
    """
    execute_karaf_command(f"log:log 'ROBOT MESSAGE: {message}'")


def is_process_still_running(pid: int):
    """Check if provided process did not finish yet.

    Args:
        process (subprocess.Popen): Process handler.

    Returns:
        None
    """
    try:
        process = psutil.Process(pid)
    except psutil.NoSuchProcess:
        return False
    return process.is_running() and process.status() != psutil.STATUS_ZOMBIE


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


def stop_process_by_pid(pid: int, gracefully: bool = True, timeout: int | None = 5):
    """Stops process by sending signal a veirfies it is not running.

    Args:
        process (subprocess.Popen): Process handler.
        gracefully (bool): Determines which signal should be sent,
            for gracefully it sends SIGINT, otherwise SIGKILL

    Returns:
        None
    """
    log.info(f"Stopping process with PID {pid}")
    signal_to_be_sent = signal.SIGTERM if gracefully else signal.SIGKILL
    log.info(f"Sending signal {signal_to_be_sent} to process with PID {pid}")
    process = psutil.Process(pid)
    process.send_signal(signal_to_be_sent)

    if timeout is not None:
        # check if it is still running
        try:
            utils.wait_until_function_returns_value(
                timeout, 1, False, is_process_still_running, process.pid
            )
        except AssertionError as e:
            raise AssertionError(
                (
                    f"Was not able to stop process with PID {process.pid}, "
                    f"it is still running."
                )
            ) from e


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


def get_file_content(path: str):
    """Returns text file content.

    Args:
        path (str): Text file path.

    Returns:
        str: Text file content.
    """
    with open(path, "r", encoding="utf-8") as file:
        content = file.read()

    return content


def save_to_a_file(path: str, content: str):
    """Stores text content to a file.

    Args:
        path (str): Text file path.

    Returns:
        None
    """
    with open(path, "w", encoding="utf-8") as file:
        file.write(content)


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


def search_and_kill_process(filter):
    """Search for processes, Log the list of them, kill them."""
    rc, processes = shell(f"ps -elf | egrep python | egrep {filter} | egrep -v grep")
    log.info(f"{processes=}")
    if not processes:
        return
    rc, commands = shell(f"echo '{processes}' | awk '{{print \"kill -{signal}\",$4}}'")
    rc, stdout = shell(f" echo 'set -exu; {commands}' | sudo sh")
    log.info(stdout)
