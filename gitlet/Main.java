package gitlet;
import java.io.IOException;
import static gitlet.Utils.message;
import static java.lang.System.exit;

/**
 * Driver class for Gitlet, the tiny stupid version-control system.
 *
 * @author Taiga Kitao
 */
public class Main {

    /**
     * Usage: java gitlet.Main ARGS, where ARGS contains
     * <COMMAND> <OPERAND> ....
     */
    public static void main(String... args) throws IOException {
        Command command = new Command();
        if (args.length == 0) {
            message("Please enter a command");
            exit(0);
        }
        switchCommand(args, command);
        exit(0);
    }

    public static boolean validateNumArgs(String[] args, int n) {
        if (args.length == n) {
            return true;
        }
        message("Incorrect operands");
        return false;
    }
    public static void checkGit(Command command) {
        if (!command.GITLET.exists()) {
            message("Not in an initialized Gitlet directory.");
            exit(0);
        }
    }
    public static void checkGitAndArg(Command command,
                                      String[] args, int n) {
        if (!command.GITLET.exists()) {
            message("Not in an initialized Gitlet directory.");
            exit(0);
        }
        if (!validateNumArgs(args, n)) {
            exit(0);
        }
    }
    public static void checkOut(String[] args, Command c) {
        if (args.length == 3) {
            if (!args[1].equals("--")) {
                message("Incorrect operands");
                return;
            } else {
                c.checkoutFile(args[2]);
                return;
            }
        } else if (args.length == 4) {
            if (!args[2].equals("--")) {
                message("Incorrect operands");
                return;
            } else {
                c.checkoutCommitFile(args[1], args[3]);
                return;
            }
        } else if (args.length == 2) {
            c.checkoutBranch(args[1]);
            return;
        } else {
            message("Incorrect operands");
            return;
        }
    }

    public static void switchCommand(String[] args,
                                     Command c) throws IOException {
        switch (args[0]) {
        case "init": {
            if (validateNumArgs(args, 1)) {
                c.init();
            }
            break;
        }
        case "add": {
            checkGitAndArg(c, args, 2);
            c.add(args[1]);
            break;
        }
        case "commit": {
            checkGitAndArg(c, args, 2);
            c.commit(args[1], null);
            break;
        }
        case "log": {
            checkGitAndArg(c, args, 1);
            c.log();
            break;
        }
        case "checkout": {
            checkGit(c);
            checkOut(args, c);
            break;
        }
        case "rm": {
            checkGitAndArg(c, args, 2);
            c.remove(args[1]);
            break;
        }
        default: {
            anotherSwitch(args, c);
        }
        }
    }
    public static void anotherSwitch(String[] args, Command c)
            throws IOException {
        switch (args[0]) {
        case "global-log": {
            checkGitAndArg(c, args, 1);
            c.globalLog();
            break;
        }
        case "find": {
            checkGitAndArg(c, args, 2);
            c.find(args[1]);
            break;
        }
        case "status": {
            checkGitAndArg(c, args, 1);
            c.status();
            break;
        }

        case "branch": {
            checkGitAndArg(c, args, 2);
            c.branch(args[1]);
            break;
        }
        case "rm-branch": {
            checkGitAndArg(c, args, 2);
            c.rmBranch(args[1]);
            break;
        }
        case "reset": {
            checkGitAndArg(c, args, 2);
            c.reset(args[1]);
            break;
        }
        case "merge": {
            checkGitAndArg(c, args, 2);
            c.merge(args[1]);
            break;
        }
        case "diff": {
            if (args.length == 1) {
                c.diff(null, null);
            } else if (args.length == 2) {
                c.diff(args[1], null);
            } else if (args.length == 3) {
                c.diff(args[1], args[2]);
            }
            break;
        }
        default: {
            message("No command with that name exists.");
        }
        }
    }
}
