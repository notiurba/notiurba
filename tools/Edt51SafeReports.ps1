# Dot-source only. Export whitelisted JUnit fields; never copy properties, output or exception text.
function Export-Edt51SafeReports {
    param([object[]]$Files, [string]$Destination, [string]$ExpectedClass)
    $totals = @{ Tests=0; Failures=0; Errors=0; Skipped=0 }
    foreach ($file in $Files) {
        $settings = [Xml.XmlReaderSettings]::new()
        $settings.DtdProcessing = [Xml.DtdProcessing]::Prohibit
        $settings.XmlResolver = $null
        $reader = [Xml.XmlReader]::Create($file.FullName, $settings)
        $raw = [Xml.XmlDocument]::new()
        try { $raw.Load($reader) } finally { $reader.Dispose() }
        if ($raw.DocumentElement.GetAttribute('name') -cne $ExpectedClass) { continue }
        $safe = [Xml.XmlDocument]::new()
        $suite = $safe.CreateElement('testsuite')
        $safe.AppendChild($suite) | Out-Null
        $suite.SetAttribute('name', $ExpectedClass)
        foreach ($case in $raw.SelectNodes('/testsuite/testcase')) {
            $testName = $case.GetAttribute('name')
            if ($testName -cnotmatch '^[A-Za-z][A-Za-z0-9_]*$') { throw 'Unexpected test name; report suppressed.' }
            $test = $safe.CreateElement('testcase')
            $test.SetAttribute('name',$testName)
            $test.SetAttribute('classname',$ExpectedClass)
            $suite.AppendChild($test) | Out-Null
            $totals.Tests++
            foreach ($kind in @('failure','error','skipped')) {
                if ($case.SelectSingleNode($kind)) {
                    $item = $safe.CreateElement($kind)
                    $item.SetAttribute('message','Details omitted to protect external credentials and signed URLs.')
                    $test.AppendChild($item) | Out-Null
                    $field = @{ failure='Failures'; error='Errors'; skipped='Skipped' }[$kind]
                    $totals[$field]++
                }
            }
        }
        foreach ($field in @('Tests','Failures','Errors','Skipped')) { $suite.SetAttribute($field.ToLowerInvariant(),[string]$totals[$field]) }
        $safe.Save((Join-Path $Destination ('TEST-' + $ExpectedClass + '.xml')))
    }
    return $totals
}
