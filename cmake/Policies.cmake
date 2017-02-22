
if (COMMAND cmake_policy)
#{
    # NEW = Libraries linked by full-path must have a valid library file name.
    if (POLICY CMP0008)
        cmake_policy (SET CMP0008 NEW)
    endif (POLICY CMP0008)

    # NEW = Included scripts do automatic cmake_policy PUSH and POP.
    if (POLICY CMP0011)
        cmake_policy (SET CMP0011 NEW)
    endif(POLICY CMP0011)
#}
endif (COMMAND cmake_policy)
