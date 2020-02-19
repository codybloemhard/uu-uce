# Git Workflow
## Install git
- install git: ```sudo pacman -Sy git``` or equivalent on your OS
- make sure git is in your path ```echo "$PATH"```
## Starting a new task
- make sure you are on master ```git branch```
- if not go to master ```git checkout master```
- make sure your master is up to date: ```git pull origin master```
- make a new branch to work on: ```git checkout -b branch-name```
- make sure that you are on the new branch: ```git branch```
- you can now work on that branch
- commit as normal
- push to your branch: ```git push origin branch-name```
- see if you see your code passes the CI, goto the github page -> actions and see if it passes
## Ending a task
- make sure your branch passes the CI and your code is tidy
- Try linting your code and writing good unit test
- if you go to git -> branch: branch-name you can click ```New pull request```
- write a comment in the pull request with a small summary of what it is
- I will review, you can assign me as reviewer (ocdy1001)
- The code will be reviewed, refactored. It may be that you are asked to change to more things before it will be merged.
- Your branch will be merged
- When you are done with the branch, you can follow the steps in ```Starting a new task``` to start a new task.
