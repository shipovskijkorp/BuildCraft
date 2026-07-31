#!/usr/bin/env bash
set -euo pipefail

ACTION="${1:-update}"
BRANCH="${2:-}"
REMOTE_NAME="${REMOTE_NAME:-origin}"
MAIN_BRANCH="${MAIN_BRANCH:-main}"
VERSION_BRANCH_REGEX="${VERSION_BRANCH_REGEX:-^[0-9]+\.[0-9]+(\.[0-9]+)?-(forge|neoforge|fabric|quilt)$}"
REPOSITORY_URL="${ROUTER_REPOSITORY_URL:-}"

fail() {
  echo "ERROR: $*" >&2
  exit 1
}

require_git_repo() {
  git rev-parse --is-inside-work-tree >/dev/null 2>&1 || fail "Run this script inside a Git repository."
}

resolve_repository_url() {
  if [[ -n "$REPOSITORY_URL" ]]; then
    printf '%s\n' "$REPOSITORY_URL"
    return
  fi

  local url
  url="$(git remote get-url "$REMOTE_NAME")"

  if [[ "$url" =~ ^git@github\.com:(.+)$ ]]; then
    printf 'https://github.com/%s\n' "${BASH_REMATCH[1]}"
  elif [[ "$url" =~ ^ssh://git@github\.com/(.+)$ ]]; then
    printf 'https://github.com/%s\n' "${BASH_REMATCH[1]}"
  else
    printf '%s\n' "$url"
  fi
}

fetch_branches() {
  git fetch --prune "$REMOTE_NAME" "+refs/heads/*:refs/remotes/$REMOTE_NAME/*"
}

remote_branch_sha() {
  local branch="$1"
  git rev-parse --verify "refs/remotes/$REMOTE_NAME/$branch^{commit}" 2>/dev/null \
    || fail "Remote branch '$branch' was not found on '$REMOTE_NAME'. Push it first."
}

validate_path() {
  local path="$1"
  [[ -n "$path" ]] || fail "Empty submodule path."
  [[ "$path" != /* ]] || fail "Absolute paths are not allowed: $path"
  [[ "$path" != *".."* ]] || fail "Path traversal is not allowed: $path"
  [[ "$path" != *$'\n'* && "$path" != *$'\r'* && "$path" != *$'\t'* && "$path" != *' '* ]] \
    || fail "Spaces and control characters are not supported in router paths: $path"
}

add_router_link() {
  local branch="$1"
  local path="${2:-$branch}"
  local allow_any="${ALLOW_ANY_BRANCH:-false}"

  [[ -n "$branch" ]] || fail "Branch name is required for the add action."
  validate_path "$path"

  if [[ "$allow_any" != "true" ]] && ! [[ "$branch" =~ $VERSION_BRANCH_REGEX ]]; then
    fail "Branch '$branch' does not match VERSION_BRANCH_REGEX: $VERSION_BRANCH_REGEX"
  fi

  local sha repo_url mode
  sha="$(remote_branch_sha "$branch")"
  repo_url="$(resolve_repository_url)"
  mode="$(git ls-files -s -- "$path" | awk 'NR == 1 { print $1 }')"

  if [[ -n "$mode" && "$mode" != "160000" ]]; then
    fail "Path '$path' is already tracked as a normal file or directory."
  fi

  if [[ -d "$path" ]] && [[ -n "$(find "$path" -mindepth 1 -maxdepth 1 -print -quit 2>/dev/null)" ]] && [[ "$mode" != "160000" ]]; then
    fail "Directory '$path' is not empty. Move or remove it before adding the router link."
  fi

  mkdir -p "$path"
  touch .gitmodules

  git config -f .gitmodules "submodule.$path.path" "$path"
  git config -f .gitmodules "submodule.$path.url" "$repo_url"
  git config -f .gitmodules "submodule.$path.branch" "$branch"
  git config -f .gitmodules "submodule.$path.router" "true"
  git add .gitmodules
  git update-index --add --cacheinfo 160000 "$sha" "$path"

  echo "Registered '$path' -> '$branch' @ ${sha:0:12}"
}

init_router_links() {
  local ref branch
  while IFS= read -r ref; do
    [[ -n "$ref" ]] || continue
    [[ "$ref" == "$REMOTE_NAME/HEAD" ]] && continue

    branch="${ref#"$REMOTE_NAME/"}"
    [[ "$branch" == "$MAIN_BRANCH" ]] && continue

    if [[ "$branch" =~ $VERSION_BRANCH_REGEX ]]; then
      add_router_link "$branch" "$branch"
    fi
  done < <(git for-each-ref --format='%(refname:short)' "refs/remotes/$REMOTE_NAME/")
}

update_router_links() {
  [[ -f .gitmodules ]] || {
    echo "No .gitmodules file found; nothing to update."
    return
  }

  local line key name path managed branch sha current
  while IFS= read -r line; do
    [[ -n "$line" ]] || continue

    key="${line%% *}"
    path="${line#* }"
    name="${key#submodule.}"
    name="${name%.path}"
    validate_path "$path"

    managed="$(git config -f .gitmodules --get "submodule.$name.router" 2>/dev/null || true)"
    [[ "$managed" == "true" ]] || continue

    branch="$(git config -f .gitmodules --get "submodule.$name.branch" 2>/dev/null || true)"
    [[ -n "$branch" ]] || branch="$path"

    sha="$(remote_branch_sha "$branch")"
    current="$(git ls-files -s -- "$path" | awk '$1 == "160000" { print $2; exit }')"

    mkdir -p "$path"

    if [[ "$current" != "$sha" ]]; then
      git update-index --add --cacheinfo 160000 "$sha" "$path"
      echo "Updated '$path': ${current:0:12} -> ${sha:0:12}"
    else
      echo "Current '$path': ${sha:0:12}"
    fi
  done < <(git config -f .gitmodules --get-regexp '^submodule\..*\.path$' 2>/dev/null || true)
}

require_git_repo
fetch_branches

case "$ACTION" in
  add)
    add_router_link "$BRANCH" "$BRANCH"
    ;;
  init)
    init_router_links
    update_router_links
    ;;
  update)
    update_router_links
    ;;
  *)
    fail "Unknown action '$ACTION'. Use: init, add <branch>, or update."
    ;;
esac
