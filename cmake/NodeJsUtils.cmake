
function (get_njs_libs nodejs_dir config_name)
#{
    # base directories for Node.js link libraries
    set (njs_build ${nodejs_dir}/build/${config_name})
    set (njs_build_lib ${nodejs_dir}/build/${config_name}/lib)

    set (njs_extra ${nodejs_dir}/${config_name})
    set (njs_extra_lib ${nodejs_dir}/${config_name}/lib)

    # project link libraries
    set (njs_libs
        # nodejs/build/$Config/lib
        ${njs_build_lib}/v8_base_0.lib
        ${njs_build_lib}/v8_base_1.lib
        ${njs_build_lib}/v8_base_2.lib
        ${njs_build_lib}/v8_base_3.lib
        ${njs_build_lib}/v8_libbase.lib
        ${njs_build_lib}/v8_libplatform.lib
        ${njs_build_lib}/v8_nosnapshot.lib
        ${njs_build_lib}/v8_snapshot.lib

        # nodejs/build/$Config
        ${njs_build}/mksnapshot.lib

        # nodejs/$Config/lib
        ${njs_extra_lib}/cares.lib
        ${njs_extra_lib}/gtest.lib
        ${njs_extra_lib}/http_parser.lib
        ${njs_extra_lib}/libuv.lib
        ${njs_extra_lib}/node.lib
        ${njs_extra_lib}/openssl.lib
        ${njs_extra_lib}/zlib.lib

        # nodejs/$Config
        ${njs_extra}/cctest.lib
    )

    set (njs_${config_name}_libs ${njs_libs} PARENT_SCOPE)
#}
endfunction (get_njs_libs)
