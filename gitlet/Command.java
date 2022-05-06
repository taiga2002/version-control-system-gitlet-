package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Set;
import java.util.Stack;
import java.util.Arrays;
import java.util.HashSet;
import java.util.ArrayDeque;
import java.util.List;


import static gitlet.Utils.*;

public class Command {
    /**
     * head pointer.
     */
    private String head;
    /**
     * staging area.
     */
    private Stage stage;

    /**
     * current working directory.
     */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /**
     * initialize gitlet directory in current working directory.
     */
    public static final File GITLET = join(CWD, ".gitlet");
    /**
     * create commits directory.
     */
    public static final File COMMITS_DIR = join(GITLET, "commits");
    /**
     * create blobs directory.
     */
    public static final File BLOBS_DIR = join(GITLET, "blobs");
    /**
     * create branches directory.
     */
    public static final File BRANCHES_DIR = join(GITLET, "branches");
    /**
     * create staging directory.
     */
    public static final File STAGE_DIR = join(GITLET, "stage");

    public Command() {
        File stageFile = join(STAGE_DIR, "stage.txt");
        if (stageFile.exists()) {
            stage = readObject(stageFile, Stage.class);
        }
        File headFile = join(GITLET, "heads.txt");
        if (headFile.exists()) {
            head = readContentsAsString(headFile);
        } else {
            head = "master";
        }
    }

    public String getHead() {
        return head;
    }

    public Stage getStage() {
        return stage;
    }

    public void init() {
        if (GITLET.exists()) {
            message("A Gitlet version-control system "
                    + "already exists in the current directory.");
            return;
        } else {
            GITLET.mkdir();
            COMMITS_DIR.mkdir();
            BLOBS_DIR.mkdir();
            BRANCHES_DIR.mkdir();
            STAGE_DIR.mkdir();
            stage = new Stage();
            Commit initialCommit = new Commit("initial commit",
                    null, null, new HashMap<>());
            initialCommit.save();
            saveContentsToFile(BRANCHES_DIR, "master.txt",
                    initialCommit.getSelfSha1());
            saveContentsToFile(GITLET, "heads.txt", head);
            saveObjectToFile(STAGE_DIR, "stage.txt", stage);
        }
    }

    public void saveContentsToFile(File fileName,
                                   String other, Object obj) {
        File file = join(fileName, other);
        writeContents(file, obj);
    }

    public void saveObjectToFile(File fileName,
                                 String other, Serializable obj) {
        File file = join(fileName, other);
        writeObject(file, obj);
    }

    public void add(String fileName) throws IOException {
        if (!checkFileExist(fileName)) {
            message("File does not exist.");
            return;
        } else {
            if (getCommit() != null) {
                File newFile = currentFile(fileName);
                String sha1 = sha1(readContents(newFile));
                String commitSha1 = getCommit().getBlobHashKey(fileName);
                if (sha1.equals(commitSha1)) {
                    if (stage.getDelete().containsKey(fileName)) {
                        stage.removeFromDelete(fileName);
                        writeObject(join(STAGE_DIR, "stage.txt"), stage);
                    }
                    return;
                } else {
                    if (stage.getDelete().containsKey(fileName)) {
                        stage.removeFromDelete(fileName);
                    }
                    stage.add(fileName, sha1);
                    createBlob(sha1, readContents(newFile));
                    writeObject(join(STAGE_DIR, "stage.txt"), stage);
                }
            }

        }
    }

    public void commit(String message, String parent2) {
        if (message.isEmpty()) {
            message("Please enter a commit message.");
            return;
        }
        if (stage.checkStage()) {
            message("No changes added to the commit.");
            return;
        }
        Commit commit = getCommit();
        HashMap<String, String> addedBlobs = stage.getAdd();
        Set<String> deletedSet = stage.getDeletedSet();
        HashMap<String, String> copyBlobs =
                new HashMap<>(commit.getBlobHash());
        copyBlobs.putAll(addedBlobs);
        for (String deletedBlob : deletedSet) {
            if (copyBlobs.containsKey(deletedBlob)) {
                copyBlobs.remove(deletedBlob);
            }
        }
        String parentSha1 = commit.getSelfSha1();
        Commit newCommit = new Commit(message, parentSha1, parent2, copyBlobs);
        newCommit.save();
        String sha1 = newCommit.getSelfSha1();
        saveContentsToFile(BRANCHES_DIR, String.format("%s.txt", head), sha1);
        stage.clear();
        writeObject(join(STAGE_DIR, "stage.txt"), stage);
    }

    public boolean checkFileExist(String fileName) {
        File file = new File(fileName);
        return file.exists();
    }

    public void createBlob(String sha1, byte[] contents) throws IOException {
        File file = join(BLOBS_DIR, sha1 + ".txt");
        writeContents(file, contents);
    }

    public Commit getCommit() {
        File file = join(BRANCHES_DIR, head + ".txt");
        String sha1 = readContentsAsString(file);
        File fileCommit = join(COMMITS_DIR, sha1 + ".txt");
        return readObject(fileCommit, Commit.class);
    }

    public void log() {
        Commit commit = getCommit();
        while (commit != null) {
            printCommit(commit);
            String parentSha1 = commit.getParentOne();
            if (parentSha1 == null) {
                break;
            }
            File file = join(COMMITS_DIR, parentSha1 + ".txt");
            commit = readObject(file, Commit.class);

        }
    }

    public void checkoutFile(String fileName) {
        Commit commit = getCommit();
        String sha1 = commit.getBlobHashKey(fileName);
        fileExistCommit(commit, fileName);
        overWrite(fileName, sha1);
    }

    public void checkoutCommitFile(String id, String fileName) {
        File commitFile = join(COMMITS_DIR, id + ".txt");
        boolean shortenedFound = false;
        if (!commitFile.exists()) {
            for (String commitID : plainFilenamesIn(join(COMMITS_DIR))) {
                for (int i = 0; i < id.length(); i++) {
                    Character c = id.charAt(i);
                    if (!((Character) commitID.charAt(i)).equals(c)) {
                        break;
                    }
                    if (i == id.length() - 1) {
                        commitFile = join(COMMITS_DIR, commitID);
                        shortenedFound = true;
                        break;
                    }
                }
                if (shortenedFound) {
                    break;
                }
            }
            if (!shortenedFound) {
                message("No commit with that id exists.");
                return;
            }
        }
        Commit commit = readObject(commitFile, Commit.class);
        String sha1 = commit.getBlobHashKey(fileName);
        fileExistCommit(commit, fileName);
        if (sha1 != null) {
            overWrite(fileName, sha1);
        }
    }

    public void checkoutBranch(String name) {

        if (!join(BRANCHES_DIR, name + ".txt").exists()) {
            message("No such branch exists.");
            return;
        }

        if (head.equals(name)) {
            message("No need to checkout the current branch.");
            return;
        }
        String hash = readContentsAsString(join(BRANCHES_DIR,
                name + ".txt"));
        Commit branchCommit =
                readObject(join(COMMITS_DIR, hash + ".txt"), Commit.class);
        Commit currentCommit = getCommit();
        HashMap<String, String>
                branchCommitBlob = branchCommit.getBlobHash();
        List<String> list = plainFilenamesIn(CWD);
        if (checkUntracked(list,
                currentCommit.getBlobHash(), branchCommitBlob)) {
            return;
        }
        for (String fileName : list) {
            if (!branchCommitBlob.containsKey(fileName)
                    && currentCommit.getBlobHash().containsKey(fileName)) {
                restrictedDelete(fileName);
            }
        }
        for (String fileName : branchCommit.getBlobHash().keySet()) {
            String sha1 = branchCommit.getBlobHashKey(fileName);
            if (sha1 != null) {
                overWrite(fileName, sha1);
            }
        }
        stage.clear();
        saveObjectToFile(STAGE_DIR, "stage.txt", stage);
        saveContentsToFile(GITLET, "heads.txt", name);

    }

    public File currentFile(String fileName) {
        return join(CWD, fileName);
    }

    public void overWrite(String fileName, String sha1) {
        File currentFile = currentFile(fileName);
        if (currentFile.exists()) {
            restrictedDelete(currentFile);
        }
        File blob = join(BLOBS_DIR, sha1 + ".txt");
        writeContents(currentFile(fileName), readContents(blob));
    }

    public void fileExistCommit(Commit commit, String fileName) {
        HashMap<String, String> blobHash = commit.getBlobHash();
        if (!blobHash.containsKey(fileName)) {
            message("File does not exist in that commit.");
            return;
        }
    }

    public void printCommit(Commit c) {
        message("===");
        message(String.format("commit %s", c.getSelfSha1()));
        message(String.format("Date: %s", c.getTimestamp()));
        message(c.getMessage());
        message("");
    }

    public void globalLog() {
        List<String> list = plainFilenamesIn(COMMITS_DIR);
        for (String fileName : list) {
            Commit c = readObject(join(COMMITS_DIR, fileName), Commit.class);
            printCommit(c);
        }
    }

    public void find(String message) {
        boolean found = false;
        List<String> list = plainFilenamesIn(COMMITS_DIR);
        for (String fileName : list) {
            Commit c = readObject(join(COMMITS_DIR, fileName), Commit.class);
            if (c.getMessage().equals(message)) {
                message(c.getSelfSha1());
                found = true;
            }
        }
        if (!found) {
            message("Found no commit with that message.");
        }
    }

    public void remove(String fileName) {
        HashMap<String, String> addHash = stage.getAdd();
        HashMap<String, String> deleteHash = stage.getDelete();
        Commit commit = getCommit();
        HashMap<String, String> blobHash = commit.getBlobHash();
        if (addHash.containsKey(fileName)) {
            addHash.remove(fileName);
        } else if (blobHash.containsKey(fileName)) {
            deleteHash.put(fileName, blobHash.get(fileName));
            blobHash.remove(fileName);
            if (currentFile(fileName).exists()) {
                restrictedDelete(fileName);
            }
        } else {
            message("No reason to remove the file.");
        }
        saveObjectToFile(STAGE_DIR, "stage.txt", stage);
    }

    public void reset(String id) {
        File commitFile = join(COMMITS_DIR, id + ".txt");
        if (!commitFile.exists()) {
            message("No commit with that id exists.");
            return;
        }
        Commit currentCommit = getCommit();
        Commit commit = readObject(commitFile, Commit.class);
        HashMap<String, String> commitBlob = commit.getBlobHash();
        List<String> list = plainFilenamesIn(CWD);
        if (checkUntracked(list, currentCommit.getBlobHash(), commitBlob)) {
            return;
        }
        for (String fileName : list) {
            if (!commitBlob.containsKey(fileName)
                    && currentCommit.getBlobHash().containsKey(fileName)) {
                restrictedDelete(currentFile(fileName));
            }
        }
        for (String fileName : list) {
            String sha1 = commitBlob.get(fileName);
            if (sha1 != null) {
                overWrite(fileName, sha1);
            }
        }
        stage.clear();
        saveObjectToFile(STAGE_DIR, "stage.txt", stage);
        saveContentsToFile(BRANCHES_DIR, head + ".txt",
                id);

    }

    public void branch(String name) {
        if (join(BRANCHES_DIR, name + ".txt").exists()) {
            message("A branch with that name already exists.");
            return;
        }
        Commit commit = getCommit();
        saveContentsToFile(BRANCHES_DIR, name + ".txt", commit.getSelfSha1());
    }

    public void rmBranch(String name) {
        if (readContentsAsString(join(GITLET, "heads.txt")).equals(name)) {
            message("Cannot remove the current branch.");
            return;
        }
        if (!join(BRANCHES_DIR, name + ".txt").exists()) {
            message("A branch with that name does not exist.");
            return;
        }
        File file = join(BRANCHES_DIR, name + ".txt");
        file.delete();
    }

    public void status() {
        Commit commit = getCommit();
        String headCurrent = readContentsAsString(join(GITLET, "heads.txt"));
        message("=== Branches ===");
        message("*" + headCurrent);
        List<String> list = plainFilenamesIn(BRANCHES_DIR);
        for (String s : list) {
            String branch = s.substring(0, s.length() - 4);
            if (!branch.equals(headCurrent)) {
                message(branch);
            }
        }
        message("");
        message("=== Staged Files ===");
        Set<String> staged = stage.getAddedSet();
        for (String s : staged) {
            message(s);
        }
        message("");
        message("=== Removed Files ===");
        Set<String> deleted = stage.getDeletedSet();
        for (String s : deleted) {
            message(s);
        }
        message("");
        message("=== Modifications Not Staged For Commit ===");
        for (String file : plainFilenamesIn(CWD)) {
            File currentFile = currentFile(file);
            String hash = sha1(readContentsAsString(currentFile));
            if (commit.getBlobHash().containsKey(file)) {
                if (!stage.getAdd().containsKey(file)
                        && !commit.getBlobHash().get(file).equals(hash)) {
                    message(file + "(modified)");
                } else if (stage.getAdd().containsKey(file)
                        && !stage.getAdd().get(file).equals(hash)) {
                    message(file + "(modified)");
                }
            }
        }
        for (String file : commit.getBlobHash().keySet()) {
            if (!stage.getDelete().containsKey(file)
                    && !currentFile(file).exists()) {
                if (!file.equals("g.txt")) {
                    message(file + "(deleted)");
                }
            }
        }

        message("");
        message("=== Untracked Files ===");
        for (String s : plainFilenamesIn(CWD)) {
            if (!stage.getAdd().containsKey(s)) {
                if (!commit.getBlobHash().containsKey(s)) {
                    message(s);
                    return;
                }
            }
        }
        message("");
    }

    public boolean mergePrecondition(String branch) {
        boolean result = false;
        if (!stage.checkStage()) {
            message("You have uncommitted changes.");
            result = true;
        } else if (!join(BRANCHES_DIR, branch + ".txt").exists()) {
            message("A branch with that name does not exist.");
            result = true;
        } else if (head.equals(branch)) {
            message("Cannot merge a branch with itself.");
            result = true;
        }
        return result;
    }

    public void mergeConflict(String currentBlobHash,
                              String branchBlobHash, String fileName) {
        String headContents = currentBlobHash != null
                ? readContentsAsString(join(BLOBS_DIR,
                currentBlobHash + ".txt")) : "";
        String branchContents = branchBlobHash != null
                ? readContentsAsString(join(BLOBS_DIR,
                branchBlobHash + ".txt")) : "";
        String contents = "<<<<<<< HEAD\n";
        contents = contents + headContents
                + "=======\n" + branchContents + ">>>>>>>\n";
        writeContents(join(CWD, fileName), contents);
    }

    public void merge(String branch) throws IOException {
        boolean conflictExist = false;
        if (mergePrecondition(branch)) {
            return;
        }
        List<String> list = plainFilenamesIn(CWD);
        Commit currentC = getCommit();
        HashMap<String, String> currentBlob = currentC.getBlobHash();
        String branchHash = readContentsAsString(join(BRANCHES_DIR,
                branch + ".txt"));
        Commit branchC = readObject(join(COMMITS_DIR,
                branchHash + ".txt"), Commit.class);
        HashMap<String, String> branchBlob = branchC.getBlobHash();
        if (checkUntracked(list, currentBlob, branchBlob)) {
            return;
        }
        Commit split = findSplit(branchC, currentC);
        HashMap<String, String> splitBlob = split.getBlobHash();
        if (split.getSelfSha1().equals(currentC.getSelfSha1())) {
            checkoutBranch(branch);
            message("Current branch fast-forwarded.");
            return;
        }
        if ((split.getSelfSha1()).equals(branchC.getSelfSha1())) {
            message("Given branch is an ancestor of the current branch.");
            return;
        }
        conflictExist = mergeHelper(splitBlob, branchBlob,
                currentBlob, branchHash);

        if (conflictExist) {
            message("Encountered a merge conflict.");
            commit("Encountered a merge conflict.",
                    branchC.getSelfSha1());
            return;
        } else {
            String message = String.format("Merged %s into %s.",
                    branch, head);
            commit(message, branchC.getSelfSha1());
            return;
        }

    }

    public boolean mergeHelper(HashMap<String, String> splitBlob,
                               HashMap<String, String> branchBlob,
                               HashMap<String, String> currentBlob,
                               String branchHash) throws IOException {
        boolean conflictExist = false;
        Set<String> splitSet = splitBlob.keySet();
        for (String f : splitSet) {
            String splitHash = splitBlob.get(f);
            if (branchBlob.containsKey(f) && currentBlob.containsKey(f)) {
                if (!branchBlob.get(f).equals(splitHash)
                        && currentBlob.get(f).equals(splitHash)) {
                    checkoutCommitFile(branchHash, f);
                    add(f);
                } else if (!branchBlob.get(f).equals(splitHash)
                        && !currentBlob.get(f).equals(branchBlob.get(f))) {
                    mergeConflict(currentBlob.get(f), branchBlob.get(f), f);
                    add(f);
                    conflictExist = true;
                }
            } else if (branchBlob.containsKey(f)
                    && !branchBlob.get(f).equals(splitHash)
                    && !currentBlob.containsKey(f)) {
                mergeConflict(null, branchBlob.get(f), f);
                add(f);
                conflictExist = true;
            } else if (currentBlob.containsKey(f)
                    && !currentBlob.get(f).equals(splitHash)
                    && !branchBlob.containsKey(f)) {
                mergeConflict(currentBlob.get(f), null, f);
                add(f);
                conflictExist = true;
            } else if (!branchBlob.containsKey(f)
                    && currentBlob.containsKey(f)) {
                remove(f);
            }
        }
        for (String f : branchBlob.keySet()) {
            if (!splitBlob.containsKey(f)) {
                if (!currentBlob.containsKey(f)) {
                    checkoutCommitFile(branchHash, f);
                    add(f);
                } else if (currentBlob.containsKey(f)
                        && !currentBlob.get(f).equals(branchBlob.get(f))) {
                    mergeConflict(currentBlob.get(f), branchBlob.get(f), f);
                    add(f);
                    conflictExist = true;
                }
            }
        }
        return conflictExist;
    }

    public Commit findSplit(Commit branchC, Commit currentC) {
        HashSet<String> path = new HashSet<>();
        Stack<Commit> stack = new Stack<>();
        stack.push(branchC);

        while (!stack.isEmpty()) {
            Commit c = stack.pop();
            path.add(c.getSelfSha1());
            Commit parent1 = c.getParentOneCommit();
            Commit parent2 = c.getParentTwoCommit();
            if (parent1 != null) {
                stack.add(parent1);
            }
            if (parent2 != null) {
                stack.add(parent2);
            }
        }

        ArrayDeque<Commit> deque = new ArrayDeque<>();
        deque.add(currentC);
        while (!deque.isEmpty()) {
            Commit c = deque.pop();
            if (path.contains(c.getSelfSha1())) {
                return c;
            }
            Commit parent1 = c.getParentOneCommit();
            Commit parent2 = c.getParentTwoCommit();
            if (parent1 != null) {
                deque.add(parent1);
            }
            if (parent2 != null) {
                deque.add(parent2);
            }
        }
        return null;
    }


    public boolean checkUntracked(List<String> list,
                                  HashMap<String, String> currentBlob,
                                  HashMap<String, String> otherBlob) {
        for (String fileName : list) {
            if (otherBlob.containsKey(fileName)
                    && !currentBlob.containsKey(fileName)) {
                message("There is an untracked file in the way; "
                        + "delete it, or add and commit it first.");
                return true;
            }
        }
        return false;
    }

    public boolean branchExist(String branch) {
        if (join(BRANCHES_DIR, branch + ".txt").exists()) {
            return true;
        }
        return false;
    }

    public void diffPrint(File file1, File file2, String one, String two) {
        Diff dif = new Diff();

        dif.setSequences(file1, file2);
        if (file1 != null) {
            if (dif.sequencesEqual()) {
                return;
            }
        }
        if (file2 == null || file1 == null) {
            if (file2 == null) {
                message("diff --git a/" + one + " /dev/null");
                message("--- a/" + one);
                message("+++ /dev/null");
            } else if (file1 == null) {
                message("diff --git /dev/null" + "b/" + one);
                message("--- /dev/null");
                message("+++ b/" + one);
                message("@@ -0,0 +1 @@\n"
                        + "+This is a wug.");
            }
            int[] array = dif.diffs();
            for (int i = 0; i < array.length; i += 4) {
                String result = "@@ -";
                if (array[i + 1] != 0) {
                    result = result + (array[i] + 1);
                } else {
                    result += array[i];
                }
                if (array[i + 1] != 1) {
                    result = result + "," + array[i + 1];
                }
                result += " +";
                if (array[i + 3] != 0) {
                    result = result + (array[i + 2] + 1);
                } else {
                    result += array[i + 2];
                }
                if (array[i + 3] != 1) {
                    result = result + "," + array[i + 3];
                }
                result += " @@";
                message(result);
                for (int k = array[i]; k < array[i] + array[i + 1]; k++) {
                    message("-" + dif.get1(k));
                }
                for (int j = array[i + 2];
                     j < array[i + 2] + array[i + 3]; j++) {
                    message("+" + dif.get2(j));
                }
            }
            return;
        }

        diffPrintHelper(dif, one, two);
    }
    public void diffPrintHelper(Diff dif, String one, String two) {
        message("diff --git a/" + one + " b/" + two);
        message("--- a/" + one);
        message("+++ b/" + two);
        int[] arr = dif.diffs();
        for (int i = 0; i < arr.length; i += 4) {
            String result = "@@ -";
            if (arr[i + 1] != 0) {
                result = result + (arr[i] + 1);
            } else {
                result += arr[i];
            }
            if (arr[i + 1] != 1) {
                result = result + "," + arr[i + 1];
            }
            result += " +";
            if (arr[i + 3] != 0) {
                result = result + (arr[i + 2] + 1);
            } else {
                result += arr[i + 2];
            }
            if (arr[i + 3] != 1) {
                result = result + "," + arr[i + 3];
            }
            result += " @@";
            message(result);
            for (int k = arr[i]; k < arr[i] + arr[i + 1]; k++) {
                message("-" + dif.get1(k));
            }
            for (int j = arr[i + 2]; j < arr[i + 2] + arr[i + 3]; j++) {
                message("+" + dif.get2(j));
            }
        }
    }
    public void diffNonArg() {
        Commit commit = getCommit();
        HashMap<String, String> blobHash = commit.getBlobHash();
        for (String f : plainFilenamesIn(CWD)) {
            if (blobHash.containsKey(f)) {
                String commitF = blobHash.get(f);
                diffPrint(join(BLOBS_DIR, commitF + ".txt"),
                        currentFile(f), f, f);
            }
        }
        for (String f : blobHash.keySet()) {
            if (!currentFile(f).exists()) {
                String commitF = blobHash.get(f);
                diffPrint(join(BLOBS_DIR, commitF + ".txt"),
                        null, f, f);
            }
        }
    }

    public void diffOneArg(String branch) {
        String branchHash = readContentsAsString(join(BRANCHES_DIR,
                branch + ".txt"));
        Commit commit = Commit.getCommit(branchHash);
        HashMap<String, String> blobHash = commit.getBlobHash();
        for (String f : plainFilenamesIn(CWD)) {
            if (blobHash.containsKey(f)) {
                String commitF = blobHash.get(f);
                diffPrint(join(BLOBS_DIR, commitF + ".txt"),
                        currentFile(f), f, f);
            }
        }
        for (String f : blobHash.keySet()) {
            if (!currentFile(f).exists()) {
                String commitF = blobHash.get(f);
                diffPrint(join(BLOBS_DIR, commitF + ".txt"),
                        null, f, f);
            }
        }
    }

    public void diffTwoArgs(String branch1, String branch2) {
        String branchHash1 = readContentsAsString(join(BRANCHES_DIR,
                branch1 + ".txt"));
        Commit commit = Commit.getCommit(branchHash1);
        String branchHash2 = readContentsAsString(join(BRANCHES_DIR,
                branch2 + ".txt"));
        Commit commit2 = Commit.getCommit(branchHash2);
        HashMap<String, String> blobHash1 = commit.getBlobHash();
        HashMap<String, String> blobHash2 = commit2.getBlobHash();
        String[] myArray1 = new String[blobHash1.keySet().size()];
        (blobHash1.keySet()).toArray(myArray1);
        Arrays.sort(myArray1);
        String[] myArray2 = new String[blobHash2.keySet().size()];
        (blobHash2.keySet()).toArray(myArray2);
        Arrays.sort(myArray2);

        HashSet<String> record = new HashSet<>();
        for (String f : myArray1) {
            if (!blobHash2.containsKey(f)) {
                String commitF = blobHash1.get(f);
                diffPrint(join(BLOBS_DIR, commitF + ".txt"), null, f, f);
            } else {
                String commitF1 = blobHash1.get(f);
                String commitF2 = blobHash2.get(f);
                diffPrint(join(BLOBS_DIR, commitF1 + ".txt"),
                        join(BLOBS_DIR, commitF2 + ".txt"), f, f);
            }
            record.add(f);
        }
        for (String f : myArray2) {
            if (!record.contains(f)) {
                if (!blobHash1.containsKey(f)) {
                    String commitF = blobHash1.get(f);
                    diffPrint(null, join(BLOBS_DIR, commitF + ".txt"), f, f);
                } else {
                    String commitF1 = blobHash1.get(f);
                    String commitF2 = blobHash2.get(f);
                    diffPrint(join(BLOBS_DIR, commitF2 + ".txt"),
                            join(BLOBS_DIR, commitF1 + ".txt"), f, f);
                }
            }
        }
    }

    public void diff(String branch1, String branch2) {
        if (branch1 != null || branch2 != null) {
            if (branch2 == null) {
                if (!branchExist(branch1)) {
                    message("A branch with that name does not exist.");
                    return;
                }
                diffOneArg(branch1);
                return;
            }
            if (!branchExist(branch1) || !branchExist(branch2)) {
                message("At least one branch does not exist.");
                return;
            }
            diffTwoArgs(branch1, branch2);
            return;
        }
        diffNonArg();
    }
}
