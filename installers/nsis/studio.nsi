# 2018-03-06 13:59:07

!ifndef APPNAME
 !define APPNAME "Convertigo Mobility Platform Studio"
!endif

Name "${APPNAME}"

SetCompressor /FINAL zlib

# Defines
!ifndef ConvertigoVersion
 !error "Missing ConvertigoVersion (x.y.z)"
!endif

!ifndef CONVERTIGO_EXE_NAME
 !define CONVERTIGO_EXE_NAME ConvertigoStudio.exe
!endif

!ifndef Arch
 !error "Missing Arch (x86, x86_64)"
!endif

!define COMPANY "Convertigo Mobility Platform"
!define URL http://www.convertigo.com
!define /date currYear %Y

# MUI defines
!define MUI_WELCOMEFINISHPAGE_BITMAP "..\data\wizard.bmp"
!define MUI_HEADERIMAGE
!define MUI_HEADERIMAGE_BITMAP "..\data\header.bmp"
!define MUI_FINISHPAGE_RUN $INSTDIR\${CONVERTIGO_EXE_NAME}
!define MUI_ICON "..\data\convertigo.ico"

# Included files
!include Sections.nsh
!include MUI.nsh
!include FileFunc.nsh
!insertmacro GetParent
!include StrFunc.nsh
#${StrLoc}
!include include\func.nsh

# Reserved Files
ReserveFile "${NSISDIR}\Plugins\amd64-unicode\AdvSplash.dll"
;ReserveFile "${NSISDIR}\Plugins\x86-ansi\AdvSplash.dll"

# Variables
RequestExecutionLevel user

# Installer pages
!insertmacro MUI_PAGE_WELCOME
!insertmacro MUI_PAGE_LICENSE ..\..\license.txt
!insertmacro MUI_PAGE_DIRECTORY
!insertmacro MUI_PAGE_INSTFILES
!insertmacro MUI_PAGE_FINISH

# Installer languages
!insertmacro MUI_LANGUAGE English

# Installer attributes
BrandingText "${COMPANY} 2000-${currYear}"
!ifndef OutFile
 !define OutFile ${CONVERTIGO_EXE_NAME}
!endif

OutFile "${OutFile}"
InstallDir "$PROFILE\${COMPANY}\$(^Name) ${ConvertigoVersion}\"
CRCCheck off
XPStyle on
ShowInstDetails nevershow
VIProductVersion "${ConvertigoVersion}.0"
VIAddVersionKey /LANG=${LANG_ENGLISH} ProductName "${APPNAME}"
VIAddVersionKey /LANG=${LANG_ENGLISH} ProductVersion "${ConvertigoVersion}"
VIAddVersionKey /LANG=${LANG_ENGLISH} CompanyName "${COMPANY}"
VIAddVersionKey /LANG=${LANG_ENGLISH} CompanyWebsite "${URL}"
VIAddVersionKey /LANG=${LANG_ENGLISH} FileVersion "${ConvertigoVersion}"
VIAddVersionKey /LANG=${LANG_ENGLISH} FileDescription "${APPNAME}"
VIAddVersionKey /LANG=${LANG_ENGLISH} LegalCopyright "${COMPANY} (c) 2000-${currYear}"


# Installer sections
Section ""
    
    SetOutPath $INSTDIR
    SetOverwrite ifnewer
    File /r ..\..\eclipse-repository\target\products\com.convertigo.studio\win32\win32\${Arch}\*
    File ..\data\convertigo.ico
    
    # Desktop shortcut
    CreateShortCut "$DESKTOP\$(^Name).lnk" "$INSTDIR\${CONVERTIGO_EXE_NAME}" "" "$INSTDIR\convertigo.ico" 0 SW_SHOWMAXIMIZED "" "${APPNAME}"
    
    # Configure License acceptation in ConvertigoStudio.ini.
    Push "$INSTDIR\ConvertigoStudio.ini" ; file to modify
    Push "-Dconvertigo.license.accepted=" ; string that a line must begin with
    Push "-Dconvertigo.license.accepted=true" ; string to replace whole line with
    Call ReplaceLineStr
SectionEnd

# Installer functions
Function .onInit
    InitPluginsDir
    Push $R1
    File "/oname=$PLUGINSDIR\spltmp.bmp" "..\..\eclipse-plugin-product\splash.bmp"
    advsplash::show 1000 1000 1000 -1 $PLUGINSDIR\spltmp
    Pop $R1
    Pop $R1
FunctionEnd

# Installer Language Strings