/*
 * Android stdio stubs for V8
 * 
 * V8's platform code expects stdout/stderr/stdin as linkable symbols,
 * but Android's Bionic libc provides them as macros. These stubs
 * provide the actual symbols that the linker needs.
 */

#include <stdio.h>
#include <stdarg.h>

// Save the real Android stdio before undefining
#define ANDROID_STDOUT (&__sF[1])
#define ANDROID_STDERR (&__sF[2])
#define ANDROID_STDIN  (&__sF[0])

// Undefine the macros so we can declare variables with these names
#undef stdout
#undef stderr
#undef stdin

// Now declare the actual FILE* variables that V8 expects
FILE* stdout;
FILE* stderr;
FILE* stdin;

// Initialize them to point to Android's actual stdio streams
__attribute__((constructor))
static void init_stdio_stubs(void) {
    stdout = ANDROID_STDOUT;
    stderr = ANDROID_STDERR;
    stdin  = ANDROID_STDIN;
}

// Provide __fwrite_chk for fortified builds
// This is the checked version of fwrite used with _FORTIFY_SOURCE
size_t __fwrite_chk(const void *ptr, size_t size, size_t nmemb, FILE *stream, size_t buflen) {
    // Simple passthrough to regular fwrite
    // In a real fortified implementation, we'd check buffer bounds
    return fwrite(ptr, size, nmemb, stream);
}
