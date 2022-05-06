package gitlet;

import java.io.File;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import static gitlet.Utils.*;

/**
 * Commit Class.
 *
 * @author Taiga Kitao.
 */

public class Commit implements Serializable {
    /**
     * Message about commit.
     */
    private String message;
    /**
     * Timestamp when commit was created.
     */
    private String timestamp;
    /**
     * The unique sha1 hash for commit object.
     */
    private String selfSha1;
    /**
     * The parentOne sha1 hash.
     */
    private String parentOne;
    /**
     * The parentTwo sha1 hash.
     */
    private String parentTwo;
    /**
     * The parent list of sha1 hash for merge.
     */
    private String[] parentList = null;
    /**
     * Current file for this specific commit using selfSha1.
     */
    private File currFile;
    /**
     * HashMap: all the blob files hash with fileName as keys.
     */
    private HashMap<String, String> blobHash;

    /**
     * Commit constructor.
     *
     * @param messageP
     * @param parent1
     * @param parent2
     * @param blobHashP
     */

    public Commit(String messageP, String parent1, String parent2,
                  HashMap<String, String> blobHashP) {
        this.message = messageP;
        this.parentOne = parent1;
        this.parentTwo = parent2;
        if (this.parentOne == null) {

            this.timestamp = "Thu Jan 1 00:00:00 1970 +0000";
        } else {
            this.timestamp = dateGenerator();
        }
        this.blobHash = blobHashP;
        this.selfSha1 = createSha1();
        this.currFile = join(Command.COMMITS_DIR, this.selfSha1 + ".txt");
    }

    /*save current file according to the selfSha1.*/
    public void save() {
        writeObject(currFile, this);
    }

    /*get commit object using selfSha1.*/
    public static Commit getCommit(String selfSha1) {
        if (selfSha1 == null) {
            return null;
        }
        return readObject(join(Command.COMMITS_DIR,
                selfSha1 + ".txt"), Commit.class);
    }

    /*return message.*/
    public String getMessage() {
        return message;
    }

    /*return timestamp.*/
    public String getTimestamp() {
        return timestamp;
    }

    /*return parent.*/

    public String getParentOne() {
        return parentOne;
    }
    public Commit getParentOneCommit() {
        return getCommit(parentOne);
    }

    public String getParentTwo() {
        return parentTwo;
    }

    public Commit getParentTwoCommit() {
        return getCommit(parentTwo);
    }

    /* return message.*/
    public String getSelfSha1() {
        return selfSha1;
    }

    /* return blobHash. */
    public HashMap<String, String> getBlobHash() {
        return blobHash;
    }

    public String getBlobHashKey(String fileName) {
        return blobHash.get(fileName);
    }

    /* create sha1 id */
    public String createSha1() {
        return sha1(serialize(this));


    }

    public static String dateGenerator() {
        Date dNow = new Date();
        SimpleDateFormat ft =
                new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy Z");

        String date = ft.format(dNow);
        return date;
    }
}
