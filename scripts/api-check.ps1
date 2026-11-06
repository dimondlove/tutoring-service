
param(
    [string]$Base = "http://localhost:8080",
    [int]$Racers = 8
)

$ErrorActionPreference = "Stop"
[System.Net.ServicePointManager]::DefaultConnectionLimit = 64   # real parallelism on Windows PowerShell 5.1
Add-Type -AssemblyName System.Net.Http
$client = [System.Net.Http.HttpClient]::new()
$client.Timeout = [TimeSpan]::FromSeconds(60)
$inv = [System.Globalization.CultureInfo]::InvariantCulture
$script:Passed = 0
$script:Failed = 0

# ============================ helpers ============================
function Section([string]$Title) { Write-Host ""; Write-Host "== $Title ==" -ForegroundColor Cyan }

function Check([string]$Name, [bool]$Cond, [string]$Info = "") {
    if ($Cond) { $script:Passed++; Write-Host "  [PASS] $Name" -ForegroundColor Green }
    else       { $script:Failed++; Write-Host "  [FAIL] $Name  $Info" -ForegroundColor Red }
}

function Summary {
    Write-Host ""
    $color = if ($script:Failed -eq 0) { "Green" } else { "Red" }
    Write-Host "RESULT: passed $($script:Passed), failed $($script:Failed)" -ForegroundColor $color
}

function Require([bool]$Cond, [string]$Message) {
    if (-not $Cond) { Write-Host "ABORT: $Message" -ForegroundColor Red; Summary; exit 2 }
}

function Api([string]$Method, [string]$Path, [string]$Token = $null, $Body = $null) {
    $req = [System.Net.Http.HttpRequestMessage]::new([System.Net.Http.HttpMethod]::new($Method), "$Base$Path")
    if ($Token) { $req.Headers.Authorization = [System.Net.Http.Headers.AuthenticationHeaderValue]::new("Bearer", $Token) }
    if ($null -ne $Body) {
        $payload = if ($Body -is [string]) { $Body } else { $Body | ConvertTo-Json -Compress -Depth 5 }
        $req.Content = [System.Net.Http.StringContent]::new($payload, [System.Text.Encoding]::UTF8, "application/json")
    }
    $resp = $client.SendAsync($req).GetAwaiter().GetResult()
    $text = $resp.Content.ReadAsStringAsync().GetAwaiter().GetResult()
    $parsed = $null
    if ($text) { try { $parsed = $text | ConvertFrom-Json } catch { $parsed = $null } }
    $cc = $resp.Headers.CacheControl
    [pscustomobject]@{
        Status  = [int]$resp.StatusCode
        Json    = $parsed
        Text    = $text
        NoStore = ($null -ne $cc -and $cc.NoStore)
    }
}

# always returns an array (PowerShell unrolls single items and turns empty arrays into $null)
function Arr($x) { if ($null -eq $x) { return ,@() } return ,@($x) }

function HasKeys($obj, [string[]]$Keys) {
    if ($null -eq $obj) { return $false }
    $names = $obj.PSObject.Properties.Name
    foreach ($k in $Keys) { if ($names -notcontains $k) { return $false } }
    return $true
}

function Login([string]$Email, [string]$Password) { Api "POST" "/api/auth/login" $null @{ email = $Email; password = $Password } }

function Book([string]$Token, $TutorId, $SubjectId, [string]$Start, [int]$Minutes = 60, [bool]$Intro = $false) {
    Api "POST" "/api/bookings" $Token @{ tutorId = $TutorId; subjectId = $SubjectId; startTime = $Start; durationMinutes = $Minutes; intro = $Intro }
}

function Cancel([string]$Token, $Id) { Api "POST" "/api/bookings/$Id/cancel" $Token }

# ---- dates (the demo schedule lives in Moscow time, UTC+3) ----
function MoscowToday { [DateTime]::UtcNow.AddHours(3).Date }
function WorkdayOnOrAfter([DateTime]$d) {
    while ($d.DayOfWeek -eq [DayOfWeek]::Saturday -or $d.DayOfWeek -eq [DayOfWeek]::Sunday) { $d = $d.AddDays(1) }
    $d
}
function DateStr([DateTime]$d) { $d.ToString("yyyy-MM-dd", $inv) }
function MoscowMidnight([DateTime]$d) {
    [DateTimeOffset]::new([DateTime]::SpecifyKind($d.Date.AddHours(-3), [DateTimeKind]::Utc))
}
function Iso([DateTimeOffset]$d) { $d.UtcDateTime.ToString("yyyy-MM-dd'T'HH:mm:ss'Z'", $inv) }
function Shift([string]$Start, [int]$Minutes) { Iso ([DateTimeOffset]::Parse($Start, $inv).AddMinutes($Minutes)) }

function Slots($TutorId, $SubjectId, [DateTime]$Date, [int]$Minutes = 60) {
    $r = Api "GET" "/api/tutors/$TutorId/slots?subjectId=$SubjectId&durationMinutes=$Minutes&date=$(DateStr $Date)"
    $starts = @([regex]::Matches($r.Text, '"start"\s*:\s*"([^"]+)"') | ForEach-Object { $_.Groups[1].Value })
    [pscustomobject]@{ Response = $r; Starts = $starts }
}

# fires all booking requests back-to-back and waits for all of them
function Race($Items) {
    $reqs = @()
    foreach ($it in $Items) {
        $req = [System.Net.Http.HttpRequestMessage]::new([System.Net.Http.HttpMethod]::Post, "$Base/api/bookings")
        $req.Headers.Authorization = [System.Net.Http.Headers.AuthenticationHeaderValue]::new("Bearer", $it.Token)
        $body = @{ tutorId = $it.TutorId; subjectId = $it.SubjectId; startTime = $it.Start; durationMinutes = 60; intro = $false } | ConvertTo-Json -Compress
        $req.Content = [System.Net.Http.StringContent]::new($body, [System.Text.Encoding]::UTF8, "application/json")
        $reqs += $req
    }
    $tasks = @()
    foreach ($req in $reqs) { $tasks += $client.SendAsync($req) }
    [System.Threading.Tasks.Task]::WaitAll([System.Threading.Tasks.Task[]]$tasks)
    foreach ($t in $tasks) {
        $resp = $t.Result
        $text = $resp.Content.ReadAsStringAsync().GetAwaiter().GetResult()
        $code = $null; $id = $null
        if ($text) { try { $j = $text | ConvertFrom-Json; $code = $j.code; $id = $j.id } catch { } }
        [pscustomobject]@{ Status = [int]$resp.StatusCode; Code = $code; Id = $id }
    }
}

# ============================ preflight ============================
try { $health = Api "GET" "/actuator/health" }
catch { Write-Host "Backend is not reachable at $Base. Start it first." -ForegroundColor Red; exit 2 }
Require ($health.Status -eq 200) "health endpoint returned $($health.Status)"

$admin    = Login "admin@demo.com"    "Admin123!"
$student  = Login "student@demo.com"  "Student123!"
$student2 = Login "student2@demo.com" "Student123!"
$tutor1   = Login "tutor1@demo.com"   "Tutor123!"
$tutor2   = Login "tutor2@demo.com"   "Tutor123!"
Require (@($admin, $student, $student2, $tutor1, $tutor2 | Where-Object { $_.Status -ne 200 }).Count -eq 0) `
    "demo accounts cannot log in. Run the backend with the 'demo' profile on an empty database."

$adminT = $admin.Json.token;  $studentT = $student.Json.token;  $student2T = $student2.Json.token
$tutorT = $tutor1.Json.token; $tutor2T  = $tutor2.Json.token
$studentId = $student.Json.user.id
$stamp = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()

# ============================ 1. AUTH ============================
Section "1. Auth"
Check "login admin: role ADMIN"    ($admin.Json.user.role -eq "ADMIN")
Check "login student: role STUDENT" ($student.Json.user.role -eq "STUDENT")
Check "login tutor: role TUTOR"    ($tutor1.Json.user.role -eq "TUTOR")

$email = "chk_$stamp@test.com"
$reg = Api "POST" "/api/auth/register" $null @{ email = $email; password = "Password1!"; fullName = "Check User"; phone = "+70000000000" }
Check "register: 201 Created" ($reg.Status -eq 201) "status=$($reg.Status)"
Check "register: role is always STUDENT" ($reg.Json.user.role -eq "STUDENT")
Check "register: token returned" (-not [string]::IsNullOrEmpty($reg.Json.token))
Check "register: password hash is never exposed" (-not $reg.Text.Contains("passwordHash"))
$newToken = $reg.Json.token; $newId = $reg.Json.user.id

$dupReg = Api "POST" "/api/auth/register" $null @{ email = $email; password = "Password1!"; fullName = "Check User" }
Check "register duplicate email: 409 EMAIL_TAKEN" ($dupReg.Status -eq 409 -and $dupReg.Json.code -eq "EMAIL_TAKEN")
Check "register short password: 400" ((Api "POST" "/api/auth/register" $null @{ email = "s_$stamp@test.com"; password = "123"; fullName = "A" }).Status -eq 400)
Check "register invalid email: 400"  ((Api "POST" "/api/auth/register" $null @{ email = "not-an-email"; password = "Password1!"; fullName = "A" }).Status -eq 400)
Check "login wrong password: 401"    ((Login "student@demo.com" "wrong-password").Status -eq 401)
Check "login unknown user: 401"      ((Login "nobody@demo.com" "Password1!").Status -eq 401)

# ============================ 2. CATALOG ============================
Section "2. Public catalog and slots"
$subj = Api "GET" "/api/subjects"
$subjList = Arr $subj.Json
Check "subjects: 200, not empty, have id and name" ($subj.Status -eq 200 -and $subjList.Count -gt 0 -and (HasKeys $subjList[0] @("id", "name")))

$tut = Api "GET" "/api/tutors?sort=popular"
$cards = Arr $tut.Json
Check "tutors: 200 and not empty" ($tut.Status -eq 200 -and $cards.Count -gt 0)
$card = @($cards | Where-Object { $_.userId -eq $tutor1.Json.user.id })[0]
Require ($null -ne $card) "demo tutor1 is not in the catalog"
Check "tutor card has photo, bio, experience, rating, min price, offers" `
    (HasKeys $card @("fullName", "photoUrl", "bio", "experienceYears", "rating", "ratingCount", "minPricePerHour", "offers"))
$tutorId = $card.id
$offerList = Arr $card.offers
$offer = $offerList[0]
$subjectId = $offer.subjectId
$origPrice = [decimal]$offer.pricePerHour

Check "filter maxPrice=1: empty"        ((Arr (Api "GET" "/api/tutors?maxPrice=1").Json).Count -eq 0)
Check "filter minRating=5.1: empty"     ((Arr (Api "GET" "/api/tutors?minRating=5.1").Json).Count -eq 0)
Check "filter minExperience=100: empty" ((Arr (Api "GET" "/api/tutors?minExperience=100").Json).Count -eq 0)
$bySubject = Arr (Api "GET" "/api/tutors?subjectId=$subjectId").Json
Check "filter by subject: tutor1 included" (@($bySubject | Where-Object { $_.id -eq $tutorId }).Count -eq 1)
$byPrice = Arr (Api "GET" "/api/tutors?sort=price").Json
$prices = @($byPrice | ForEach-Object { [double]$_.minPricePerHour })
Check "sort=price: ascending" (($prices -join ",") -eq (@($prices | Sort-Object) -join ","))
$one = Api "GET" "/api/tutors/$tutorId"
Check "tutor by id: 200" ($one.Status -eq 200 -and $one.Json.id -eq $tutorId)
Check "unknown tutor: 404" ((Api "GET" "/api/tutors/999999").Status -eq 404)

$d1 = WorkdayOnOrAfter ((MoscowToday).AddDays(10))
$d1Str = DateStr $d1
$sl1 = Slots $tutorId $subjectId $d1
Check "slots on a working day: 200 and not empty" ($sl1.Response.Status -eq 200 -and $sl1.Starts.Count -gt 0)
Check "slots response is not cacheable (no-store)" ($sl1.Response.NoStore)
Check "all slot starts are on the 30-minute grid" (@($sl1.Starts | Where-Object { [DateTimeOffset]::Parse($_, $inv).UtcDateTime.Minute % 30 -ne 0 }).Count -eq 0)
$sun = $d1; while ($sun.DayOfWeek -ne [DayOfWeek]::Sunday) { $sun = $sun.AddDays(1) }
Check "slots on Sunday: empty (demo schedule is Mon-Fri)" ((Slots $tutorId $subjectId $sun).Starts.Count -eq 0)
Check "slots with unsupported duration (50): 400" ((Slots $tutorId $subjectId $d1 50).Response.Status -eq 400)
Require ($sl1.Starts.Count -gt 0) "no free slots on $d1Str"
$slotA = $sl1.Starts[0]

# ============================ 3. BOOKING FLOW ============================
Section "3. Booking flow (tutor1, $d1Str)"
$b1 = Book $studentT $tutorId $subjectId $slotA
Check "student books: 201, PENDING, 60 min" ($b1.Status -eq 201 -and $b1.Json.status -eq "PENDING" -and $b1.Json.durationMinutes -eq 60) "status=$($b1.Status) $($b1.Text)"
Require ($b1.Status -eq 201) "cannot continue without the first booking"
Check "price equals hourly price" ([double]$b1.Json.price -eq [double]$origPrice)
$id1 = $b1.Json.id

$dup = Book $student2T $tutorId $subjectId $slotA
Check "same slot, another student: 409 SLOT_TAKEN" ($dup.Status -eq 409 -and $dup.Json.code -eq "SLOT_TAKEN") "status=$($dup.Status)"
Check "overlapping slot (+30 min): 409" ((Book $student2T $tutorId $subjectId (Shift $slotA 30)).Status -eq 409)
$adj = Book $student2T $tutorId $subjectId (Shift $slotA 60)
Check "adjacent slot (+60 min): 201" ($adj.Status -eq 201)
if ($adj.Status -eq 201) { Cancel $student2T $adj.Json.id | Out-Null }

Check "booked slot disappeared from free slots" ((Slots $tutorId $subjectId $d1).Starts -notcontains $slotA)

$up = Api "GET" "/api/bookings/my/upcoming" $studentT
$upList = Arr $up.Json
Check "student upcoming: contains the booking" (@($upList | Where-Object { $_.id -eq $id1 }).Count -eq 1)
Check "student upcoming: only own bookings" (@($upList | Where-Object { $_.student.id -ne $studentId }).Count -eq 0)
Check "student upcoming: date, tutor and subject present" (@($upList | Where-Object { -not $_.startTime -or -not $_.tutor.fullName -or -not $_.subject.name }).Count -eq 0)

$rq = Api "GET" "/api/tutor/requests" $tutorT
$rqList = Arr $rq.Json
$mine = @($rqList | Where-Object { $_.id -eq $id1 })
Check "tutor requests: booking visible" ($rq.Status -eq 200 -and $mine.Count -eq 1)
Check "tutor requests: student card has name, age, phone, email, rating" ($mine.Count -eq 1 -and (HasKeys $mine[0].student @("fullName", "age", "phone", "email", "rating")))

Check "foreign student cancels: 403" ((Cancel $student2T $id1).Status -eq 403)
Check "foreign tutor accepts: 403" ((Api "POST" "/api/tutor/bookings/$id1/accept" $tutor2T).Status -eq 403)

$acc = Api "POST" "/api/tutor/bookings/$id1/accept" $tutorT
Check "tutor accepts: 200, CONFIRMED" ($acc.Status -eq 200 -and $acc.Json.status -eq "CONFIRMED")
Check "accept twice: 409" ((Api "POST" "/api/tutor/bookings/$id1/accept" $tutorT).Status -eq 409)

Check "homework empty: 400" ((Api "PUT" "/api/tutor/bookings/$id1/homework" $tutorT @{ homework = "" }).Status -eq 400)
Check "tutor sets homework: 200" ((Api "PUT" "/api/tutor/bookings/$id1/homework" $tutorT @{ homework = "Do tasks 1-10" }).Status -eq 200)
$up2 = Arr (Api "GET" "/api/bookings/my/upcoming" $studentT).Json
Check "student sees the homework" (@($up2 | Where-Object { $_.id -eq $id1 -and $_.homework -eq "Do tasks 1-10" }).Count -eq 1)

Check "rate before the lesson is done: 409" ((Api "POST" "/api/bookings/$id1/rate" $studentT @{ rating = 5 }).Status -eq 409)
Check "tutor rates student before the lesson: 409" ((Api "POST" "/api/tutor/bookings/$id1/rate-student" $tutorT @{ rating = 5 }).Status -eq 409)
Check "rating 6 is invalid: 400" ((Api "POST" "/api/bookings/$id1/rate" $studentT @{ rating = 6 }).Status -eq 400)

$can = Cancel $studentT $id1
Check "student cancels: 200, CANCELLED, cancelledAt set" ($can.Status -eq 200 -and $can.Json.status -eq "CANCELLED" -and $can.Json.cancelledAt)
Check "cancel twice: 409" ((Cancel $studentT $id1).Status -eq 409)
Check "cancelled slot is free again" ((Slots $tutorId $subjectId $d1).Starts -contains $slotA)

$pastList = Arr (Api "GET" "/api/bookings/my/past" $studentT).Json
Check "student past: has finished/cancelled lessons with a status" ($pastList.Count -gt 0 -and @($pastList | Where-Object { $_.status -notin @("COMPLETED", "CANCELLED", "REJECTED") }).Count -eq 0)

# --- decline path ---
$b2 = Book $student2T $tutorId $subjectId $slotA
Check "booking after cancel: 201" ($b2.Status -eq 201)
if ($b2.Status -eq 201) {
    $dec = Api "POST" "/api/tutor/bookings/$($b2.Json.id)/decline" $tutorT
    Check "tutor declines: 200, REJECTED" ($dec.Status -eq 200 -and $dec.Json.status -eq "REJECTED")
    Check "declined slot is free again" ((Slots $tutorId $subjectId $d1).Starts -contains $slotA)
}

# --- intro lesson ---
$introStart = Shift $slotA 120
$i1 = Book $studentT $tutorId $subjectId $introStart 30 $true
Check "intro lesson: 201, free, 30 min" ($i1.Status -eq 201 -and [double]$i1.Json.price -eq 0 -and $i1.Json.durationMinutes -eq 30 -and $i1.Json.intro -eq $true) "status=$($i1.Status) $($i1.Text)"
$i2 = Book $studentT $tutorId $subjectId (Shift $slotA 240) 30 $true
Check "second intro with the same tutor: 409 INTRO_ALREADY_USED" ($i2.Status -eq 409 -and $i2.Json.code -eq "INTRO_ALREADY_USED")
Check "intro with wrong duration (60): 400" ((Book $studentT $tutorId $subjectId (Shift $slotA 300) 60 $true).Status -eq 400)
if ($i1.Status -eq 201) { Cancel $studentT $i1.Json.id | Out-Null }

# ============================ 4. VALIDATION ============================
Section "4. Booking validation"
Check "unsupported duration (50): 400" ((Book $studentT $tutorId $subjectId (Shift $slotA 360) 50).Status -eq 400)
$late = Iso ((MoscowMidnight $d1).AddHours(20))
Check "outside working hours (20:00): 400" ((Book $studentT $tutorId $subjectId $late).Status -eq 400)
Check "start off the 30-min grid (+7 min): 400" ((Book $studentT $tutorId $subjectId (Shift $slotA 7)).Status -eq 400)
Check "booking in the past: 400" ((Book $studentT $tutorId $subjectId (Iso ([DateTimeOffset]::UtcNow.AddDays(-1)))).Status -eq 400)
Check "unknown tutor: 404" ((Book $studentT 999999 $subjectId $slotA).Status -eq 404)
Check "empty body: 400" ((Api "POST" "/api/bookings" $studentT "{}").Status -eq 400)
Check "tutor cannot create bookings: 403" ((Book $tutorT $tutorId $subjectId $slotA).Status -eq 403)

# ============================ 5. SECURITY ============================
Section "5. Security and roles"
Check "no token: /api/me 401"                 ((Api "GET" "/api/me").Status -eq 401)
Check "garbage token: /api/me 401"            ((Api "GET" "/api/me" "abc.def.ghi").Status -eq 401)
Check "no token: /api/bookings/my/upcoming 401" ((Api "GET" "/api/bookings/my/upcoming").Status -eq 401)
$me = Api "GET" "/api/me" $studentT
Check "student: /api/me 200, own profile" ($me.Status -eq 200 -and $me.Json.email -eq "student@demo.com") "status=$($me.Status)"
$put = Api "PUT" "/api/me" $newToken @{ fullName = "Renamed User"; phone = "+71112223344" }
Check "update own profile: 200" ($put.Status -eq 200 -and $put.Json.fullName -eq "Renamed User") "status=$($put.Status)"
Check "student on /api/admin/users: 403"  ((Api "GET" "/api/admin/users" $studentT).Status -eq 403)
Check "student on /api/admin/stats: 403"  ((Api "GET" "/api/admin/stats" $studentT).Status -eq 403)
Check "student on /api/tutor/requests: 403" ((Api "GET" "/api/tutor/requests" $studentT).Status -eq 403)
Check "tutor on /api/admin/users: 403"    ((Api "GET" "/api/admin/users" $tutorT).Status -eq 403)
Check "admin on /api/tutor/requests: 403" ((Api "GET" "/api/tutor/requests" $adminT).Status -eq 403)
Check "public catalog needs no token"     ((Api "GET" "/api/tutors").Status -eq 200)

Check "admin cannot change own role/status: 400" ((Api "PATCH" "/api/admin/users/$($admin.Json.user.id)" $adminT @{ enabled = $false }).Status -eq 400)
$blk = Api "PATCH" "/api/admin/users/$newId" $adminT @{ enabled = $false }
Check "admin blocks a user: 200, enabled=false" ($blk.Status -eq 200 -and $blk.Json.enabled -eq $false)
Check "blocked user's token stops working at once: 401" ((Api "GET" "/api/me" $newToken).Status -eq 401)
Check "blocked user cannot log in: 401" ((Login $email "Password1!").Status -eq 401)

# ============================ 6. TUTOR CABINET ============================
Section "6. Tutor cabinet"
$prof = Api "GET" "/api/tutor/profile" $tutorT
Check "profile: 200 with name and offers" ($prof.Status -eq 200 -and (HasKeys $prof.Json @("fullName", "offers")))
Check "my subjects: 200, not empty" ((Arr (Api "GET" "/api/tutor/subjects" $tutorT).Json).Count -gt 0)
Check "working hours: 200, not empty" ((Arr (Api "GET" "/api/tutor/working-hours" $tutorT).Json).Count -gt 0)
Check "today's lessons: 200" ((Api "GET" "/api/tutor/lessons/today" $tutorT).Status -eq 200)
$today = MoscowToday
Check "schedule by dates: 200" ((Api "GET" "/api/tutor/schedule?from=$(DateStr $today)&to=$(DateStr $today.AddDays(14))" $tutorT).Status -eq 200)
Check "schedule over 62 days: 400" ((Api "GET" "/api/tutor/schedule?from=$(DateStr $today)&to=$(DateStr $today.AddDays(70))" $tutorT).Status -eq 400)
$studs = Api "GET" "/api/tutor/students" $tutorT
$studList = Arr $studs.Json
Check "my students: 200, cards have name, age, rating" ($studs.Status -eq 200 -and ($studList.Count -eq 0 -or (HasKeys $studList[0] @("fullName", "age", "rating"))))

# price management
$newPrice = $origPrice + 100
$pp = Api "PUT" "/api/tutor/subjects/$subjectId" $tutorT @{ pricePerHour = $newPrice }
Check "tutor changes price: 200" ($pp.Status -eq 200 -and [decimal]$pp.Json.pricePerHour -eq $newPrice)
$cardNow = @(Arr (Api "GET" "/api/tutors").Json | Where-Object { $_.id -eq $tutorId })
$catalogNow = Arr (Api "GET" "/api/tutors").Json
$cardNow = @($catalogNow | Where-Object { $_.id -eq $tutorId })
$offerNow = @(Arr $cardNow[0].offers | Where-Object { $_.subjectId -eq $subjectId })
$offersNow = Arr $cardNow[0].offers
Check "new price is visible in the public catalog" (@($offersNow | Where-Object { $_.subjectId -eq $subjectId -and [decimal]$_.pricePerHour -eq $newPrice }).Count -eq 1)
Check "negative price: 400" ((Api "PUT" "/api/tutor/subjects/$subjectId" $tutorT @{ pricePerHour = -5 }).Status -eq 400)
Check "unknown subject: 404" ((Api "PUT" "/api/tutor/subjects/999999" $tutorT @{ pricePerHour = 100 }).Status -eq 404)
Api "PUT" "/api/tutor/subjects/$subjectId" $tutorT @{ pricePerHour = $origPrice } | Out-Null   # restore

# time off versus bookings; schedule changes must show up in slots at once
$d2 = WorkdayOnOrAfter $d1.AddDays(1)
$d2Str = DateStr $d2
$sl3 = Slots $tutorId $subjectId $d2
Require ($sl3.Starts.Count -gt 0) "no slots on $d2Str"
$slotB = $sl3.Starts[0]
$b3 = Book $studentT $tutorId $subjectId $slotB
Check "booking on $d2Str : 201" ($b3.Status -eq 201)
$offBody = @{ startAt = (Iso (MoscowMidnight $d2)); endAt = (Iso ((MoscowMidnight $d2).AddDays(1))); reason = "check" }
$toBad = Api "POST" "/api/tutor/time-off" $tutorT $offBody
Check "time off over an existing booking: 409 TIME_OFF_HAS_BOOKINGS" ($toBad.Status -eq 409 -and $toBad.Json.code -eq "TIME_OFF_HAS_BOOKINGS")
if ($b3.Status -eq 201) { Cancel $studentT $b3.Json.id | Out-Null }
$toOk = Api "POST" "/api/tutor/time-off" $tutorT $offBody
Check "time off after the booking is cancelled: 201" ($toOk.Status -eq 201)
Check "slots for the day off: empty at once" ((Slots $tutorId $subjectId $d2).Starts.Count -eq 0)
Check "booking on the day off: 400" ((Book $studentT $tutorId $subjectId $slotB).Status -eq 400)
Check "time off is listed" (@(Arr (Api "GET" "/api/tutor/time-off" $tutorT).Json | Where-Object { $_.id -eq $toOk.Json.id }).Count -eq 1)
if ($toOk.Status -eq 201) {
    Check "delete time off: 204" ((Api "DELETE" "/api/tutor/time-off/$($toOk.Json.id)" $tutorT).Status -eq 204)
    Check "slots are back after deleting the day off" ((Slots $tutorId $subjectId $d2).Starts.Count -gt 0)
}

# ============================ 7. ADMIN ============================
Section "7. Admin panel"
$st = Api "GET" "/api/admin/stats" $adminT
Check "stats: 200 with dashboard fields" ($st.Status -eq 200 -and (HasKeys $st.Json @("usersByRole", "bookingsByStatus", "newUsersLast7Days", "upcomingBookingsNext7Days", "cancellationRatePercent", "topTutors", "topSubjects")))
$usersR = Api "GET" "/api/admin/users" $adminT
Check "users: 200, at least 5" ($usersR.Status -eq 200 -and (Arr $usersR.Json).Count -ge 5)
$onlyTutors = Arr (Api "GET" "/api/admin/users?role=TUTOR" $adminT).Json
Check "users filtered by role=TUTOR" ($onlyTutors.Count -ge 2 -and @($onlyTutors | Where-Object { $_.role -ne "TUTOR" }).Count -eq 0)
Check "users search by q" ((Arr (Api "GET" "/api/admin/users?q=student2@demo" $adminT).Json).Count -eq 1)
Check "admin tutors list: 200 with active flag" ((Api "GET" "/api/admin/tutors" $adminT).Status -eq 200 -and (HasKeys (Arr (Api "GET" "/api/admin/tutors" $adminT).Json)[0] @("active")))

$subjName = "Check subject $stamp"
$cs = Api "POST" "/api/admin/subjects" $adminT @{ name = $subjName }
Check "create subject: 201" ($cs.Status -eq 201)
Check "duplicate subject: 409" ((Api "POST" "/api/admin/subjects" $adminT @{ name = $subjName.ToUpper() }).Status -eq 409)
Check "tutor cannot create subjects: 403" ((Api "POST" "/api/admin/subjects" $tutorT @{ name = "X $stamp" }).Status -eq 403)
if ($cs.Status -eq 201) {
    Check "deactivate subject: 204" ((Api "DELETE" "/api/admin/subjects/$($cs.Json.id)" $adminT).Status -eq 204)
    $subjAfter = @(Arr (Api "GET" "/api/subjects").Json | Where-Object { $_.id -eq $cs.Json.id })
    $publicSubjects = Arr (Api "GET" "/api/subjects").Json
    Check "deactivated subject is hidden from the public list" (@($publicSubjects | Where-Object { $_.id -eq $cs.Json.id }).Count -eq 0)
}

$from = DateStr $d1; $to = DateStr $d2
Check "admin schedule: 200" ((Api "GET" "/api/admin/schedule?from=$from&to=$to" $adminT).Status -eq 200)
Check "admin schedule with tutor and status filters: 200" ((Api "GET" "/api/admin/schedule?from=$from&to=$to&tutorId=$tutorId&status=CANCELLED" $adminT).Status -eq 200)
Check "admin sees tutor working hours: 200" ((Api "GET" "/api/admin/tutors/$tutorId/working-hours" $adminT).Status -eq 200)

$ab = Book $studentT $tutorId $subjectId (Shift $slotA 180)
Check "booking for the admin-cancel check: 201" ($ab.Status -eq 201)
if ($ab.Status -eq 201) {
    $ac = Api "POST" "/api/admin/bookings/$($ab.Json.id)/cancel" $adminT
    Check "admin cancels any booking: 200, CANCELLED" ($ac.Status -eq 200 -and $ac.Json.status -eq "CANCELLED")
}

# ============================ 8. CONCURRENCY ============================
Section "8. Double-booking races ($Racers parallel students)"
$racerTokens = @()

for ($i = 1; $i -le $Racers; $i++) {
    $rr = Api "POST" "/api/auth/register" $null @{ email = "race_${stamp}_$i@test.com"; password = "Password1!"; fullName = "Race $i" }
    if ($rr.Status -eq 201) { $racerTokens += $rr.Json.token }
}
Require ($racerTokens.Count -eq $Racers) "could not register $Racers racers"

$d3 = WorkdayOnOrAfter $d2.AddDays(1)
$d3Str = DateStr $d3
$sl5 = Slots $tutorId $subjectId $d3
Require ($sl5.Starts.Count -gt 0) "no slots on $d3Str"
$raceStart = $sl5.Starts[0]

# race 1: everybody wants exactly the same slot
$items = @()
foreach ($tk in $racerTokens) { $items += @{ Token = $tk; TutorId = $tutorId; SubjectId = $subjectId; Start = $raceStart } }
$res = @(Race $items)
$ok = @($res | Where-Object { $_.Status -eq 201 })
$conflicts = @($res | Where-Object { $_.Status -eq 409 })
$other = @($res | Where-Object { $_.Status -ne 201 -and $_.Status -ne 409 })
Check "race 1 (same slot): exactly one 201" ($ok.Count -eq 1) "got $($ok.Count)"
Check "race 1: all others got 409" ($conflicts.Count -eq ($Racers - 1)) "got $($conflicts.Count)"
Check "race 1: no 4xx/5xx other than 409" ($other.Count -eq 0) "statuses: $(($other | ForEach-Object { $_.Status }) -join ',')"
Check "race 1: conflict code is SLOT_TAKEN" (@($conflicts | Where-Object { $_.Code -ne "SLOT_TAKEN" }).Count -eq 0)
$dbRows = Arr (Api "GET" "/api/admin/schedule?from=$d3Str&to=$d3Str&tutorId=$tutorId&status=PENDING" $adminT).Json
Check "race 1: exactly one booking stored in the database" (@($dbRows | Where-Object { $_.startTime }).Count -eq 1)
foreach ($o in $ok) { Api "POST" "/api/admin/bookings/$($o.Id)/cancel" $adminT | Out-Null }

# race 2: different starts whose intervals overlap (10:00-11:00 vs 10:30-11:30 style)
$starts2 = @($raceStart, (Shift $raceStart 30))
$items = @()
for ($i = 0; $i -lt $racerTokens.Count; $i++) { $items += @{ Token = $racerTokens[$i]; TutorId = $tutorId; SubjectId = $subjectId; Start = $starts2[$i % 2] } }
$res = @(Race $items)
$ok = @($res | Where-Object { $_.Status -eq 201 })
$conflicts = @($res | Where-Object { $_.Status -eq 409 })
$other = @($res | Where-Object { $_.Status -ne 201 -and $_.Status -ne 409 })
Check "race 2 (overlapping intervals): exactly one 201" ($ok.Count -eq 1) "got $($ok.Count)"
Check "race 2: all others got 409, no 5xx" ($conflicts.Count -eq ($Racers - 1) -and $other.Count -eq 0)
foreach ($o in $ok) { Api "POST" "/api/admin/bookings/$($o.Id)/cancel" $adminT | Out-Null }

# race 3: ONE student, two tutors, same time (student overlap is enforced by the database constraint)
$card2 = @($cards | Where-Object { $_.userId -eq $tutor2.Json.user.id })[0]
$common = @(Arr $card2.offers | Where-Object { $_.subjectId -eq $subjectId })
if ($null -ne $card2 -and $common.Count -gt 0) {
    $items = @(
        @{ Token = $racerTokens[0]; TutorId = $tutorId;    SubjectId = $subjectId; Start = $raceStart },
        @{ Token = $racerTokens[0]; TutorId = $card2.id;   SubjectId = $subjectId; Start = $raceStart }
    )
    $res = @(Race $items)
    $ok = @($res | Where-Object { $_.Status -eq 201 })
    $conflicts = @($res | Where-Object { $_.Status -eq 409 })
    Check "race 3 (one student, two tutors): exactly one 201" ($ok.Count -eq 1) "statuses: $(($res | ForEach-Object { $_.Status }) -join ',')"
    Check "race 3: the other one is 409 STUDENT_BUSY" ($conflicts.Count -eq 1 -and $conflicts[0].Code -eq "STUDENT_BUSY")
    foreach ($o in $ok) { Api "POST" "/api/admin/bookings/$($o.Id)/cancel" $adminT | Out-Null }
}
else { Write-Host "  [SKIP] race 3: tutor2 does not teach the same subject" -ForegroundColor Yellow }

# after cleanup the slot must be bookable again
$again = Book $racerTokens[1] $tutorId $subjectId $raceStart
Check "slot is bookable again after the races are cleaned up" ($again.Status -eq 201)
if ($again.Status -eq 201) { Api "POST" "/api/admin/bookings/$($again.Json.id)/cancel" $adminT | Out-Null }

# ============================ summary ============================
Summary
if ($script:Failed -gt 0) { exit 1 } else { exit 0 }