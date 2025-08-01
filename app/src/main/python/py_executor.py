import sys
from io import StringIO
import traceback

def run_code(code):
    """
    Executes a string of Python code and captures its stdout and stderr.
    """
    # Create in-memory text streams
    stdout_capture = StringIO()
    stderr_capture = StringIO()

    # Redirect the system's standard streams
    sys.stdout = stdout_capture
    sys.stderr = stderr_capture

    try:
        # The `exec` function executes the code string.
        # We pass an empty dictionary for the globals to start with a clean slate.
        exec(code, {})
    except Exception:
        # If an error occurs, print the traceback to our captured stderr.
        traceback.print_exc()
    finally:
        # Always restore the original stdout and stderr.
        sys.stdout = sys.__stdout__
        sys.stderr = sys.__stderr__

    # Get the string values from our capture streams
    stdout_val = stdout_capture.getvalue()
    stderr_val = stderr_capture.getvalue()

    return stdout_val + stderr_val