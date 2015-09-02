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
        'deps/v8',
        '$(JAVA_HOME)/include'
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
        [ 'OS=="win"', {
          'defines': [
            'FD_SETSIZE=1024',
            '_UNICODE=1',
          ],
          'libraries': [ '-lpsapi.lib' ],
	      'include_dirs': [
            '$(JAVA_HOME)/include/windows',
          ]
        }, { #, POSIX
          'defines': [ '__POSIX__' ],
        }],
        [ 'OS=="mac"', {
          # linking Corefoundation is needed since certain OSX debugging tools
          # like Instruments require it for some features
          'libraries': [ '-framework CoreFoundation' ],
          'include_dirs': [
            '$(JAVA_HOME)/include/darwin',
          ],
        }],
        [ 'OS=="linux"', {
          'include_dirs': [
            '$(JAVA_HOME)/include/linux',
          ],
        }],
      ],
    },
  ] # end targets
}
