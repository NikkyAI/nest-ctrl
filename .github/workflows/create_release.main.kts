#!/usr/bin/env kotlin

@file:Repository("https://repo.maven.apache.org/maven2/")
@file:DependsOn("io.github.typesafegithub:github-workflows-kt:3.3.0")

@file:Repository("https://bindings.krzeminski.it")

@file:DependsOn("actions:checkout:v4")
@file:DependsOn("actions:setup-java:v3")
@file:DependsOn("softprops:action-gh-release:v2.0.6")
@file:DependsOn("joutvhu:/create-release:v1.0.1")
@file:DependsOn("gradle:actions__setup-gradle:v3")
@file:DependsOn("jimeh:update-tags-action:v1.0.1")
@file:DependsOn("Dylan700:sftp-upload-action:v1.2.3")

import io.github.typesafegithub.workflows.actions.actions.Checkout
import io.github.typesafegithub.workflows.actions.actions.SetupJava
import io.github.typesafegithub.workflows.actions.gradle.ActionsSetupGradle
import io.github.typesafegithub.workflows.actions.softprops.ActionGhRelease
import io.github.typesafegithub.workflows.domain.RunnerType
import io.github.typesafegithub.workflows.domain.triggers.Push
import io.github.typesafegithub.workflows.dsl.expressions.expr
import io.github.typesafegithub.workflows.dsl.workflow
import io.github.typesafegithub.workflows.yaml.ConsistencyCheckJobConfig

workflow(
    name = "package distributable",
    on = listOf(
        Push(tags = listOf("v*"))
    ),
    sourceFile = __FILE__,
    consistencyCheckJobConfig = ConsistencyCheckJobConfig.Configuration(
        condition = null,
        env = emptyMap(),
        additionalSteps = {

        },
        useLocalBindingsServerAsFallback = false
    )
) {
    job(id = "build_release", runsOn = RunnerType.WindowsLatest
    ) {
        uses(name = "Check out", action = Checkout())

        uses(
            name = "setup jdk",
            action = SetupJava(
                javaPackage = SetupJava.JavaPackage.Jdk,
                javaVersion = "21",
                architecture = "x64",
                distribution = SetupJava.Distribution.Adopt,
                cache = SetupJava.BuildPlatform.Gradle,
            )
        )

        uses(
            name = "setup gradle",
            action = ActionsSetupGradle()
        )

        run(command = "./gradlew packageDistributionForCurrentOS -Ptag=${expr { github.ref_name }} --no-daemon")

//        uses(
//            name = "update tag",
//            action = UpdateTagsAction_Untyped(
//                tags_Untyped = "nightly"
//            )
//        )

        uses(
            name = "create release",
            action = ActionGhRelease(
//                body = "Nightly Build",
//                draft = false,
//                prerelease = false,
                files = listOf(
                    "build/compose/binaries/main/msi/*.msi",
//                    "build/nestctrl.zip"
                ),
//                name = "Nightly",
//                tagName = "nightly",
                failOnUnmatchedFiles = true,
//                token = expr { github.token },
                generateReleaseNotes = true,
//                makeLatest = ActionGhRelease.MakeLatest.True,
            )
        )
    }
}

