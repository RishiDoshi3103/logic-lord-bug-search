$tests = @(
  "sample.txt",
  "empty.txt",
  "badmode.txt",
  "noint.txt",
  "oddnums.txt",
  "oob_rs.txt",
  "corner.txt",
  "dup_rs.txt",
  "overlap.txt",
  "fully_searched_skip.txt",
  "sleep_stress.txt",
  "boundary.txt",
  "mode_row.txt",
  "mode_col.txt"
)

# compile once
javac .\search.java
if ($LASTEXITCODE -ne 0) { throw "Compilation failed." }

foreach ($t in $tests) {
  Write-Host "`n=== Running $t ==="
  try {
    java search $t
  } catch {
    Write-Host "❌ Crash on $t"
  }
}
