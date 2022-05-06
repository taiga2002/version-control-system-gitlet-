package gitlet;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Set;

public class Stage implements Serializable {
    /**HashMap of staged added blob files with fileNames keys.*/
    private HashMap<String, String> addHash;
    /**HashMap of staged deleted blob files with fileNames keys.*/
    private HashMap<String, String> deleteHash;
    /**HashMap of nonCategorized blob files with fileNames keys.*/
    private HashMap<String, String> nonCategorizedHash;

    public Stage() {
        this.addHash = new HashMap<>();
        this.deleteHash = new HashMap<>();
        this.nonCategorizedHash = new HashMap<>();
    }

    public HashMap<String, String> getAdd() {
        return this.addHash;
    }

    public HashMap<String, String> getDelete() {
        return this.deleteHash;
    }

    public HashMap<String, String> getAll() {
        HashMap<String, String> allMap = new HashMap<>();
        allMap.putAll(addHash);
        allMap.putAll(deleteHash);
        return allMap;
    }

    public Set<String> getAllSet() {
        return getAll().keySet();
    }

    public Set<String> getAddedSet() {
        return getAdd().keySet();
    }

    public Set<String> getDeletedSet() {
        return getDelete().keySet();
    }

    public HashMap getNonCategorized() {
        return this.nonCategorizedHash;
    }

    public void add(String fileName, String sha1) {
        addHash.put(fileName, sha1);
    }

    public void delete(String fileName, String sha1) {
        deleteHash.put(fileName, sha1);
    }

    public void removeFromDelete(String fileName) {
        deleteHash.remove(fileName);
    }

    public void clear() {
        this.addHash = new HashMap<>();
        this.deleteHash = new HashMap<>();
        this.nonCategorizedHash = new HashMap<>();
    }

    public boolean checkStage() {
        return addHash.isEmpty() && deleteHash.isEmpty();
    }
}
