import logging
import time
import re

from libraries import ssh_utils

log = logging.getLogger(__name__)

class KarafShell:
    """
    A persistent SSH client for Karaf that keeps a single shell open.
    It cleans up ANSI control codes but PRESERVES the prompt at the end.
    """
    PROMPT_REGEX = r'[a-zA-Z0-9_\-\.]+@[a-zA-Z0-9_\-\.]+>$'

    def __init__(self, host="127.0.0.1", port=8101, user="karaf", password="karaf"):
        self.host = host
        self.port = port
        self.user = user
        self.password = password
        self.client = None
        self.shell = None

    def connect(self):
        """Connects to ODL karaf CLI using SSH.

        Args:
            None

        Returns:
            None
        """
        if self.client is None or self.shell is None:
            log.debug(f"Opening interactive KarafShell to {self.host}:{self.port}...")
            self.client = ssh_utils.create_ssh_client(
                self.host, self.port, self.user, self.password
            )
            self.shell = self.client.invoke_shell(term='dumb', width=500, height=200)
            self._read_until_prompt()
            log.debug("KarafShell successfully connected.")

    def close(self):
        """Closes SSH connection to ODL karaf CLI

        Args:
            host (str): Hostname of the remote SSH server.
            username (str): Client username.
            password (str): Client password.
            port (int): Remote SHH server port.

        Returns:
            None
        """
        log.debug(f"Closing interactive KarafShell to {self.host}:{self.port}...")
        if self.shell:
            self.shell.close()
            self.shell = None
        if self.client:
            self.client.close()
            self.client = None
        log.debug("KarafShell successfully disconnected.")

    def execute(self, command: str, timeout: int = 30) -> str:
        """Executes command on karaf CLI.

        Args:
            command (str): Command to be executed.
            timeout (int): Timeout in seconds.

        Returns:
            str: command output
        """
        # If not properly connected, reconnect
        if not self.shell or not self.client:
            self.close()
            self.connect()

        try:
            # Read all the output left from previous command
            while self.shell.recv_ready():
                self.shell.recv(4096)

            # Send command
            self.shell.send(f"{command}\n")
            raw_output = self._read_until_prompt(timeout)

            # Remove the echoed command from output (first line)
            raw_output = "\n".join(raw_output.splitlines()[1:])
            return raw_output

        except Exception as e:
            log.warning(f"KarafShell error during executing command: {command} - {e}.")
            self.close()
            raise e

    def _read_until_prompt(self, timeout=30):
        """Reads stdout from karaf CLI until prompt is recognized.

        Args:
            timeout (int): Timeout in seconds.

        Returns:
            None
        """
        buffer = ""
        start_time = time.time()
        while True:
            if time.time() - start_time > timeout:
                raise TimeoutError(f"Timed out waiting for Karaf prompt. Buffer:\n{buffer}")

            if self.shell.recv_ready():
                chunk = self.shell.recv(4096).decode('utf-8', errors='ignore')
                buffer += chunk
                if re.search(self.PROMPT_REGEX, buffer):
                    return buffer

            time.sleep(0.05)