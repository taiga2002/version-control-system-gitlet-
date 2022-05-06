# Gitlet Design Document
author: **Taiga Kitao**

## Design Document Guidelines

Please use the following format for your Gitlet design document. Your design
document should be written in markdown, a language that allows you to nicely 
format and style a text file. Organize your design document in a way that 
will make it easy for you or a course-staff member to read.  

## 1. Classes and Data Structures

###Main
Deals with argument type and call functions from Repo class

###Repo Class
####Fields
- private String head (initilized to "master")
- private Stage stage
- private File currDir

###Commit Class
####Fields
- private String message: message for each commit
- private String timestamp: time when this commit was created
- private String selfSha1
- private String parentSha1:
- private Blob blob
- private Hashmap commitHash
- private Hashmap<String, String> blobHash


###Stage Class
####Fields
- private HashMap<String, String> addHash
- private HashMap<String, String> deleteHash
- private HashMap<String, String> noncategorizedHash

## 2. Algorithms

###Repo Class
- Initialize: init
- Add: add [file name]
- Commit: commit [message]
- Remove: rm [file name]
- Log: log
- -Global Log: global-log
- Find: find [commit message]
- Status: status
- Checkout:
  * checkout [file name]
  * checkout [commit id] [file name]
  * checkout [branch name]
- Branch: branch [branch name]
- Remove: rm-branch [branch name]
- Reset: reset [commit id]
- Merge: merge [branch name]
- Rebase: rebase [branch name]
###Commit Class
- getMessage
- getTimestamp
- getSelfSha1
- getParentSha1
- getBlob
- getCommitHash
- getBlobHash
###Stage Class
- add
- delete
- garbage
## 3. Persistence
- .gitlet
  * .commit
  * .addStage
  * .deleteStage

## 4. Design Diagram


