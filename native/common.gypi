{
  'variables': {
    'asan%': 0,
    'werror': '',                     # Turn off -Werror in V8 build.
    'visibility%': 'hidden',          # V8's visibility setting
    'target_arch%': 'ia32',           # set v8's target architecture
    'host_arch%': 'ia32',             # set v8's host architecture
    'want_separate_host_toolset%': 0, # V8 should not build target and host
    'library%': 'shared_library',     # allow override to 'shared_library' for DLL/.so builds
    'component%': 'static_library',   # NB. these names match with what V8 expects
    'msvs_multi_core_compile': '0',   # we do enable multicore compiles, but not using the V8 way
    'python%': 'python',

    # Default to -O0 for debug builds.
    'v8_optimized_debug%': 0,

    # Enable disassembler for `--print-code` v8 options
    'v8_enable_disassembler': 1,

    # Don't bake anything extra into the snapshot.
    'v8_use_external_startup_data%': 0,

    'conditions': [
      ['OS == "win"', {
        'os_posix': 0,
        'v8_postmortem_support%': 'false',
      }, {
        'os_posix': 1,
        'v8_postmortem_support%': 'true',
      }],
      ['GENERATOR == "ninja" or OS== "mac"', {
        'OBJ_DIR': '<(PRODUCT_DIR)/obj',
        'V8_BASE': '<(PRODUCT_DIR)/libv8_base.a',
      }, {
        #'OBJ_DIR': '<(PRODUCT_DIR)/obj.target',
        #'V8_BASE': '<(PRODUCT_DIR)/obj.target/deps/v8/tools/gyp/libv8_base.a',
      }],
      ['OS=="mac"', {
        'clang%': 1,
      }, {
        'clang%': 0,
      }],
    ],
  },

  'target_defaults': {
    'default_configuration': 'Release',
    'configurations': {
      'Debug': {
        'variables': {
          'v8_enable_handle_zapping%': 1,
        },
        'defines': [ 'DEBUG', '_DEBUG' ],
        'cflags': [ '-g', '-O0' ],
        'conditions': [
          ['target_arch=="x64"', {
            'msvs_configuration_platform': 'x64',
          }],
        ],
        'msvs_settings': {
          'VCCLCompilerTool': {
            'RuntimeLibrary': 1, # static debug
            'Optimization': 0, # /Od, no optimization
            'MinimalRebuild': 'false',
            'OmitFramePointers': 'false',
            'BasicRuntimeChecks': 3, # /RTC1
          },
          'VCLinkerTool': {
            'LinkIncremental': 2, # enable incremental linking
          },
        },
        'xcode_settings': {
          'GCC_OPTIMIZATION_LEVEL': '0', # stop gyp from defaulting to -Os
        },
      },
      'Release': {
        'variables': {
          'v8_enable_handle_zapping%': 0,
        },
        'cflags': [ '-O3', '-ffunction-sections', '-fdata-sections' ],
        'conditions': [
          ['target_arch=="x64"', {
            'msvs_configuration_platform': 'x64',
          }],
          ['OS=="solaris"', {
            # pull in V8's postmortem metadata
            'ldflags': [ '-Wl,-z,allextract' ]
          }],
          ['OS!="mac" and OS!="win"', {
            'cflags': [ '-fno-omit-frame-pointer' ],
          }],
        ],
        'msvs_settings': {
          'VCCLCompilerTool': {
            'RuntimeLibrary': 0, # static release
            'Optimization': 3, # /Ox, full optimization
            'FavorSizeOrSpeed': 1, # /Ot, favour speed over size
            'InlineFunctionExpansion': 2, # /Ob2, inline anything eligible
            'WholeProgramOptimization': 'true', # /GL, whole program optimization, needed for LTCG
            'OmitFramePointers': 'true',
            'EnableFunctionLevelLinking': 'true',
            'EnableIntrinsicFunctions': 'true',
            'RuntimeTypeInfo': 'false',
            'AdditionalOptions': [
              '/MP', # compile across multiple CPUs
            ],
          },
          'VCLibrarianTool': {
            'AdditionalOptions': [
              '/LTCG', # link time code generation
            ],
          },
          'VCLinkerTool': {
            'LinkTimeCodeGeneration': 1, # link-time code generation
            'OptimizeReferences': 2, # /OPT:REF
            'EnableCOMDATFolding': 2, # /OPT:ICF
            'LinkIncremental': 1, # disable incremental linking
          },
        },
      }
    },
    # Forcibly disable -Werror.  We support a wide range of compilers, it's
    # simply not feasible to squelch all warnings, never mind that the
    # libraries in deps/ are not under our control.
    'cflags!': ['-Werror'],
    'msvs_settings': {
      'VCCLCompilerTool': {
        'StringPooling': 'true', # pool string literals
        'DebugInformationFormat': 3, # Generate a PDB
        'WarningLevel': 3,
        'BufferSecurityCheck': 'true',
        'ExceptionHandling': 0, # /EHsc
        'SuppressStartupBanner': 'true',
        'WarnAsError': 'false',
      },
      'VCLibrarianTool': {
      },
      'VCLinkerTool': {
        'conditions': [
          ['target_arch=="ia32"', {
            'TargetMachine' : 1, # /MACHINE:X86
            'target_conditions': [
              ['_type=="executable"', {
                'AdditionalOptions': [ '/SubSystem:Console,"5.01"' ],
              }],
            ],
          }],
          ['target_arch=="x64"', {
            'TargetMachine' : 17, # /MACHINE:AMD64
            'target_conditions': [
              ['_type=="executable"', {
                'AdditionalOptions': [ '/SubSystem:Console,"5.02"' ],
              }],
            ],
          }],
        ],
        'GenerateDebugInformation': 'true',
        'RandomizedBaseAddress': 2, # enable ASLR
        'DataExecutionPrevention': 2, # enable DEP
        'AllowIsolation': 'true',
        'SuppressStartupBanner': 'true',
      },
    },
    'msvs_disabled_warnings': [4351, 4355, 4800],
    'conditions': [
      ['asan != 0', {
        'cflags+': [
          '-fno-omit-frame-pointer',
          '-fsanitize=address',
          '-w',  # http://crbug.com/162783
        ],
        'cflags_cc+': [ '-gline-tables-only' ],
        'cflags!': [ '-fomit-frame-pointer' ],
        'ldflags': [ '-fsanitize=address' ],
      }],
      ['OS == "win"', {
        'msvs_cygwin_shell': 0, # prevent actions from trying to use cygwin
        'defines': [
          'WIN32',
          # we don't really want VC++ warning us about
          # how dangerous C functions are...
          '_CRT_SECURE_NO_DEPRECATE',
          # ... or that C implementations shouldn't use
          # POSIX names
          '_CRT_NONSTDC_NO_DEPRECATE',
          # Make sure the STL doesn't try to use exceptions
          '_HAS_EXCEPTIONS=0',
          'BUILDING_V8_SHARED=1',
        ],
      }],
      [ 'OS in "linux freebsd openbsd solaris"', {
        'cflags': [ '-pthread', ],
        'ldflags': [ '-pthread' ],
      }],
      [ 'OS in "linux freebsd openbsd solaris android"', {
        'cflags': [ '-Wall', '-Wextra', '-Wno-unused-parameter', ],
        'cflags_cc': [ '-fno-rtti', '-fno-exceptions', '-std=gnu++0x' ],
        'ldflags': [ '-rdynamic' ],
        'target_conditions': [
          ['_type=="static_library"', {
            'standalone_static_library': 1, # disable thin archive which needs binutils >= 2.19
          }],
        ],
        'conditions': [
          [ 'target_arch=="ia32"', {
            'cflags': [ '-m32' ],
            'ldflags': [ '-m32' ],
          }],
          [ 'target_arch=="x32"', {
            'cflags': [ '-mx32' ],
            'ldflags': [ '-mx32' ],
          }],
          [ 'target_arch=="x64"', {
            'cflags': [ '-m64' ],
            'ldflags': [ '-m64' ],
          }],
          [ 'OS=="solaris"', {
            'cflags': [ '-pthreads' ],
            'ldflags': [ '-pthreads' ],
            'cflags!': [ '-pthread' ],
            'ldflags!': [ '-pthread' ],
          }],
        ],
      }],
      [ 'OS=="android"', {
        'defines': ['_GLIBCXX_USE_C99_MATH'],
        'libraries': [ '-llog' ],
      }],
      ['OS=="mac"', {
        'defines': ['_DARWIN_USE_64_BIT_INODE=1'],
        'xcode_settings': {
          'ALWAYS_SEARCH_USER_PATHS': 'NO',
          'GCC_CW_ASM_SYNTAX': 'NO',                # No -fasm-blocks
          'GCC_DYNAMIC_NO_PIC': 'NO',               # No -mdynamic-no-pic
                                                    # (Equivalent to -fPIC)
          'GCC_ENABLE_CPP_EXCEPTIONS': 'NO',        # -fno-exceptions
          'GCC_ENABLE_CPP_RTTI': 'NO',              # -fno-rtti
          'GCC_ENABLE_PASCAL_STRINGS': 'NO',        # No -mpascal-strings
          'GCC_THREADSAFE_STATICS': 'NO',           # -fno-threadsafe-statics
          'PREBINDING': 'NO',                       # No -Wl,-prebind
          'MACOSX_DEPLOYMENT_TARGET': '10.5',       # -mmacosx-version-min=10.5
          'USE_HEADERMAP': 'NO',
          'OTHER_CFLAGS': [
            '-fno-strict-aliasing',
          ],
          'WARNING_CFLAGS': [
            '-Wall',
            '-Wendif-labels',
            '-W',
            '-Wno-unused-parameter',
          ],
        },
        'target_conditions': [
          ['_type!="static_library"', {
            'xcode_settings': {'OTHER_LDFLAGS': ['-Wl,-search_paths_first']},
          }],
        ],
        'conditions': [
          ['target_arch=="ia32"', {
            'xcode_settings': {'ARCHS': ['i386']},
          }],
          ['target_arch=="x64"', {
            'xcode_settings': {'ARCHS': ['x86_64']},
          }],
          ['clang==1', {
            'xcode_settings': {
              'GCC_VERSION': 'com.apple.compilers.llvm.clang.1_0',
              'CLANG_CXX_LANGUAGE_STANDARD': 'gnu++0x',  # -std=gnu++0x
            },
          }],
        ],
      }],
      ['OS=="freebsd"', {
        'ldflags': [
          '-Wl,--export-dynamic',
        ],
      }]
    ],
  }
}
