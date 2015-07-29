@rem Set environment for msbuild

@rem Look for Visual Studio 2015
if not defined VS140COMNTOOLS goto vc-set-2013
if not exist "%VS140COMNTOOLS%\..\..\vc\vcvarsall.bat" goto vc-set-2013
if "%VCVARS_VER%" NEQ "140" (
  call "%VS140COMNTOOLS%\..\..\vc\vcvarsall.bat"
  SET VCVARS_VER=140
)
if not defined VCINSTALLDIR goto vc-set-2013
set GYP_MSVS_VERSION=2015
goto msbuild-found

:vc-set-2013
@rem Look for Visual Studio 2013
if not defined VS120COMNTOOLS goto msbuild-not-found
if not exist "%VS120COMNTOOLS%\..\..\vc\vcvarsall.bat" goto msbuild-not-found
if "%VCVARS_VER%" NEQ "120" (
  call "%VS120COMNTOOLS%\..\..\vc\vcvarsall.bat"
  SET VCVARS_VER=120
)
if not defined VCINSTALLDIR goto msbuild-not-found
set GYP_MSVS_VERSION=2013
goto msbuild-found

:msbuild-not-found
echo Failed to find Visual Studio installation.
goto exit

:msbuild-found
@rem Generate the VS project.
python configure --dest-cpu=x86
msbuild j2v8.sln /m /t:Build /p:Configuration=Release /clp:NoSummary;NoItemAndPropertyList;Verbosity=minimal /nologo

'python configure --dest-cpu=x64
'msbuild j2v8.sln /m /t:Build /p:Configuration=Release /clp:NoSummary;NoItemAndPropertyList;Verbosity=minimal /nologo
