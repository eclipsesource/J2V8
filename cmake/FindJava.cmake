
SET(_JAVA_HINTS $ENV{JAVA_HOME}/bin)

SET(_JAVA_PATHS
  /usr/lib/java/bin
  /usr/share/java/bin
  /usr/local/java/bin
  /usr/local/java/share/bin
  /usr/java/j2sdk1.4.2_04
  /usr/lib/j2sdk1.4-sun/bin
  /usr/java/j2sdk1.4.2_09/bin
  /usr/lib/j2sdk1.5-sun/bin
  /opt/sun-jdk-1.5.0.04/bin
  )

FIND_PROGRAM(JAVA_EXECUTABLE
  NAMES java
  HINTS ${_JAVA_HINTS}
  PATHS ${_JAVA_PATHS}
)

IF(JAVA_EXECUTABLE)
  EXECUTE_PROCESS(COMMAND ${JAVA_EXECUTABLE} -version
    RESULT_VARIABLE res
    OUTPUT_VARIABLE var
    ERROR_VARIABLE var
    OUTPUT_STRIP_TRAILING_WHITESPACE
    ERROR_STRIP_TRAILING_WHITESPACE)

  IF( res )
    IF(${Java_FIND_REQUIRED})
      MESSAGE( FATAL_ERROR "Error executing java -version" )
    ELSE()
      MESSAGE( STATUS "Warning, could not run java --version")
    ENDIF()

  ELSE( res )
    IF(var MATCHES "java version \"[0-9]+\\.[0-9]+\\.[0-9_.]+[oem-]*\".*")
      STRING( REGEX REPLACE ".* version \"([0-9]+\\.[0-9]+\\.[0-9_.]+)[oem-]*\".*"
        "\\1" Java_VERSION_STRING "${var}" )
    ELSEIF(var MATCHES "java full version \"kaffe-[0-9]+\\.[0-9]+\\.[0-9_]+\".*")
      STRING( REGEX REPLACE "java full version \"kaffe-([0-9]+\\.[0-9]+\\.[0-9_]+).*"
        "\\1" Java_VERSION_STRING "${var}" )
    ELSE()
      IF(NOT Java_FIND_QUIETLY)
        message(WARNING "regex not supported: ${var}. Please report")
      ENDIF(NOT Java_FIND_QUIETLY)
    ENDIF()
    STRING( REGEX REPLACE "([0-9]+).*" "\\1" Java_VERSION_MAJOR "${Java_VERSION_STRING}" )
    STRING( REGEX REPLACE "[0-9]+\\.([0-9]+).*" "\\1" Java_VERSION_MINOR "${Java_VERSION_STRING}" )
    STRING( REGEX REPLACE "[0-9]+\\.[0-9]+\\.([0-9]+).*" "\\1" Java_VERSION_PATCH "${Java_VERSION_STRING}" )
    STRING( REGEX REPLACE "[0-9]+\\.[0-9]+\\.[0-9]+\\_?\\.?([0-9]*)$" "\\1" Java_VERSION_TWEAK "${Java_VERSION_STRING}" )
    if( Java_VERSION_TWEAK STREQUAL "" )
      set(Java_VERSION ${Java_VERSION_MAJOR}.${Java_VERSION_MINOR}.${Java_VERSION_PATCH})
    else( )
      set(Java_VERSION ${Java_VERSION_MAJOR}.${Java_VERSION_MINOR}.${Java_VERSION_PATCH}.${Java_VERSION_TWEAK})
    endif( )

    IF(NOT Java_FIND_QUIETLY)
      MESSAGE( STATUS "Java version ${Java_VERSION} found!" )
    ENDIF(NOT Java_FIND_QUIETLY)

  ENDIF( res )
ENDIF(JAVA_EXECUTABLE)

UNSET(JAVA_EXECUTABLE CACHE)

if( Java_VERSION_MINOR LESS 6 )
  message("-- WARNING: Your system is running Java ${Java_VERSION_MAJOR}.${Java_VERSION_MINOR}. Java JDK 1.6+ is required for compiling ${PROGNAME}.")
  set(Java_OLD_VERSION TRUE)
else()
  set(Java_OLD_VERSION FALSE)
endif()

if(!$ENV{JAVA_HOME})
  message("Cannot find JAVA_HOME. Please setup the path to the base of the Java JDK to JAVA_HOME before compiling.")
endif()

if(APPLE)
  EXECUTE_PROCESS(COMMAND "/usr/libexec/java_home"
    RESULT_VARIABLE res
    OUTPUT_VARIABLE var
    ERROR_VARIABLE var
    OUTPUT_STRIP_TRAILING_WHITESPACE
    ERROR_STRIP_TRAILING_WHITESPACE)

    if(res)
      MESSAGE(FATAL_ERROR "Error executing java_home")
    else()
      set(Java_ROOT ${var})
    endif()
else()
  set(Java_ROOT "$ENV{JAVA_HOME}")
endif()

if ("${Java_ROOT}" STREQUAL "")
  message(FATAL_ERROR "Unable to locate Java JDK")
endif()

message ("Using Java-Root: ${Java_ROOT}")
