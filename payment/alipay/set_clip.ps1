Add-Type -AssemblyName System.Windows.Forms
Add-Type -AssemblyName System.Drawing
$path = 'C:\Users\lzhao3730\Desktop\github\_tmp_alipay_keys\app_icon_320.png'
$img = [System.Drawing.Image]::FromFile($path)
[System.Windows.Forms.Clipboard]::SetImage($img)
$img.Dispose()
Write-Output 'clipboard-set-ok'
