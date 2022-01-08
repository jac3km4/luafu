cd ..
$baseCp = (Get-ChildItem -Path "lib" | %{ $_.FullName }) -join ";"
$addedCp = "{0}\luafu\luafu.jar;{1}" -f (pwd),$baseCp

jre\win32\x64\bin\java.exe "-Djava.library.path=natives\win32\x64" -cp $addedCp Luafu
