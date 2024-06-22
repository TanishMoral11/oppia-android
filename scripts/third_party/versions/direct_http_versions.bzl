"""
Contains all of the HTTP archive/jar & Git repository dependencies that are directly required for
production & test builds of Oppia Android scripts. These are exposed via DIRECT_REMOTE_DEPENDENCIES.
"""

load(
    "//third_party/macros:direct_dep_defs.bzl",
    "EXPORT_TOOLCHAIN",
    "create_export_binary_details",
    "create_export_library_details",
    "create_git_repository_reference",
    "create_http_jar_reference",
)

DIRECT_REMOTE_DEPENDENCIES = [
    create_http_jar_reference(
        name = "android_bundletool",
        sha = "1e8430002c76f36ce2ddbac8aadfaf2a252a5ffbd534dab64bb255cda63db7ba",
        version = "1.8.0",
        url = "https://github.com/google/bundletool/releases/download/{0}/bundletool-all-{0}.jar",
        test_only = False,
        exports_details = [
            create_export_library_details(
                exposed_artifact_name = "android_bundletool",
                exportable_target = "jar",
                export_toolchain = EXPORT_TOOLCHAIN.ANDROID,
            ),
            create_export_binary_details(
                exposed_artifact_name = "android_bundletool_binary",
                main_class = "com.android.tools.build.bundletool.BundleToolMain",
                exportable_runtime_target = "jar",
            ),
        ],
    ),
    create_git_repository_reference(
        name = "archive_patcher",
        commit = "50ca40a3de7983392a383ed3cc7b48e25f1b69b3",
        remote = "https://github.com/google/archive-patcher",
        test_only = False,
        build_file = "//scripts/third_party/versions/mods:BUILD.archive-patcher",
        export_details = create_export_library_details(
            exposed_artifact_name = "com_google_archivepatcher",
            exportable_target = ":tools",
            export_toolchain = EXPORT_TOOLCHAIN.ANDROID,
        ),
    ),
    create_git_repository_reference(
        name = "oppia_proto_api",
        commit = "9cf993ea0b798a67b3faa21c690c30b9027fb371",
        remote = "https://github.com/oppia/oppia-proto-api",
        test_only = False,
        exports_details = [
            create_export_library_details(
                exposed_artifact_name = "oppia_proto_api_protos",
                exportable_target = ":android_protos",
                export_toolchain = EXPORT_TOOLCHAIN.ALIAS,
            ),
            create_export_library_details(
                exposed_artifact_name = "oppia_proto_api_java_protos",
                exportable_target = ":android_java_protos",
                export_toolchain = EXPORT_TOOLCHAIN.ALIAS,
            ),
        ],
    ),
]
