Function ReplaceLineStr
 Exch $R0 ; string to replace that whole line with
 Exch
 Exch $R1 ; string that line should start with
 Exch
 Exch 2
 Exch $R2 ; file
 Push $R3 ; file handle
 Push $R4 ; temp file
 Push $R5 ; temp file handle
 Push $R6 ; global
 Push $R7 ; input string length
 Push $R8 ; line string length
 Push $R9 ; global
 
  StrLen $R7 $R1
 
  GetTempFileName $R4
 
  FileOpen $R5 $R4 w
  IfFileExists $R2 OpenFileR 0
  ;MessageBox MB_OK "FILE DOES NOT EXIST"
    ClearErrors
    FileOpen $R3 $R2 w
    IfErrors 0 CloseFileW
        ;MessageBox MB_OK "DIR DOES NOT EXIST"
        ${GetParent} "$R2" $R6 
        CreateDirectory $R6
        FileOpen $R3 $R2 w
    ;MessageBox MB_OK "DIR EXISTS"
    CloseFileW:
        FileClose $R3
  OpenFileR:
  ;MessageBox MB_OK "FILE EXISTS"
  FileOpen $R3 $R2 r
  StrCpy $0 "notfound"
 
  ReadLoop:
  ClearErrors
   FileRead $R3 $R6
    IfErrors Done
 
   StrLen $R8 $R6
   StrCpy $R9 $R6 $R7 -$R8
   StrCmp $R9 $R1 0 +4
 
    FileWrite $R5 "$R0$\r$\n"
    StrCpy $0 "found" ; string to replace has been found
    Goto ReadLoop
 
    FileWrite $R5 $R6
    Goto ReadLoop
 
  Done:
 
  FileClose $R3
  StrCmp $0 "notfound" 0 +4 ; if string has not been found, add the string to file
  FileSeek $R5 0 END
  FileWrite $R5 "$\r$\n" ; we write a new line
  FileWrite $R5 "$R0$\r$\n"
    
  FileClose $R5
 
  SetDetailsPrint none
   Delete $R2
   Rename $R4 $R2
   Delete $R4
  SetDetailsPrint both
 
 Pop $R9
 Pop $R8
 Pop $R7
 Pop $R6
 Pop $R5
 Pop $R4
 Pop $R3
 Pop $R2
 Pop $R1
 Pop $R0
FunctionEnd

; Push $filenamestring (e.g. 'c:\this\and\that\filename.htm')
; Push "\"
; Call StrSlash
; Pop $R0
; ;Now $R0 contains 'c:/this/and/that/filename.htm'
Function StrSlash
  Exch $R3 ; $R3 = needle ("\" or "/")
  Exch
  Exch $R1 ; $R1 = String to replacement in (haystack)
  Push $R2 ; Replaced haystack
  Push $R4 ; $R4 = not $R3 ("/" or "\")
  Push $R6
  Push $R7 ; Scratch reg
  StrCpy $R2 ""
  StrLen $R6 $R1
  StrCpy $R4 "\"
  StrCmp $R3 "/" loop
  StrCpy $R4 "/"  
loop:
  StrCpy $R7 $R1 1
  StrCpy $R1 $R1 $R6 1
  StrCmp $R7 $R3 found
  StrCpy $R2 "$R2$R7"
  StrCmp $R1 "" done loop
found:
  StrCpy $R2 "$R2$R4"
  StrCmp $R1 "" done loop
done:
  StrCpy $R3 $R2
  Pop $R7
  Pop $R6
  Pop $R4
  Pop $R2
  Pop $R1
  Exch $R3
FunctionEnd

!macro CreateInternetShortcut FILENAME URL ICONFILE ICONINDEX
    WriteINIStr "${FILENAME}.url" "InternetShortcut" "URL" "${URL}"
    WriteINIStr "${FILENAME}.url" "InternetShortcut" "IconFile" "${ICONFILE}"
    WriteINIStr "${FILENAME}.url" "InternetShortcut" "IconIndex" "${ICONINDEX}"
!macroend

!macro MoveDirectory SRCPATH FOLDER DESTPATH
    IfFileExists "${SRCPATH}\${FOLDER}" 0 +4
        RMDir /r "${DESTPATH}\${FOLDER}"
        CreateDirectory "${DESTPATH}\${FOLDER}"            
        Rename "${SRCPATH}\${FOLDER}" "${DESTPATH}\${FOLDER}"
    #DO NOT REMOVE
!macroend

!macro CopyLine2File SRCFILE LINE DESTFILE
    IfFileExists "${SRCFILE}" 0 +16
        ClearErrors
        FileOpen $0 "${SRCFILE}" r
        IfErrors +13        
        FileRead $0 $1
        IfErrors +11
        StrLen $3 ${LINE}
        StrCpy $2 $1 $3
        StrCmp $2 ${LINE} +2 0
        Goto -5
        #EDIT engine.properties
        Push "${DESTFILE}" ; file to modify
        Push "${LINE}" ; string that a line must begin with
        Push "$1" ; string to replace whole line with
        Call ReplaceLineStr
        FileClose $0
    #DO NOT REMOVE
!macroend