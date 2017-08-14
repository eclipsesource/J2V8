#-----------------------------------------------------------------------
# Expects a list of absolute paths to the expectd Node.js static libraries
# and exits CMake with a fatal error if one of the libs does not exist.
#-----------------------------------------------------------------------
function (assert_nodejs_libs_exist)
    # ARGN: a list of absolute paths
    set (njs_libs ${ARGN})

    foreach(lib ${njs_libs})
        if (NOT EXISTS ${lib})
            message(FATAL_ERROR "ERROR: Unable to locate required Node.js library: ${lib}")
        endif()
    endforeach()

endfunction(assert_nodejs_libs_exist)
#-----------------------------------------------------------------------
# Based on the used operating system, look for the static Node.js libraries
# that must be included for linking the J2V8 JNI native bridge code.
#-----------------------------------------------------------------------
function (get_njs_libs nodejs_dir config_name fail_on_missing_libs)
#{
    #-----------------------------------------------------------------------
    if (CMAKE_SYSTEM_NAME STREQUAL "Windows")
    #-----------------------------------------------------------------------
    #{
        # base directories for Node.js link libraries
        set (njs_build ${nodejs_dir}/build/${config_name})
        set (njs_build_lib ${nodejs_dir}/build/${config_name}/lib)

        set (njs_extra ${nodejs_dir}/${config_name})
        set (njs_extra_lib ${nodejs_dir}/${config_name}/lib)

        # Node.js link libraries
        set (njs_libs
            # nodejs/build/$Config/lib
            ${njs_build_lib}/standalone_inspector.lib
            ${njs_build_lib}/v8_base_0.lib
            ${njs_build_lib}/v8_base_1.lib
            ${njs_build_lib}/v8_base_2.lib
            ${njs_build_lib}/v8_base_3.lib
            ${njs_build_lib}/v8_libbase.lib
            ${njs_build_lib}/v8_libplatform.lib
            ${njs_build_lib}/v8_libsampler.lib
            ${njs_build_lib}/v8_nosnapshot.lib
            ${njs_build_lib}/v8_snapshot.lib

            # nodejs/build/$Config
            ${njs_build}/mksnapshot.lib

            # nodejs/$Config/lib
            ${njs_extra_lib}/cares.lib
            ${njs_extra_lib}/gtest.lib
            ${njs_extra_lib}/http_parser.lib
            ${njs_extra_lib}/icudata.lib
            ${njs_extra_lib}/icui18n.lib
            ${njs_extra_lib}/icustubdata.lib
            ${njs_extra_lib}/icutools.lib
            ${njs_extra_lib}/icuucx.lib
            ${njs_extra_lib}/libuv.lib
            ${njs_extra_lib}/node.lib
            ${njs_extra_lib}/openssl.lib
            ${njs_extra_lib}/zlib.lib

            # nodejs/$Config
            ${njs_extra}/cctest.lib
        )

        # verify that all required Node.js libs actually exist
        if (${fail_on_missing_libs})
            assert_nodejs_libs_exist(${njs_libs})
        endif()

        # additional link libraries
        set (njs_libs ${njs_libs}
            # additional windows libs, required by Node.js
            Dbghelp
            Shlwapi
        )

        set (njs_${config_name}_libs ${njs_libs} PARENT_SCOPE)
    #}
    #-----------------------------------------------------------------------
    elseif(CMAKE_SYSTEM_NAME STREQUAL "Darwin")
    #-----------------------------------------------------------------------
    #{
        # base directories for Node.js link libraries
        set (njs_out ${nodejs_dir}/out/${config_name})

        # Node.js link libraries
        set (njs_libs
            # v8 libs
            ${njs_out}/libv8_base.a
            ${njs_out}/libv8_libbase.a
            ${njs_out}/libv8_libplatform.a
            ${njs_out}/libv8_nosnapshot.a
            ${njs_out}/libv8_libsampler.a

            # node libs
            ${njs_out}/libcares.a
            ${njs_out}/libgtest.a
            ${njs_out}/libhttp_parser.a
            ${njs_out}/libuv.a
            ${njs_out}/libopenssl.a
            ${njs_out}/libzlib.a
        )

        # verify that all required Node.js libs actually exist
        if (${fail_on_missing_libs})
            assert_nodejs_libs_exist(${njs_libs} ${njs_out}/libnode.a)
        endif()

        # additional link libraries
        set (njs_libs ${njs_libs}
            # Node.js libs that require special linker treatments
            -force_load ${njs_out}/libnode.a
        )

        set (njs_${config_name}_libs ${njs_libs} PARENT_SCOPE)
    #}
    #-----------------------------------------------------------------------
    elseif(CMAKE_SYSTEM_NAME STREQUAL "Android")
    #-----------------------------------------------------------------------
    #{
        # base directories for Node.js link libraries
        set (njs_out_target ${nodejs_dir}/out/${config_name}/obj.target)
        set (njs_out_v8 ${nodejs_dir}/out/${config_name}/obj.target/deps/v8/src)
        set (njs_out_deps ${nodejs_dir}/out/${config_name}/obj.target/deps)

        # Node.js link libraries
        set (njs_libs
            # node libs
            ${njs_out_deps}/uv/libuv.a
            ${njs_out_deps}/openssl/libopenssl.a
            ${njs_out_deps}/http_parser/libhttp_parser.a
            ${njs_out_deps}/gtest/libgtest.a
            ${njs_out_deps}/zlib/libzlib.a
            ${njs_out_deps}/cares/libcares.a

            # v8 libs
            ${njs_out_v8}/libv8_base.a
            ${njs_out_v8}/libv8_nosnapshot.a
            ${njs_out_v8}/libv8_libplatform.a
            ${njs_out_v8}/libv8_libbase.a
            ${njs_out_v8}/libv8_libsampler.a
        )

        # verify that all required Node.js libs actually exist
        if (${fail_on_missing_libs})
            assert_nodejs_libs_exist(${njs_libs} ${njs_out_target}/libnode.a)
        endif()

        # finalize linker settings
        set (njs_libs
            # <LinkerGroup>
            -Wl,--start-group
                # the carefree libs
                ${njs_libs}

                # Node.js libs that require special linker treatments
                -Wl,--whole-archive ${njs_out_target}/libnode.a -Wl,--no-whole-archive
            -Wl,--end-group
            # </LinkerGroup>
        )

        set (njs_${config_name}_libs ${njs_libs} PARENT_SCOPE)
    #}
    #-----------------------------------------------------------------------
    elseif(CMAKE_SYSTEM_NAME STREQUAL "Linux")
    #-----------------------------------------------------------------------
    #{
        # base directories for Node.js link libraries
        set (njs_out_target ${nodejs_dir}/out/${config_name}/obj.target)
        set (njs_out_v8 ${nodejs_dir}/out/${config_name}/obj.target/deps/v8/src)
        set (njs_out_deps ${nodejs_dir}/out/${config_name}/obj.target/deps)

        # Node.js link libraries
        set (njs_libs
            # node libs
            ${njs_out_deps}/uv/libuv.a
            ${njs_out_deps}/openssl/libopenssl.a
            ${njs_out_deps}/http_parser/libhttp_parser.a
            ${njs_out_deps}/gtest/libgtest.a
            ${njs_out_deps}/zlib/libzlib.a
            ${njs_out_deps}/cares/libcares.a

            # v8 libs
            ${njs_out_v8}/libv8_base.a
            ${njs_out_v8}/libv8_nosnapshot.a
            ${njs_out_v8}/libv8_libplatform.a
            ${njs_out_v8}/libv8_libbase.a
            ${njs_out_v8}/libv8_libsampler.a
        )

        # verify that all required Node.js libs actually exist
        if (${fail_on_missing_libs})
            assert_nodejs_libs_exist(${njs_libs} ${njs_out_target}/libnode.a)
        endif()

        # finalize linker settings
        set (njs_libs
            # <LinkerGroup>
            -Wl,--start-group
                # the carefree libs
                ${njs_libs}

                # Node.js libs that require special linker treatments
                -Wl,--whole-archive ${njs_out_target}/libnode.a -Wl,--no-whole-archive
            -Wl,--end-group
            # </LinkerGroup>
        )

        set (njs_${config_name}_libs ${njs_libs} PARENT_SCOPE)
    #}
    endif()
#}
endfunction (get_njs_libs)
#-----------------------------------------------------------------------
