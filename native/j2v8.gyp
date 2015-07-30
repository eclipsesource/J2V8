{
  'variables': {
    'v8_use_snapshot%': 'false',
    'j2v8_target_type%': 'shared_library',
    'j2v8_v8_options%': 'false',
  },
  'targets': [
    {
      'target_name': 'j2v8',
      'type': '<(j2v8_target_type)',

      'dependencies': [
        'deps/v8/tools/gyp/v8.gyp:v8',
        'deps/v8/tools/gyp/v8.gyp:v8_libplatform'
      ],

      'include_dirs': [
        'src',
        'deps/v8', # include/v8_platform.h
        '$(JAVA_HOME)/include',
        '$(JAVA_HOME)/include/linux',
      ],

      'sources': [
        'src/com_eclipsesource_v8_V8Impl.cc',
        'common.gypi',
      ],

      'defines': [
        'J2V8_ARCH="<(target_arch)"',
        'J2V8_PLATFORM="<(OS)"',
        'J2V8_V8_OPTIONS="<(j2v8_v8_options)"',
      ],

      'conditions': [
        [ 'v8_postmortem_support=="true"', {
          'dependencies': [ 'deps/v8/tools/gyp/v8.gyp:postmortem-metadata' ],
          'conditions': [
            # -force_load is not applicable for the static library
            [ 'j2v8_target_type!="static_library"', {
              'xcode_settings': {
                'OTHER_LDFLAGS': [
                  #'-Wl,-force_load,<(V8_BASE)',
                ],
              },
            }],
          ],
        }],
        [ 'OS=="win"', {
          'defines': [
            'FD_SETSIZE=1024',
            '_UNICODE=1',
          ],
          'libraries': [ '-lpsapi.lib' ]
        }, { # POSIX
          'defines': [ '__POSIX__' ],
        }],
        [ 'OS=="mac"', {
          # linking Corefoundation is needed since certain OSX debugging tools
          # like Instruments require it for some features
          'libraries': [ '-framework CoreFoundation' ],
        }],
        [ 'OS=="freebsd"', {
          'libraries': [
            '-lutil',
            '-lkvm',
          ],
        }],
        [ 'OS=="solaris"', {
          'libraries': [
            '-lkstat',
            '-lumem',
          ],
        }],
        [ 'OS=="freebsd" or OS=="linux"', {
          'ldflags': [ '-Wl,-z,noexecstack',
                       #'-Wl,--whole-archive <(V8_BASE)',
                       '-Wl,--no-whole-archive' ]
        }],
        [ 'OS=="sunos"', {
          'ldflags': [ '-Wl,-M,/usr/lib/ld/map.noexstk' ],
        }],
      ],
    },
  ] # end targets
}
