# rk-capture.ps1 — builds the project, runs every experiment of labs 1–7 and saves
# the console logs, HTTP responses and page screenshots into .\rk-evidence
# Usage (from the project folder):  powershell -ExecutionPolicy Bypass -File tools\rk-capture.ps1

param([string]$Only = '')
$ErrorActionPreference = 'Continue'
[Console]::OutputEncoding = [Text.Encoding]::UTF8
$OutputEncoding = [Text.Encoding]::UTF8

$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

# The project was copied without .mvn and .git: restore both from tools\
$wrapperDir = Join-Path $root '.mvn\wrapper'
if (-not (Test-Path (Join-Path $wrapperDir 'maven-wrapper.properties'))) {
    New-Item -ItemType Directory -Force -Path $wrapperDir | Out-Null
    Copy-Item (Join-Path $PSScriptRoot 'maven-wrapper.properties') $wrapperDir
}
$bundle = Join-Path $PSScriptRoot 'spring-lab.bundle'
if (Test-Path $bundle) {
    if (-not (Test-Path (Join-Path $root '.git'))) {
        git init -q
        git remote add origin https://github.com/Alilishka3/spring-lab-01.git
    }
    git fetch -q --update-head-ok $bundle '+refs/heads/*:refs/heads/*'
    git symbolic-ref HEAD refs/heads/rk1
    git reset -q
}
$ev = Join-Path $root 'rk-evidence'
$B = 'http://localhost:8080'
if ($Only -ne 'experiments') { Remove-Item -Recurse -Force $ev -ErrorAction SilentlyContinue }
New-Item -ItemType Directory -Force -Path $ev | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $ev 'shots') | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $ev 'html') | Out-Null
$bodies = Join-Path $ev 'bodies'
New-Item -ItemType Directory -Force -Path $bodies | Out-Null

function Note([string]$text) { Write-Host "== $text" -ForegroundColor Cyan }

# ---------- Java ----------
$java = (Get-Command java -ErrorAction SilentlyContinue).Source
$jdk = Get-ChildItem "$env:USERPROFILE\.jdks" -Directory -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -match '25' } | Select-Object -First 1
if (-not $env:JAVA_HOME -and $jdk) { $env:JAVA_HOME = $jdk.FullName }
if ($env:JAVA_HOME) { $java = Join-Path $env:JAVA_HOME 'bin\java.exe'; $env:Path = "$env:JAVA_HOME\bin;$env:Path" }
Note "java: $java"

if ($Only -ne 'experiments') {
# ---------- environment (lab 1, task 1) ----------
$envFile = Join-Path $ev 'env.txt'
"PS> java -version" | Out-File $envFile -Encoding utf8
& $java -version 2>&1 | ForEach-Object { "$_" } | Out-File $envFile -Append -Encoding utf8
"`r`nPS> javac -version" | Out-File $envFile -Append -Encoding utf8
& (Join-Path (Split-Path $java) 'javac.exe') -version 2>&1 | ForEach-Object { "$_" } | Out-File $envFile -Append -Encoding utf8
"`r`nPS> git --version" | Out-File $envFile -Append -Encoding utf8
git --version 2>&1 | ForEach-Object { "$_" } | Out-File $envFile -Append -Encoding utf8
"`r`nPS> .\mvnw.cmd -version" | Out-File $envFile -Append -Encoding utf8
& .\mvnw.cmd -version 2>&1 | ForEach-Object { "$_" } | Out-File $envFile -Append -Encoding utf8
"`r`nPS> git config --list | findstr user" | Out-File $envFile -Append -Encoding utf8
git config --list 2>&1 | Select-String 'user\.' | ForEach-Object { "$_" } | Out-File $envFile -Append -Encoding utf8

git log --oneline --graph --all --decorate 2>&1 | ForEach-Object { "$_" } | Out-File (Join-Path $ev 'git-log.txt') -Encoding utf8

}
# ---------- build ----------
Note 'building the jar'
& .\mvnw.cmd -q -DskipTests clean package 2>&1 | ForEach-Object { "$_" } | Out-File (Join-Path $ev 'build.log') -Encoding utf8
$jar = Get-ChildItem (Join-Path $root 'target') -Filter '*.jar' | Where-Object { $_.Name -notmatch 'original' } | Select-Object -First 1
if (-not $jar) { Note 'BUILD FAILED - see rk-evidence\build.log'; exit 1 }
$jar = $jar.FullName

# ---------- helpers ----------
function Start-App([string]$name, [string[]]$appArgs = @(), [hashtable]$envs = @{}, [string[]]$jvmArgs = @(), [string]$jarPath = $jar) {
    foreach ($k in $envs.Keys) { [Environment]::SetEnvironmentVariable($k, $envs[$k], 'Process') }
    $log = Join-Path $ev "$name.log"
    $all = @('-Dstdout.encoding=UTF-8', '-Dstderr.encoding=UTF-8', '-Duser.language=en', '-Duser.country=US') + $jvmArgs + @('-jar', $jarPath) + $appArgs
    $p = Start-Process -FilePath $java -ArgumentList $all -RedirectStandardOutput $log -RedirectStandardError "$log.err" -PassThru -NoNewWindow
    foreach ($k in $envs.Keys) { [Environment]::SetEnvironmentVariable($k, $null, 'Process') }
    for ($i = 0; $i -lt 120; $i++) {
        Start-Sleep -Milliseconds 500
        if ($p.HasExited) { break }
        if ((Test-Path $log) -and (Select-String -Path $log -Pattern 'Started SpringLab01Application|Tomcat started on port' -Quiet)) { break }
    }
    Start-Sleep -Milliseconds 700
    return $p
}
function Stop-App($p) {
    if ($p -and -not $p.HasExited) { Stop-Process -Id $p.Id -Force -ErrorAction SilentlyContinue }
    Start-Sleep -Seconds 2
}
function Req([string]$file, [string]$title, [string[]]$curlArgs) {
    $out = & curl.exe -s -i --max-time 20 @curlArgs 2>&1 | Out-String
    $shown = ($curlArgs | ForEach-Object { if ($_ -match '\s') { "`"$_`"" } else { $_ } }) -join ' '
    $block = "### $title`r`n`$ curl -i $shown`r`n$out`r`n"
    $path = Join-Path $ev $file
    for ($try = 0; $try -lt 20; $try++) {      # antivirus may hold the file for a moment
        try { [IO.File]::AppendAllText($path, $block, (New-Object Text.UTF8Encoding($false))); break }
        catch { Start-Sleep -Milliseconds 250 }
    }
}
function Body([string]$name, [string]$json) {
    $path = Join-Path $bodies $name
    [IO.File]::WriteAllText($path, $json, (New-Object Text.UTF8Encoding($false)))
    return "@$path"
}
$edge = @("${env:ProgramFiles(x86)}\Microsoft\Edge\Application\msedge.exe", "$env:ProgramFiles\Microsoft\Edge\Application\msedge.exe") |
        Where-Object { Test-Path $_ } | Select-Object -First 1
function Shot([string]$name, [string]$url, [int]$w = 1440, [int]$h = 1000) {
    if (-not $edge) { return }
    $png = Join-Path $ev "shots\$name.png"
    $prof = Join-Path $env:TEMP 'rk-edge-profile'
    & $edge --headless=new --disable-gpu --hide-scrollbars --no-first-run "--user-data-dir=$prof" "--window-size=$w,$h" "--screenshot=$png" "--virtual-time-budget=4000" $url 2>&1 | Out-Null
    Start-Sleep -Milliseconds 300
}
# Saves the HTML a POST produced and screenshots it (the page keeps its CSS through <base>).
function ShotHtml([string]$name, [string]$html, [int]$w = 1440, [int]$h = 1000) {
    $html = $html -replace '<head>', '<head><base href="http://localhost:8080/">'
    $file = Join-Path $ev "html\$name.html"
    [IO.File]::WriteAllText($file, $html, (New-Object Text.UTF8Encoding($false)))
    Shot $name ("file:///" + ($file -replace '\\', '/')) $w $h
}
function PostForm([string]$jar, [string]$url, [string]$data, [switch]$follow) {
    $a = @('-s', '-b', $jar, '-c', $jar)
    if ($follow) { $a += '-L' }
    foreach ($pair in $data.Split('&')) { $a += @('--data-urlencode', $pair) }
    $a += $url
    return (& curl.exe @a 2>&1 | Out-String)
}

if ($Only -ne 'experiments') {
# =====================================================================
# RUN A — no profile: labs 1, 2, 3, 4, 5, 6, 7
# =====================================================================
Note 'run A: default profile'
$A = Start-App 'runA-default'

# lab 1
Req 'lab1.txt' 'GET /api/hello' @("$B/api/hello")
Req 'lab1.txt' 'GET /api/hello?name=IITU' @("$B/api/hello?name=IITU")
Req 'lab1.txt' 'GET /api/info' @("$B/api/info")
Req 'lab1.txt' 'GET /api/none' @("$B/api/none")
Req 'lab1.txt' 'GET / (start page)' @('-o', 'NUL', '-w', 'HTTP %{http_code}', "$B/")
Req 'lab1-variant.txt' 'reverse: Spring' @("$B/api/reverse?text=Spring")
Req 'lab1-variant.txt' 'reverse: IITU 2026' @("$B/api/reverse?text=IITU%202026")
Req 'lab1-variant.txt' 'reverse: racecar' @("$B/api/reverse?text=racecar")
Req 'lab1-variant.txt' 'reverse: missing parameter' @("$B/api/reverse")

# lab 2
Req 'lab2.txt' 'GET /api/lab2/notify?text=IITU' @("$B/api/lab2/notify?text=IITU")
Req 'lab2.txt' 'GET /api/lab2/lifecycle' @("$B/api/lab2/lifecycle")
Req 'lab2.txt' 'GET /api/lab2/scopes (first request)' @("$B/api/lab2/scopes")
Req 'lab2.txt' 'GET /api/lab2/scopes (second request)' @("$B/api/lab2/scopes")
Req 'lab2-variant.txt' 'custom: Hello' @("$B/api/lab2/custom?text=Hello")
Req 'lab2-variant.txt' 'custom: Spring Framework' @("$B/api/lab2/custom?text=Spring%20Framework")
Req 'lab2-variant.txt' 'custom: IITU-2409' @("$B/api/lab2/custom?text=IITU-2409")

# lab 3
Req 'lab3-config.txt' 'no profile' @("$B/api/lab3/config")
Req 'lab3-actuator.txt' 'value source, no overrides' @("$B/actuator/env/app.mail.retry-count")
Req 'lab3-actuator.txt' 'health' @("$B/actuator/health")

# lab 4 (each call is separated in the log by a marker request)
Req 'lab4.txt' 'GET /api/lab4/items?limit=5' @("$B/api/lab4/items?limit=5")
Req 'lab4.txt' 'GET /api/lab4/item/1' @("$B/api/lab4/item/1")
Req 'lab4.txt' 'DELETE /api/lab4/item/0' @('-X', 'DELETE', "$B/api/lab4/item/0")
Req 'lab4.txt' 'DELETE /api/lab4/item/5' @('-X', 'DELETE', "$B/api/lab4/item/5")
Req 'lab4.txt' 'GET /api/lab4/remove-twice/5' @("$B/api/lab4/remove-twice/5")
Req 'lab4.txt' 'GET /api/lab4/proxy' @("$B/api/lab4/proxy")
Req 'lab4.txt' 'GET /api/lab4/stats' @("$B/api/lab4/stats")

# pages before the data changes (lab 5, lab 6, final site)
Note 'screenshots'
Shot 'site-home' "$B/" 1440 1300
Shot 'site-books' "$B/books?lang=en" 1440 1500
Shot 'site-books-bloch' "$B/books?author=Bloch" 1440 1000
Shot 'site-books-genre' "$B/books?genre=NOVEL" 1440 1000
Shot 'site-book-1' "$B/books/1" 1440 900
Shot 'site-form-en' "$B/books/new?lang=en" 1440 1100
Shot 'site-form-ru' "$B/books/new?lang=ru" 1440 1100
Shot 'site-404' "$B/books/999" 1440 800
Shot 'site-books-mobile' "$B/books" 390 1700
Shot 'site-css' "$B/css/app.css" 1000 500
Shot 'swagger' "$B/swagger-ui.html" 1440 1500

# lab 5 — REST operations and status codes
Req 'lab5-rest.txt' 'GET /api/books' @("$B/api/books")
Req 'lab5-rest.txt' 'GET /api/books?author=Bloch' @("$B/api/books?author=Bloch")
Req 'lab5-rest.txt' 'GET /api/books/1' @("$B/api/books/1")
Req 'lab5-rest.txt' 'GET /api/books/999' @("$B/api/books/999")
$pro = Body 'pro-spring.json' '{"title":"Pro Spring 6","author":"Iuliana Cosmina","year":2023,"genre":"PROGRAMMING"}'
Req 'lab5-rest.txt' 'POST /api/books' @('-X', 'POST', '-H', 'Content-Type: application/json', '--data-binary', $pro, "$B/api/books")
Req 'lab5-rest.txt' 'DELETE /api/books/2 (first)' @('-X', 'DELETE', "$B/api/books/2")
Req 'lab5-rest.txt' 'DELETE /api/books/2 (second)' @('-X', 'DELETE', "$B/api/books/2")
Req 'lab5-errors.txt' 'GET /api/books/abc' @("$B/api/books/abc")
Req 'lab5-errors.txt' 'GET /api/nothing' @("$B/api/nothing")
Req 'lab5-errors.txt' 'DELETE /api/books (no identifier)' @('-X', 'DELETE', "$B/api/books")
Req 'lab5-errors.txt' 'POST /api/books with Content-Type: text/plain' @('-X', 'POST', '-H', 'Content-Type: text/plain', '--data-binary', $pro, "$B/api/books")
Req 'lab5-errors.txt' 'GET /api/books with Accept: application/xml' @('-H', 'Accept: application/xml', "$B/api/books")
Req 'lab5-variant.txt' 'search: java' @("$B/api/books/search?title=java")
Req 'lab5-variant.txt' 'search: SPRING (case-insensitive)' @("$B/api/books/search?title=SPRING")
Req 'lab5-variant.txt' 'search: nothing matches' @("$B/api/books/search?title=kotlin")
Req 'lab5-static.txt' 'GET /css/app.css (no controller)' @('-I', "$B/css/app.css")

# lab 6 — the form, validation, languages, advice
Note 'lab 6 form'
$jarEn = Join-Path $ev 'cookies-en.txt'; $jarRu = Join-Path $ev 'cookies-ru.txt'
& curl.exe -s -c $jarEn -b $jarEn "$B/books/new?lang=en" | Out-Null
& curl.exe -s -c $jarRu -b $jarRu "$B/books/new?lang=ru" | Out-Null
ShotHtml 'form-empty-title' (PostForm $jarEn "$B/books" 'title=&author=Joshua Bloch&year=2018&genre=PROGRAMMING') 1440 1150
ShotHtml 'form-year-1200-no-genre' (PostForm $jarEn "$B/books" 'title=Old Book&author=Unknown&year=1200&genre=') 1440 1200
ShotHtml 'form-no-genre' (PostForm $jarEn "$B/books" 'title=Dune&author=Frank Herbert&year=1965&genre=') 1440 1150
ShotHtml 'form-isbn-digits' (PostForm $jarEn "$B/books" 'title=Refactoring&author=Fowler 2018&year=2018&genre=PROGRAMMING&isbn=978-0134757590') 1440 1200
ShotHtml 'form-duplicate' (PostForm $jarEn "$B/books" 'title=Effective Java&author=Joshua Bloch&year=2018&genre=PROGRAMMING') 1440 1100
ShotHtml 'form-ru-empty' (PostForm $jarRu "$B/books" 'title=&author=&year=0&genre=') 1440 1250
ShotHtml 'form-success' (PostForm $jarEn "$B/books" 'title=Refactoring&author=Martin Fowler&year=2018&genre=PROGRAMMING&isbn=978-0134757599&available=true' -follow) 1440 900
Req 'lab6-advice.txt' 'GET /books/999 (browser page)' @('-H', 'Accept: text/html', "$B/books/999")
Req 'lab6-advice.txt' 'GET /api/books/999 (API)' @("$B/api/books/999")
Req 'lab6-prg.txt' 'POST /books (valid form) — no -L, shows the 302' @('-X', 'POST', '--data-urlencode', 'title=Java Precisely', '--data-urlencode', 'author=Peter Sestoft', '--data-urlencode', 'year=2016', '--data-urlencode', 'genre=PROGRAMMING', "$B/books")

# lab 7 — REST API
Note 'lab 7 api'
$valid = Body 'valid.json' '{"title":"Domain-Driven Design","author":"Eric Evans","year":2003,"genre":"PROGRAMMING","isbn":"978-0321125217"}'
$empty = Body 'empty-title.json' '{"title":"","author":"Eric Evans","year":2003,"genre":"PROGRAMMING"}'
$dupe = Body 'duplicate.json' '{"title":"Spring in Action","author":"Craig Walls","year":2022,"genre":"PROGRAMMING"}'
Req 'lab7-crud.txt' 'GET /api/books' @("$B/api/books")
Req 'lab7-crud.txt' 'GET /api/books/1' @("$B/api/books/1")
Req 'lab7-crud.txt' 'GET /api/books/999' @("$B/api/books/999")
Req 'lab7-crud.txt' 'POST /api/books (valid)' @('-X', 'POST', '-H', 'Content-Type: application/json', '--data-binary', $valid, "$B/api/books")
$newId = ((& curl.exe -s "$B/api/books/search?title=Domain") | ConvertFrom-Json)[0].id
Req 'lab7-crud.txt' "GET /api/books/$newId (following Location)" @("$B/api/books/$newId")
Req 'lab7-crud.txt' "PUT /api/books/$newId" @('-X', 'PUT', '-H', 'Content-Type: application/json', '--data-binary', (Body 'put.json' '{"title":"Domain-Driven Design","author":"Eric Evans","year":2004,"genre":"PROGRAMMING","isbn":"978-0321125217"}'), "$B/api/books/$newId")
Req 'lab7-crud.txt' 'DELETE /api/books/1 (first)' @('-X', 'DELETE', "$B/api/books/1")
Req 'lab7-crud.txt' 'DELETE /api/books/1 (second)' @('-X', 'DELETE', "$B/api/books/1")
Req 'lab7-errors.txt' 'GET /api/books/999' @("$B/api/books/999")
Req 'lab7-errors.txt' 'POST with an empty title' @('-X', 'POST', '-H', 'Content-Type: application/json', '--data-binary', $empty, "$B/api/books")
Req 'lab7-errors.txt' 'POST of a book that already exists' @('-X', 'POST', '-H', 'Content-Type: application/json', '--data-binary', $dupe, "$B/api/books")
Req 'lab7-variant.txt' 'page 0, size 5' @("$B/api/books?page=0&size=5")
Req 'lab7-variant.txt' 'page 2, size 5' @("$B/api/books?page=2&size=5")
Req 'lab7-variant.txt' 'page 0, default size (app.pagination.default-size)' @("$B/api/books?page=0")
Req 'lab7-variant.txt' 'page -1 (invalid)' @("$B/api/books?page=-1&size=5")
& curl.exe -s "$B/v3/api-docs" | Out-File (Join-Path $ev 'openapi.json') -Encoding utf8
Shot 'site-books-after' "$B/books" 1440 1500
Stop-App $A

# =====================================================================
# Lab 1: another port; lab 3: profiles, precedence, validation, --debug
# =====================================================================
Note 'lab 1 port 9090'
$p = Start-App 'lab1-port9090' @('--server.port=9090')
Req 'lab1-port.txt' 'GET http://localhost:9090/api/hello' @('http://localhost:9090/api/hello')
Stop-App $p

Note 'lab 3 profiles'
$p = Start-App 'lab3-dev' @('--spring.profiles.active=dev')
Req 'lab3-config.txt' 'dev profile (port 8081)' @('http://localhost:8081/api/lab3/config')
Stop-App $p
$p = Start-App 'lab3-prod' @('--spring.profiles.active=prod')
Req 'lab3-config.txt' 'prod profile' @("$B/api/lab3/config")
Stop-App $p

Note 'lab 3 precedence'
$p = Start-App 'lab3-env' @() @{ APP_MAIL_RETRYCOUNT = '7' }
Req 'lab3-precedence.txt' 'yml + environment variable APP_MAIL_RETRYCOUNT=7' @("$B/api/lab3/config")
Req 'lab3-actuator.txt' 'value source with the environment variable' @("$B/actuator/env/app.mail.retry-count")
Stop-App $p
$p = Start-App 'lab3-env-sysprop' @() @{ APP_MAIL_RETRYCOUNT = '7' } @('-Dapp.mail.retry-count=5')
Req 'lab3-precedence.txt' 'yml + variable 7 + system property 5' @("$B/api/lab3/config")
Stop-App $p
$p = Start-App 'lab3-env-arg' @('--app.mail.retry-count=9') @{ APP_MAIL_RETRYCOUNT = '7' }
Req 'lab3-precedence.txt' 'yml + variable 7 + command line argument 9' @("$B/api/lab3/config")
Req 'lab3-actuator.txt' 'value source with the command line argument' @("$B/actuator/env/app.mail.retry-count")
Stop-App $p
$p = Start-App 'lab3-dev-arg' @('--spring.profiles.active=dev', '--app.mail.retry-count=9')
Req 'lab3-precedence.txt' 'dev profile + command line argument 9' @('http://localhost:8081/api/lab3/config')
Stop-App $p

Note 'lab 3 validation failure'
$p = Start-App 'lab3-retry99' @('--app.mail.retry-count=99')
Stop-App $p
Note 'lab 3 --debug'
$p = Start-App 'lab3-debug' @('--debug')
Stop-App $p

}

# =====================================================================
# Experiments that need a code change: done on a temporary copy
# =====================================================================
function Build-Copy([string]$name, [scriptblock]$patch) {
    $dir = Join-Path $env:TEMP "rk-$name"
    Remove-Item -Recurse -Force $dir -ErrorAction SilentlyContinue
    New-Item -ItemType Directory -Force -Path $dir | Out-Null
    Copy-Item -Recurse (Join-Path $root 'src'), (Join-Path $root '.mvn'), (Join-Path $root 'pom.xml'), (Join-Path $root 'mvnw.cmd') $dir
    & $patch $dir
    Push-Location $dir
    $blog = Join-Path $ev "$name-build.log"
    "dir: $dir" | Out-File $blog -Encoding utf8
    Get-ChildItem -Force $dir | ForEach-Object { "  $($_.Name)" } | Out-File $blog -Append -Encoding utf8
    & cmd.exe /c "cd /d `"$dir`" && mvnw.cmd -B -DskipTests package 2>&1" | ForEach-Object { "$_" } | Out-File $blog -Append -Encoding utf8
    Pop-Location
    $built = Get-ChildItem (Join-Path $dir 'target') -Filter '*.jar' -ErrorAction SilentlyContinue | Where-Object { $_.Name -notmatch 'original' } | Select-Object -First 1
    if ($built) { return $built.FullName } else { return $null }
}

Note 'lab 2 without @Primary'
$j = Build-Copy 'no-primary' { param($d)
    $f = Join-Path $d 'src\main\java\kz\iitu\springlab\notify\EmailNotifier.java'
    [IO.File]::WriteAllText($f, ((Get-Content $f -Raw -Encoding UTF8) -replace '(?m)^@Primary', '// @Primary'), (New-Object Text.UTF8Encoding($false))) }
if ($j) { $p = Start-App 'lab2-no-primary' -jarPath $j; Stop-App $p }

Note 'lab 4 self-invocation before the fix'
$j = Build-Copy 'self-invocation' { param($d)
    $f = Join-Path $d 'src\main\java\kz\iitu\springlab\service\CatalogService.java'
    [IO.File]::WriteAllText($f, ((Get-Content $f -Raw -Encoding UTF8) -replace 'self\.remove\(', 'remove('), (New-Object Text.UTF8Encoding($false))) }
if ($j) {
    $p = Start-App 'lab4-before-fix' -jarPath $j
    Req 'lab4-before-fix.txt' 'DELETE /api/lab4/item/5 (external call)' @('-X', 'DELETE', "$B/api/lab4/item/5")
    Req 'lab4-before-fix.txt' 'GET /api/lab4/remove-twice/5 (self-invocation)' @("$B/api/lab4/remove-twice/5")
    Stop-App $p
}

Note "done: $ev"
