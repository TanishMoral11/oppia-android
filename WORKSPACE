"""
This file initializes all external dependencies & toolchains needed to build Oppia Android.
"""

# Note to developer: The order of loads & calls below MUST NOT BE CHANGED--it will guarantee a
# breakage as subsequent loads depend on both previous loads & calls. There is no other order
# possible for the configuration below, and it ought to never need to be changed.

workspace(name = "oppia_android")

# The list of repositories from which all Maven-tied dependencies may be downloaded.
_MAVEN_REPOSITORIES = [
    "https://maven.fabric.io/public",
    "https://maven.google.com",
    "https://repo1.maven.org/maven2",
]

load(
    "//scripts/third_party/versions:direct_http_versions.bzl",
    scripts_remote_deps = "DIRECT_REMOTE_DEPENDENCIES",
)
load("//third_party/macros:direct_dep_downloader.bzl", "download_direct_workspace_dependencies")
load(
    "//third_party/versions:direct_http_versions.bzl",
    app_remote_deps = "DIRECT_REMOTE_DEPENDENCIES",
)

download_direct_workspace_dependencies(app_remote_deps, _MAVEN_REPOSITORIES)

download_direct_workspace_dependencies(scripts_remote_deps, _MAVEN_REPOSITORIES)

# Initialize workspace toolchains in 3 steps in case there are multiple loads necessary.
load("//third_party/tools:toolchains_step1.bzl", "initialize_toolchains_for_workspace_step1")

initialize_toolchains_for_workspace_step1()

load("//third_party/tools:toolchains_step2.bzl", "initialize_toolchains_for_workspace_step2")

initialize_toolchains_for_workspace_step2()

load("//third_party/tools:toolchains_step3.bzl", "initialize_toolchains_for_workspace_step3")

initialize_toolchains_for_workspace_step3()

load(
    "//scripts/third_party/versions:maven_config.bzl",
    scripts_maven_artifact_config = "MAVEN_ARTIFACT_CONFIGURATION",
)
load("//third_party/macros:direct_dep_loader.bzl", "download_maven_dependencies")
load(
    "//third_party/versions:maven_config.bzl",
    app_maven_artifact_config = "MAVEN_ARTIFACT_CONFIGURATION",
)

download_maven_dependencies("maven_app", app_maven_artifact_config, _MAVEN_REPOSITORIES)

download_maven_dependencies("maven_scripts", scripts_maven_artifact_config, _MAVEN_REPOSITORIES)

load("@maven_app//:defs.bzl", pinned_maven_install_app = "pinned_maven_install")
load("@maven_scripts//:defs.bzl", pinned_maven_install_scripts = "pinned_maven_install")

pinned_maven_install_app()

pinned_maven_install_scripts()
