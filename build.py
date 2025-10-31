"""
This script should be invoked directly via the CLI to start a J2V8 build
"""
import sys

import build_system.cli as cli
import build_system.build_interactive as interactive
import build_system.build_executor as bex

# interactive shell entrypoint
if (len(sys.argv) >= 2 and sys.argv[1] in ["--interactive", "-i"]):
    print("\nentering interactive mode...\n")
    interactive.run_interactive_cli()
# passive command-line entrypoint
else:
    parser = cli.get_parser()
    args = parser.parse_args()
    bex.execute_build(args)
