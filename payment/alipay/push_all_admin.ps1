$ErrorActionPreference = 'Stop'
$baseKeys = $PSScriptRoot
$alipayPub = (Get-Content -Raw (Join-Path $baseKeys 'alipay_public_key_body.txt')).Trim()
$blogPriv = (Get-Content -Raw (Join-Path $baseKeys 'blog\app_private_key_pkcs8.pem'))
$blogPrivBody = (($blogPriv -split "`n") | Where-Object { $_ -notmatch 'PRIVATE KEY' } | ForEach-Object { $_.Trim() }) -join ''

function Get-Body($path) {
  (Get-Content -Raw $path).Trim()
}

function Login-Token($name, $base, $path, $body, $tokenFrom) {
  $r = Invoke-RestMethod -Method Post -Uri ($base + $path) -ContentType 'application/json; charset=utf-8' -Body ($body | ConvertTo-Json -Compress) -TimeoutSec 30
  switch ($tokenFrom) {
    'data.access_token' { return $r.data.access_token }
    'data.token' { return $r.data.token }
    'data' { return $r.data }
    'result.token' { return $r.result.token }
    'result.access_token' { return $r.result.access_token }
    'result' {
      if ($r.result.jwt_token) { return $r.result.jwt_token }
      if ($r.result.access_token) { return $r.result.access_token }
      if ($r.result.token) { return $r.result.token }
      return $r.result
    }
    default { throw "unknown tokenFrom $tokenFrom" }
  }
}

function Post-Json($base, $path, $token, $obj) {
  $h = @{ Authorization = "Bearer $token"; 'Content-Type' = 'application/json; charset=utf-8' }
  return Invoke-RestMethod -Method Post -Uri ($base + $path) -Headers $h -Body ($obj | ConvertTo-Json -Depth 8 -Compress) -TimeoutSec 40
}

$apps = @(
  @{
    key='blog'; appId='2021006159661313'; admin='https://dev.admin.bgssai-blog.com'; user='https://dev.user.bgssai-blog.com'
    priv=$blogPrivBody; name='BGSSAIBlog'
    loginPath='/api/auth/login'; loginBody=@{ username_or_email='admin'; password='admin123456' }; tokenFrom='data.access_token'
    notify='/api/payments/alipay/notify'; returnUrl='https://dev.user.bgssai-blog.com/'
    domainPath='/api/admin/config/system-domain/save'
    merchantPath='/api/admin/alipay-pay/merchants'
    oauthPath='/api/admin/oauth/config/save'
    oauthBody=@{ provider='ALIPAY'; client_id='2021006159661313'; client_secret=$blogPrivBody; scope='auth_user'; enabled=$true }
  }
  @{
    key='builder'; appId='2021006185674173'; admin='https://dev.admin.bgssai-builder.com'; user='https://dev.user.bgssai-builder.com'
    priv=(Get-Body (Join-Path $baseKeys 'builder\app_private_key_body.txt')); name='BGSSAIBuilder'
    loginPath='/api/auth/login'; loginBody=@{ account='admin@bgssai.com'; password='password123' }; tokenFrom='data.access_token'
    notify='/api/payment/notify/alipay'; returnUrl='https://dev.user.bgssai-builder.com/'
    domainPath=$null
    merchantPath='/api/admin/payment/alipay-merchants/save'
    oauthPath=$null; oauthBody=$null
  }
  @{
    key='geo-cn'; appId='2021006185686182'; admin='https://dev.admin.bgssai-geo.cn'; user='https://dev.user.bgssai-geo.cn'
    priv=(Get-Body (Join-Path $baseKeys 'geo-cn\app_private_key_body.txt')); name='BGSSAIGeoCN'
    loginPath='/api/auth/login'; loginBody=@{ user_custom_id='admin'; password='Admin@123456' }; tokenFrom='data.access_token'
    notify='/payment/alipay/notify'; returnUrl='https://dev.user.bgssai-geo.cn/'
    domainPath='/api/admin/config/system-domain/save'
    merchantPath='/api/admin/config/alipay/save'
    oauthPath='/api/admin/config/oauth/save'
    oauthBody=@{ channel='alipay'; app_id='2021006185686182'; app_secret=(Get-Body (Join-Path $baseKeys 'geo-cn\app_private_key_body.txt')); frontend_url='https://dev.user.bgssai-geo.cn'; scope='auth_user'; is_enabled=$true }
  }
  @{
    key='geo'; appId='2021006185698192'; admin='https://dev.admin.bgssai-geo.com'; user='https://dev.user.bgssai-geo.com'
    priv=(Get-Body (Join-Path $baseKeys 'geo\app_private_key_body.txt')); name='BGSSAIGeo'
    loginPath='/api/auth/password/login'; loginBody=@{ account='admin@bgssai.com'; password='Admin@123456' }; tokenFrom='data.access_token'
    notify='/payment/alipay/notify'; returnUrl='https://dev.user.bgssai-geo.com/'
    domainPath='/api/admin/config/system-domain/save'
    merchantPath='/api/admin/config/alipay/save'
    oauthPath=$null; oauthBody=$null
  }
  @{
    key='saas'; appId='2021006186639146'; admin='https://dev.admin.bgssai-saas.com'; user='https://dev.user.bgssai-saas.com'
    priv=(Get-Body (Join-Path $baseKeys 'saas\app_private_key_body.txt')); name='BGSSAISaaS'
    loginPath='/bgssai/platform-admin/login'; loginBody=@{ login_type='password'; identifier='admin'; password='Bgssai@2026' }; tokenFrom='result'
    notify='/bgssai/payment/notify/ALIPAY/BGSSAISaaS'; returnUrl='https://dev.user.bgssai-saas.com/'
    domainPath='/bgssai/platform-admin/config/system-domain/save'
    merchantPath='/bgssai/platform-admin/pay-merchant/save'
    oauthPath=$null; oauthBody=$null
  }
  @{
    key='magic'; appId='2021006186636165'; admin='https://dev.admin.bgssai-magic.com'; user='https://dev.user.bgssai-magic.com'
    priv=(Get-Body (Join-Path $baseKeys 'magic\app_private_key_body.txt')); name='BGSSAIMagic'
    loginPath='/bgssai/admin/login'; loginBody=@{ identifier='admin'; password='admin123' }; tokenFrom='result'
    notify='/bgssai/user/payment/alipay/notify'; returnUrl='https://dev.user.bgssai-magic.com/'
    domainPath=$null
    merchantPath='/bgssai/admin/pay/alipay-configs/save'
    oauthPath=$null; oauthBody=$null
  }
  @{
    key='marklens'; appId='2021006185681180'; admin='https://dev.admin.bgssai-marklens.com'; user='https://dev.user.bgssai-marklens.com'
    priv=(Get-Body (Join-Path $baseKeys 'marklens\app_private_key_body.txt')); name='BGSSAIMarklens'
    loginPath='/bgssai/admin/login'; loginBody=@{ identifier='admin'; password='admin123' }; tokenFrom='result'
    notify='/bgssai/pay/notify/alipay'; returnUrl='https://dev.user.bgssai-marklens.com/'
    domainPath=$null
    merchantPath='/bgssai/admin/pay/alipay-configs/save'
    oauthPath=$null; oauthBody=$null
  }
  @{
    key='publish'; appId='2021006186687014'; admin='https://dev.admin.bgssai-publish.com'; user='https://dev.user.bgssai-publish.com'
    priv=(Get-Body (Join-Path $baseKeys 'publish\app_private_key_body.txt')); name='BGSSAIPublish'
    loginPath='/api/admin/auth/login'; loginBody=@{ username='admin'; password='admin123456' }; tokenFrom='data.access_token'
    notify='/api/pay/alipay/notify'; returnUrl='https://dev.user.bgssai-publish.com/'
    domainPath='/api/admin/config/system-domain/save'
    merchantPath='/api/admin/payment/alipay-configs/save'
    oauthPath=$null; oauthBody=$null
  }
  @{
    key='voiceunion'; appId='2021006185633244'; admin='https://dev.admin.bgssai-voiceunion.com'; user='https://dev.user.bgssai-voiceunion.com'
    priv=(Get-Body (Join-Path $baseKeys 'voiceunion\app_private_key_body.txt')); name='BGSSAIVoiceUnion'
    loginPath='/bgssai/admin/login'; loginBody=@{ identifier='admin'; password='admin123' }; tokenFrom='result'
    notify='/bgssai/user/pay/notify/alipay'; returnUrl='https://dev.user.bgssai-voiceunion.com/'
    domainPath=$null
    merchantPath='/bgssai/admin/pay/alipay-configs/save'
    oauthPath=$null; oauthBody=$null
  }
)

$results = @()
foreach ($a in $apps) {
  $item = [ordered]@{ product=$a.key; appId=$a.appId; login=''; domain=''; merchant=''; oauth=''; note='' }
  try {
    $token = Login-Token $a.key $a.admin $a.loginPath $a.loginBody $a.tokenFrom
    if (-not $token -or ($token -is [psobject] -and -not $token.ToString())) {
      # try nested common shapes
      $probe = Invoke-RestMethod -Method Post -Uri ($a.admin + $a.loginPath) -ContentType 'application/json; charset=utf-8' -Body ($a.loginBody | ConvertTo-Json -Compress)
      $token = $probe.data.access_token
      if (-not $token) { $token = $probe.data.token }
      if (-not $token) { $token = $probe.result.access_token }
      if (-not $token) { $token = $probe.result.token }
      if (-not $token -and $probe.result -is [string]) { $token = $probe.result }
      if (-not $token) { throw ("login token missing: " + ($probe | ConvertTo-Json -Depth 4 -Compress)) }
    }
    $item.login = 'ok'

    if ($a.domainPath) {
      try {
        $dr = Post-Json $a.admin $a.domainPath $token @{ domain = $a.user }
        $item.domain = 'ok'
      } catch {
        $item.domain = 'fail:' + $_.Exception.Message
      }
    } else {
      $item.domain = 'n/a'
    }

    $notifyUrl = $a.user + $a.notify
    $merchantBody = $null
    switch ($a.key) {
      'blog' {
        $merchantBody = @{
          merchant_name=$a.name; app_id=$a.appId; key_mode='PUBLIC_KEY'
          app_private_key=$a.priv; alipay_public_key=$alipayPub
          sign_type='RSA2'; charset='utf-8'; gateway_url='https://openapi.alipay.com/gateway.do'
          notify_url=$notifyUrl; return_url=$a.returnUrl; env_mode='PRODUCTION'; is_enabled=$true
        }
      }
      'builder' {
        $merchantBody = @{
          merchant_name=$a.name; app_id=$a.appId; key_mode='PUBLIC_KEY'
          app_private_key=$a.priv; alipay_public_key=$alipayPub; sign_type='RSA2'
          notify_url=$notifyUrl; return_url=$a.returnUrl; env_mode='PRODUCTION'; is_enabled=$true
        }
      }
      'geo-cn' {
        $merchantBody = @{
          merchant_name=$a.name; app_id=$a.appId; key_mode='PUBLIC_KEY'
          app_private_key=$a.priv; alipay_public_key=$alipayPub
          notify_url=$notifyUrl; return_url=$a.returnUrl; env_mode='PRODUCTION'; is_enabled=$true
        }
      }
      'geo' {
        $merchantBody = @{
          app_id=$a.appId; app_private_key=$a.priv; alipay_public_key=$alipayPub
          notify_url=$notifyUrl; return_url=$a.returnUrl; sign_type='RSA2'; charset='utf-8'
          gateway_url='https://openapi.alipay.com/gateway.do'
        }
      }
      'saas' {
        $merchantBody = @{
          channel='ALIPAY'; merchant_no='BGSSAISaaS'; merchant_name=$a.name; enabled=$true; is_default=$true
          config=@{ app_id=$a.appId; private_key=$a.priv; public_key=$alipayPub }
          remark='DEV alipay'
        }
      }
      default {
        $merchantBody = @{
          merchant_name=$a.name; app_id=$a.appId; key_mode='PUBLIC_KEY'
          app_private_key=$a.priv; alipay_public_key=$alipayPub; sign_type='RSA2'; charset='utf-8'
          gateway_url='https://openapi.alipay.com/gateway.do'
          notify_url=$notifyUrl; return_url=$a.returnUrl; env_mode='PRODUCTION'; is_enabled=$true
        }
      }
    }

    try {
      $mr = Post-Json $a.admin $a.merchantPath $token $merchantBody
      $item.merchant = 'ok'
      # try enable/collection-active endpoints if exist (blog-style)
      if ($a.key -eq 'blog') {
        try {
          Post-Json $a.admin '/api/admin/alipay-pay/merchants/update' $token (@{ id=1; is_enabled=$true; is_collection_active=$true } + $merchantBody) | Out-Null
        } catch {}
      }
    } catch {
      $item.merchant = 'fail:' + ($_.ErrorDetails.Message)
      if (-not $item.merchant -or $item.merchant -eq 'fail:') { $item.merchant = 'fail:' + $_.Exception.Message }
    }

    if ($a.oauthPath -and $a.oauthBody) {
      try {
        Post-Json $a.admin $a.oauthPath $token $a.oauthBody | Out-Null
        $item.oauth = 'ok'
      } catch {
        $item.oauth = 'fail:' + $_.Exception.Message
      }
    } else {
      $item.oauth = 'n/a'
    }

    $item.note = 'alipay_pub=platform_key_uploaded'
  } catch {
    $item.login = 'fail'
    $item.note = $_.Exception.Message
  }
  $results += [pscustomobject]$item
  Write-Output ("[{0}] login={1} domain={2} merchant={3} oauth={4} note={5}" -f $item.product, $item.login, $item.domain, $item.merchant, $item.oauth, $item.note)
}

$results | ConvertTo-Json -Depth 4 | Set-Content -Encoding UTF8 (Join-Path $baseKeys 'push_admin_results.json')
Write-Output 'DONE'

