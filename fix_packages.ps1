$baseDir = "c:/Users/Jeroen/Documents/GitHub/microbot/runelite-client/src/main/java/net/runelite/client/plugins/microbot/util/ethans/EthanApiPlugin"
$files = Get-ChildItem -Path $baseDir -Recurse -Filter "*.java"

foreach ($file in $files) {
    $content = Get-Content $file.FullName -Raw
    
    # Replace package names
    $content = $content -replace "package com\.example\.EthanApiPlugin", "package net.runelite.client.plugins.microbot.util.ethans.EthanApiPlugin"
    $content = $content -replace "import com\.example\.EthanApiPlugin", "import net.runelite.client.plugins.microbot.util.ethans.EthanApiPlugin"
    
    # Write back to file
    Set-Content -Path $file.FullName -Value $content
}
