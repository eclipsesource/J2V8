
if (COMMAND cmake_policy)
#{
    # NEW = Libraries linked by full-path must have a valid library file name.
    # see: https://cmake.org/cmake/help/v3.0/policy/CMP0008.html
    if (POLICY CMP0008)
        cmake_policy (SET CMP0008 NEW)
    endif (POLICY CMP0008)

    # NEW = Included scripts do automatic cmake_policy PUSH and POP.
    # see: https://cmake.org/cmake/help/v3.0/policy/CMP0011.html
    if (POLICY CMP0011)
        cmake_policy (SET CMP0011 NEW)
    endif(POLICY CMP0011)
#}
endif (COMMAND cmake_policy)
