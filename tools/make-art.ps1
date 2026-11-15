# 生成模组图标与封面图（512x512 图标 / 1280x720 封面）
# 用法: powershell -ExecutionPolicy Bypass -File tools/make-art.ps1
# 需要 Windows + 系统自带字体（Microsoft YaHei）

param([string]$Root = $PSScriptRoot)

Add-Type -AssemblyName System.Drawing

if ([string]::IsNullOrWhiteSpace($Root)) {
	$Root = Join-Path (Get-Location) '.'
}
$assets = Join-Path $Root 'src\main\resources\assets\fakeultimine'
$dist = Join-Path $Root '..\dist'
New-Item -ItemType Directory -Force -Path $assets | Out-Null
New-Item -ItemType Directory -Force -Path $dist | Out-Null

function New-RoundedRect([single]$x, [single]$y, [single]$w, [single]$h, [single]$r) {
	$path = New-Object System.Drawing.Drawing2D.GraphicsPath
	$d = $r * 2
	$path.AddArc($x, $y, $d, $d, 180, 90)
	$path.AddArc($x + $w - $d, $y, $d, $d, 270, 90)
	$path.AddArc($x + $w - $d, $y + $h - $d, $d, $d, 0, 90)
	$path.AddArc($x, $y + $h - $d, $d, $d, 90, 90)
	$path.CloseFigure()
	return $path
}

function New-Pen([int]$a, [int]$r, [int]$g, [int]$b, [single]$w) {
	return New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb($a, $r, $g, $b)), $w
}

function New-Solid([int]$a, [int]$r, [int]$g, [int]$b) {
	return New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb($a, $r, $g, $b))
}

function New-Background($graphics, [int]$w, [int]$h) {
	$rect = New-Object System.Drawing.Rectangle 0, 0, $w, $h
	$brush = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
		$rect,
		[System.Drawing.Color]::FromArgb(255, 30, 37, 48),
		[System.Drawing.Color]::FromArgb(255, 9, 12, 16),
		[System.Drawing.Drawing2D.LinearGradientMode]::Vertical)
	$graphics.FillRectangle($brush, $rect)
	$brush.Dispose()

	$pen = New-Pen 18 120 160 200 ([single]1)
	$step = [int]($w / 16)
	for ($i = $step; $i -lt $w; $i += $step) { $graphics.DrawLine($pen, $i, 0, $i, $h) }
	for ($i = $step; $i -lt $h; $i += $step) { $graphics.DrawLine($pen, 0, $i, $w, $i) }
	$pen.Dispose()
}

function Draw-Grid($graphics, [single]$cx, [single]$cy, [single]$cell, [single]$gap) {
	$total = 3 * $cell + 2 * $gap
	$startX = $cx - $total / 2
	$startY = $cy - $total / 2
	$radius = $cell * 0.16

	for ($row = 0; $row -lt 3; $row++) {
		for ($col = 0; $col -lt 3; $col++) {
			$x = $startX + $col * ($cell + $gap)
			$y = $startY + $row * ($cell + $gap)
			$isCenter = ($row -eq 1 -and $col -eq 1)

			if ($isCenter) {
				for ($i = 6; $i -ge 1; $i--) {
					$glow = New-Solid ([int](9 * $i)) 63 208 255
					$gp = New-RoundedRect ($x - $i * 3) ($y - $i * 3) ($cell + $i * 6) ($cell + $i * 6) ($radius + $i * 2)
					$graphics.FillPath($glow, $gp)
					$glow.Dispose(); $gp.Dispose()
				}
				$fill = New-Solid 255 63 208 255
				$path = New-RoundedRect $x $y $cell $cell $radius
				$graphics.FillPath($fill, $path)
				$fill.Dispose()

				$innerPen = New-Pen 230 210 245 255 ([single]2)
				$p2 = New-RoundedRect ($x + $cell * 0.1) ($y + $cell * 0.1) ($cell * 0.8) ($cell * 0.8) ($radius * 0.7)
				$graphics.DrawPath($innerPen, $p2)
				$innerPen.Dispose(); $p2.Dispose(); $path.Dispose()
			}
			else {
				$path = New-RoundedRect $x $y $cell $cell $radius
				$fill = New-Solid 255 46 58 70
				$graphics.FillPath($fill, $path)
				$fill.Dispose()
				$pen = New-Pen 255 74 92 107 ([single]2)
				$graphics.DrawPath($pen, $path)
				$pen.Dispose(); $path.Dispose()
			}
		}
	}
}

# ---------- 图标 512x512 ----------
$bmp = New-Object System.Drawing.Bitmap 512, 512
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$g.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit
New-Background $g 512 512
Draw-Grid $g 256 208 112 14

$font = New-Object System.Drawing.Font 'Microsoft YaHei', 30, ([System.Drawing.FontStyle]::Bold)
$textBrush = New-Solid 235 236 245 250
$fmt = New-Object System.Drawing.StringFormat
$fmt.Alignment = [System.Drawing.StringAlignment]::Center
$g.DrawString('假人连锁', $font, $textBrush, 256, 420, $fmt)
$g.Dispose()
$iconPath = Join-Path $assets 'icon.png'
$bmp.Save($iconPath, [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()
Copy-Item $iconPath (Join-Path $dist 'icon-512.png') -Force
Write-Output "icon  -> $iconPath"

# ---------- 封面 1280x720 ----------
$bmp = New-Object System.Drawing.Bitmap 1280, 720
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$g.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit
New-Background $g 1280 720
Draw-Grid $g 300 360 150 18

$titleFont = New-Object System.Drawing.Font 'Microsoft YaHei', 66, ([System.Drawing.FontStyle]::Bold)
$subFont = New-Object System.Drawing.Font 'Microsoft YaHei', 33, ([System.Drawing.FontStyle]::Bold)
$descFont = New-Object System.Drawing.Font 'Microsoft YaHei', 22, ([System.Drawing.FontStyle]::Regular)
$smallFont = New-Object System.Drawing.Font 'Consolas', 19, ([System.Drawing.FontStyle]::Regular)

$white = New-Solid 255 240 246 252
$cyan = New-Solid 255 63 208 255
$grey = New-Solid 255 150 168 184

$g.DrawString('假人连锁破坏', $titleFont, $white, 560, 140)
$g.DrawString('FakePlayer Ultimine', $subFont, $cyan, 566, 250)
$g.DrawString('地毯 / GCA 假人 × FTB Ultimine 式连锁破坏', $descFont, $white, 566, 320)
$g.DrawString('全指令控制 · 7 种形状 · 每刻预算防卡服', $descFont, $grey, 566, 368)
$g.DrawString('v1.0.0  ·  Minecraft 1.21.1 Fabric', $smallFont, $grey, 566, 436)

$bar = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
	(New-Object System.Drawing.Rectangle 0, 690, 1280, 30),
	[System.Drawing.Color]::FromArgb(255, 63, 208, 255),
	[System.Drawing.Color]::FromArgb(0, 63, 208, 255),
	[System.Drawing.Drawing2D.LinearGradientMode]::Horizontal)
$g.FillRectangle($bar, 0, 690, 1280, 30)
$bar.Dispose()

$g.Dispose()
$coverPath = Join-Path $dist 'cover-1280x720.png'
$bmp.Save($coverPath, [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()
Write-Output "cover -> $coverPath"
