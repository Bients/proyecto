; Instalador de Taller Pro (Inno Setup 6). Se compila con crear-instalador.bat
; Requiere haber generado antes target\app\TallerPro (jpackage app-image).

#define NombreApp "Taller Pro"
#define Editor "Taller Pro"
#define Exe "TallerPro.exe"
#define ImagenApp "target\app\TallerPro"
#define VersionPE GetVersionNumbersString(AddBackslash(SourcePath) + ImagenApp + "\" + Exe)
#if VersionPE == ""
  #error Genera la imagen con build-app-image.bat antes de compilar el instalador.
#endif
#define VersionApp Copy(VersionPE, 1, RPos(".", VersionPE) - 1)

[Setup]
AppId={{7B3E5C1A-4D2F-4E8A-9C61-2F5A8D0B7E34}
AppName={#NombreApp}
AppVersion={#VersionApp}
VersionInfoVersion={#VersionApp}
AppPublisher={#Editor}
DefaultDirName={autopf}\{#NombreApp}
DefaultGroupName={#NombreApp}
DisableProgramGroupPage=yes
; Si existe un archivo LICENSE se muestra la pantalla de aceptación de licencia
#ifexist "LICENSE"
LicenseFile=LICENSE
#endif
OutputDir=target\instalador
OutputBaseFilename=Setup
SetupIconFile=icono\TallerPro.ico
UninstallDisplayIcon={app}\{#Exe}
Compression=lzma2/max
SolidCompression=yes
WizardStyle=classic
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible
PrivilegesRequired=admin
CloseApplications=yes

[Languages]
Name: "spanish"; MessagesFile: "compiler:Languages\Spanish.isl"

[Tasks]
Name: "desktopicon"; Description: "Crear un acceso directo en el &escritorio"; GroupDescription: "Accesos directos:"
Name: "startmenuicon"; Description: "Crear un acceso directo en el menú &Inicio"; GroupDescription: "Accesos directos:"

[InstallDelete]
Type: files; Name: "{app}\app\taller-pro-*.jar"

[Files]
; Aplicación autocontenida (incluye su propio Java, no requiere instalar nada más)
Source: "{#ImagenApp}\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{autoprograms}\{#NombreApp}"; Filename: "{app}\{#Exe}"; Tasks: startmenuicon
Name: "{autoprograms}\Desinstalar {#NombreApp}"; Filename: "{uninstallexe}"; Tasks: startmenuicon
Name: "{autodesktop}\{#NombreApp}"; Filename: "{app}\{#Exe}"; Tasks: desktopicon

[Run]
Filename: "{app}\{#Exe}"; Description: "Abrir {#NombreApp} al terminar"; Flags: nowait postinstall skipifsilent

; La base de datos queda en la carpeta del usuario (.taller-pro) y no se borra al desinstalar.
