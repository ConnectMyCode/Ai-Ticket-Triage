# Git Cheat Sheet: Frontend Branch Setup and Safe Pull

## 1. Create the repo and push the `frontend` branch

Run from `C:\AISupportTicketSystemTriage\frontend-support-ticket-triage`:

```
git init
git branch -M frontend
git add .
git status
git commit -m "Initial commit: frontend support ticket triage"
git remote add origin <same-url-as-backend-repo>
git push -u origin frontend
```

| Command | What it does |
|---|---|
| `git init` | Creates the local repo |
| `git branch -M frontend` | Renames the current branch to `frontend` |
| `git add .` | Stages all files (respecting `.gitignore`) |
| `git status` | Confirms `node_modules` and `.idea` are not listed |
| `git commit -m "..."` | Saves the first snapshot |
| `git remote add origin <url>` | Links the local repo to the remote |
| `git push -u origin frontend` | Creates the `frontend` branch on the remote and links it |

## 2. Create the working branch

```
git switch -c frontend-dev
git push -u origin frontend-dev
```

Do daily work on `frontend-dev`. Open a Pull Request into `frontend` (not `master`) when ready.

## 3. Daily workflow

```
git switch frontend-dev
git add .
git commit -m "Describe your change"
git push
```

## 4. Safe pull from remote

```
git status
git fetch origin
git log --oneline HEAD..origin/frontend
git diff HEAD origin/frontend
git merge origin/frontend
```

| Command | What it does |
|---|---|
| `git status` | Confirms your working tree is clean first |
| `git fetch origin` | Downloads remote changes without touching your files or branch |
| `git log --oneline HEAD..origin/frontend` | Lists the commits you're about to receive |
| `git diff HEAD origin/frontend` | Shows what would change |
| `git merge origin/frontend` | Applies the changes only when you're happy with them |

Replace `frontend` with your current branch name if you're pulling on a different branch (for example `origin/frontend-dev`).

## 5. Handy extras

```
git branch                  # show current branch (marked with *)
git pull --ff-only          # safe one-line pull, refuses if histories diverged
git merge --abort           # undo a merge in progress
git stash / git stash pop   # set aside uncommitted work, then restore it
```