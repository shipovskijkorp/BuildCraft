[CmdletBinding()]
param(
    [Parameter(Position = 0)]
    [ValidateSet("init", "add", "update")]
    [string]$Action = "update",

    [Parameter(Position = 1)]
    [string]$Branch,

    [string]$Path,
    [string]$RemoteName = "origin",
    [string]$MainBranch = "main",
    [string]$VersionBranchPattern = '^\d+\.\d+(?:\.\d+)?-(?:forge|neoforge|fabric|quilt)$',
    [string]$RepositoryUrl,
    [switch]$AllowAnyBranch,
    [switch]$NoPush
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version 2.0

function Invoke-Git {
    param(
        [Parameter(Mandatory = $true)]
        [string[]]$Arguments,
        [switch]$Capture,
        [switch]$AllowFailure
    )

    $output = @(& git @Arguments 2>&1)
    $exitCode = $LASTEXITCODE

    if ($exitCode -ne 0 -and -not $AllowFailure) {
        $message = ($output | ForEach-Object { $_.ToString() }) -join [Environment]::NewLine
        throw "git $($Arguments -join ' ') failed with exit code $exitCode.$([Environment]::NewLine)$message"
    }

    if ($Capture) {
        return @($output | ForEach-Object { $_.ToString() })
    }

    foreach ($line in $output) {
        Write-Host $line
    }

    return $exitCode
}

function Get-OneGitLine {
    param(
        [Parameter(Mandatory = $true)]
        [string[]]$Arguments,
        [switch]$AllowFailure
    )

    $lines = @(Invoke-Git -Arguments $Arguments -Capture -AllowFailure:$AllowFailure)
    if ($lines.Count -eq 0) {
        return ""
    }

    return $lines[0].Trim()
}

function Resolve-RepositoryUrl {
    if ($RepositoryUrl) {
        return $RepositoryUrl
    }

    $url = Get-OneGitLine -Arguments @("remote", "get-url", $RemoteName)

    if ($url -match '^git@github\.com:(.+)$') {
        return "https://github.com/$($Matches[1])"
    }

    if ($url -match '^ssh://git@github\.com/(.+)$') {
        return "https://github.com/$($Matches[1])"
    }

    return $url
}

function Assert-RouterPath {
    param([Parameter(Mandatory = $true)][string]$RouterPath)

    if ([string]::IsNullOrWhiteSpace($RouterPath)) {
        throw "Router path cannot be empty."
    }

    if ([System.IO.Path]::IsPathRooted($RouterPath) -or $RouterPath.Contains("..")) {
        throw "Unsafe router path: $RouterPath"
    }

    if ($RouterPath -match '\s') {
        throw "Spaces and control characters are not supported in router paths: $RouterPath"
    }
}

function Get-RemoteBranchSha {
    param([Parameter(Mandatory = $true)][string]$RemoteBranch)

    $sha = Get-OneGitLine -Arguments @(
        "rev-parse",
        "--verify",
        "refs/remotes/$RemoteName/$RemoteBranch^{commit}"
    ) -AllowFailure

    if (-not $sha) {
        throw "Remote branch '$RemoteBranch' was not found on '$RemoteName'. Push it first."
    }

    return $sha
}

function Get-IndexMode {
    param([Parameter(Mandatory = $true)][string]$RouterPath)

    $line = Get-OneGitLine -Arguments @("ls-files", "-s", "--", $RouterPath) -AllowFailure
    if (-not $line) {
        return ""
    }

    if ($line -match '^(\d+)\s+') {
        return $Matches[1]
    }

    return ""
}

function Get-CurrentGitlinkSha {
    param([Parameter(Mandatory = $true)][string]$RouterPath)

    $line = Get-OneGitLine -Arguments @("ls-files", "-s", "--", $RouterPath) -AllowFailure
    if ($line -match '^160000\s+([0-9a-fA-F]+)\s+') {
        return $Matches[1]
    }

    return ""
}

function Add-RouterLink {
    param(
        [Parameter(Mandatory = $true)][string]$RemoteBranch,
        [Parameter(Mandatory = $true)][string]$RouterPath
    )

    Assert-RouterPath -RouterPath $RouterPath

    if (-not $AllowAnyBranch -and $RemoteBranch -notmatch $VersionBranchPattern) {
        throw "Branch '$RemoteBranch' does not match VersionBranchPattern: $VersionBranchPattern"
    }

    $sha = Get-RemoteBranchSha -RemoteBranch $RemoteBranch
    $mode = Get-IndexMode -RouterPath $RouterPath

    if ($mode -and $mode -ne "160000") {
        throw "Path '$RouterPath' is already tracked as a normal file or directory."
    }

    if (Test-Path -LiteralPath $RouterPath) {
        $firstChild = Get-ChildItem -LiteralPath $RouterPath -Force -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($firstChild -and $mode -ne "160000") {
            throw "Directory '$RouterPath' is not empty. Move or remove it before adding the router link."
        }
    }
    else {
        New-Item -ItemType Directory -Path $RouterPath | Out-Null
    }

    if (-not (Test-Path -LiteralPath ".gitmodules")) {
        New-Item -ItemType File -Path ".gitmodules" | Out-Null
    }

    $url = Resolve-RepositoryUrl
    Invoke-Git -Arguments @("config", "-f", ".gitmodules", "submodule.$RouterPath.path", $RouterPath) | Out-Null
    Invoke-Git -Arguments @("config", "-f", ".gitmodules", "submodule.$RouterPath.url", $url) | Out-Null
    Invoke-Git -Arguments @("config", "-f", ".gitmodules", "submodule.$RouterPath.branch", $RemoteBranch) | Out-Null
    Invoke-Git -Arguments @("config", "-f", ".gitmodules", "submodule.$RouterPath.router", "true") | Out-Null
    Invoke-Git -Arguments @("add", ".gitmodules") | Out-Null
    Invoke-Git -Arguments @("update-index", "--add", "--cacheinfo", "160000", $sha, $RouterPath) | Out-Null

    Write-Host "Registered '$RouterPath' -> '$RemoteBranch' @ $($sha.Substring(0, [Math]::Min(12, $sha.Length)))"
}

function Initialize-RouterLinks {
    $refs = @(Invoke-Git -Arguments @(
        "for-each-ref",
        "--format=%(refname:short)",
        "refs/remotes/$RemoteName/"
    ) -Capture)

    foreach ($refLine in $refs) {
        $ref = $refLine.Trim()
        if (-not $ref -or $ref -eq "$RemoteName/HEAD") {
            continue
        }

        $prefix = "$RemoteName/"
        if (-not $ref.StartsWith($prefix)) {
            continue
        }

        $remoteBranch = $ref.Substring($prefix.Length)
        if ($remoteBranch -eq $MainBranch) {
            continue
        }

        if ($remoteBranch -match $VersionBranchPattern) {
            Add-RouterLink -RemoteBranch $remoteBranch -RouterPath $remoteBranch
        }
    }
}

function Update-RouterLinks {
    if (-not (Test-Path -LiteralPath ".gitmodules")) {
        Write-Host "No .gitmodules file found; nothing to update."
        return
    }

    $pathLines = @(Invoke-Git -Arguments @(
        "config",
        "-f",
        ".gitmodules",
        "--get-regexp",
        '^submodule\..*\.path$'
    ) -Capture -AllowFailure)

    foreach ($line in $pathLines) {
        if ($line -notmatch '^submodule\.(.+)\.path\s+(.+)$') {
            continue
        }

        $name = $Matches[1]
        $routerPath = $Matches[2].Trim()
        Assert-RouterPath -RouterPath $routerPath

        $managed = Get-OneGitLine -Arguments @(
            "config", "-f", ".gitmodules", "--get", "submodule.$name.router"
        ) -AllowFailure

        if ($managed -ne "true") {
            continue
        }

        $remoteBranch = Get-OneGitLine -Arguments @(
            "config", "-f", ".gitmodules", "--get", "submodule.$name.branch"
        ) -AllowFailure

        if (-not $remoteBranch) {
            $remoteBranch = $routerPath
        }

        $sha = Get-RemoteBranchSha -RemoteBranch $remoteBranch
        $current = Get-CurrentGitlinkSha -RouterPath $routerPath

        if (-not (Test-Path -LiteralPath $routerPath)) {
            New-Item -ItemType Directory -Path $routerPath | Out-Null
        }

        if ($current -ne $sha) {
            Invoke-Git -Arguments @("update-index", "--add", "--cacheinfo", "160000", $sha, $routerPath) | Out-Null
            $oldShort = if ($current) { $current.Substring(0, [Math]::Min(12, $current.Length)) } else { "none" }
            $newShort = $sha.Substring(0, [Math]::Min(12, $sha.Length))
            Write-Host "Updated '$routerPath': $oldShort -> $newShort"
        }
        else {
            Write-Host "Current '$routerPath': $($sha.Substring(0, [Math]::Min(12, $sha.Length)))"
        }
    }
}

if (-not (Get-Command git -ErrorAction SilentlyContinue)) {
    throw "Git was not found in PATH."
}

$repoRoot = Get-OneGitLine -Arguments @("rev-parse", "--show-toplevel")
Set-Location -LiteralPath $repoRoot

$dirty = @(Invoke-Git -Arguments @("status", "--porcelain") -Capture)
if ($dirty.Count -gt 0) {
    throw "The working tree is not clean. Commit or stash your changes before running this script."
}

$originalBranch = Get-OneGitLine -Arguments @("branch", "--show-current")

try {
    Invoke-Git -Arguments @("fetch", "--prune", $RemoteName, "+refs/heads/*:refs/remotes/$RemoteName/*") | Out-Null
    Invoke-Git -Arguments @("switch", $MainBranch) | Out-Null
    Invoke-Git -Arguments @("pull", "--ff-only", $RemoteName, $MainBranch) | Out-Null

    switch ($Action) {
        "add" {
            if (-not $Branch) {
                throw "Use -Branch <name> with the add action."
            }

            $routerPath = if ($Path) { $Path } else { $Branch }
            Add-RouterLink -RemoteBranch $Branch -RouterPath $routerPath
            Update-RouterLinks
        }
        "init" {
            Initialize-RouterLinks
            Update-RouterLinks
        }
        "update" {
            Update-RouterLinks
        }
    }

    & git diff --cached --quiet
    $diffExitCode = $LASTEXITCODE

    if ($diffExitCode -eq 0) {
        Write-Host "Nothing changed."
    }
    elseif ($diffExitCode -eq 1) {
        $commitMessage = if ($Action -eq "update") {
            "Update version branch links"
        }
        else {
            "Add or update version branch links"
        }

        Invoke-Git -Arguments @("commit", "-m", $commitMessage) | Out-Null

        if (-not $NoPush) {
            Invoke-Git -Arguments @("push", $RemoteName, "HEAD:$MainBranch") | Out-Null
            Write-Host "Changes were pushed to '$MainBranch'."
        }
        else {
            Write-Host "Changes were committed locally. Push '$MainBranch' manually."
        }
    }
    else {
        throw "git diff --cached --quiet failed with exit code $diffExitCode."
    }
}
finally {
    if ($originalBranch -and $originalBranch -ne $MainBranch) {
        Invoke-Git -Arguments @("switch", $originalBranch) -AllowFailure | Out-Null
    }
}
