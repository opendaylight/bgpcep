import subprocess
import time

def test():
    # --- CONFIGURE THIS ---
    # Use the full, absolute paths if possible.
    # This is the command that we suspect is failing.
    COMMAND_TO_TEST = ["stdbuf", "-oL", "java", "-jar", "build_tools/pcep-pcc-mock.jar", "--reconnect", "1", "--local-address", "127.0.0.1", "--remote-address", "127.0.0.1"]
    LOG_FILE = "debug_output.log"
    WAIT_SECONDS = 15 # How long to let the process run before we kill it.

    # --- RUN THE TEST ---
    process = None
    print(f"Starting test. Running command for {WAIT_SECONDS} seconds...")
    print(f"Command: {' '.join(COMMAND_TO_TEST)}")
    print(f"All output will be captured in: {LOG_FILE}")

    try:
        # Open the log file for writing
        with open(LOG_FILE, "w") as log_file:
            # Start the process simply, with all output redirected to the file
            process = subprocess.Popen(
                COMMAND_TO_TEST,
                stdout=log_file,
                stderr=log_file,
                text=True
            )
            
            # Let the process run for a while. wait() will raise an exception
            # if the timeout is reached, which is what we expect for a server.
            process.wait(timeout=WAIT_SECONDS)

    except subprocess.TimeoutExpired:
        print(f"\nProcess ran for the full {WAIT_SECONDS} seconds as expected.")
        # This is the "success" path for a long-running server.

    except FileNotFoundError:
        print(f"\nCRITICAL ERROR: Command not found. Is '{COMMAND_TO_TEST[0]}' installed and in your PATH?")
        # This is a common failure point.

    except Exception as e:
        print(f"\nAn unexpected error occurred: {e}")

    finally:
        if process and process.poll() is None:
            print("Terminating process...")
            process.terminate()
            process.communicate() # Safe cleanup
        print(f"Test finished. Please inspect the contents of '{LOG_FILE}'.")