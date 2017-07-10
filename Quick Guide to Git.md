# setting up Git
1. Download and install Git.
2. Navigate to the correct directory: ~/workspace/diagnosticSuite. This can be done through the `cd` ('change directory') and `dir` (or `ls` for *nix) commands.
3. Initialize the local Git repository through the `init` command of Git.
4. Initialize the GitHub repository as a remote using the [.git URL](https://github.com/sbansal21/AlcatrazDiagnosticSuite.git).

# Git commands
`git remote add <name> <.git URL>`&emsp;&ensp;adds a remote locally  
`git init`&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&ensp;&thinsp;initializes a local Git repo  
`git log`&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&thinsp;lists previous commits  
`git add <file>`&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&ensp;&thinsp;&thinsp;adds a file to the staging area  
`git commit -m <message>`&emsp;&emsp;&emsp;&emsp;&emsp;&ensp;&thinsp;commits a set of changes to the local repo  
`git push <remote> <branch>`&emsp;&emsp;&emsp;&emsp;&thinsp;pushs a commit or set of commits to the shared repos  
`git pull <remote> <branch>`&emsp;&emsp;&emsp;&emsp;&thinsp;pulls all new commits from the shared to the local repo  

# ground rules
1. Don't commit incremental changes (e.g. refactoring)--only valid changes that affect functionality (e.g. bug fixes, added functionality).
2. Commit messages should describe the changes and their purposes (see previous commits).
3. Don't use the '-f' (force) flag for anything affecting the shared repository, and don't rebase.