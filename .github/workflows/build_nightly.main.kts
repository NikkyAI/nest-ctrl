#!/usr/bin/env kotlin

@file:Repository("https://repo.maven.apache.org/maven2/")
@file:DependsOn("io.github.typesafegithub:github-workflows-kt:2.1.1")

@file:Repository("https://bindings.krzeminski.it")

@file:DependsOn("actions:checkout:v4")
@file:DependsOn("actions:setup-java:v3")
@file:DependsOn("softprops:action-gh-release:v2.0.6")
@file:DependsOn("joutvhu:/create-release:v1.0.1")
@file:DependsOn("gradle:actions__setup-gradle:v3")

import io.github.typesafegithub.workflows.actions.actions.CheckoutV4
import io.github.typesafegithub.workflows.actions.actions.CreateReleaseV1
import io.github.typesafegithub.workflows.actions.actions.SetupJava
import io.github.typesafegithub.workflows.actions.gradle.ActionsSetupGradle
import io.github.typesafegithub.workflows.actions.softprops.ActionGhRelease
import io.github.typesafegithub.workflows.actions.softprops.ActionGhReleaseV2
import io.github.typesafegithub.workflows.domain.RunnerType.UbuntuLatest
import io.github.typesafegithub.workflows.domain.triggers.Push
import io.github.typesafegithub.workflows.dsl.expressions.expr
import io.github.typesafegithub.workflows.dsl.workflow
import io.github.typesafegithub.workflows.yaml.ConsistencyCheckJobConfig

workflow(
    name = "Test workflow",
    on = listOf(
        Push(branches = listOf("main"))
    ),
    sourceFile = __FILE__,
    consistencyCheckJobConfig = ConsistencyCheckJobConfig.Configuration(
        condition = null,
        env = emptyMap()
    ) {

    }
) {
    job(id = "build_nightly", runsOn = UbuntuLatest) {
        uses(name = "Check out", action = CheckoutV4())

        uses(
            name = "setup jdk",
            action = SetupJava(
                javaPackage = SetupJava.JavaPackage.Jdk,
                javaVersion = "18",
                architecture = "x64",
                distribution = SetupJava.Distribution.Adopt,
                cache = SetupJava.BuildPlatform.Gradle,
            )
        )

        uses(
            name = "setup gradle",
            action = ActionsSetupGradle()
        )

        run(command = "./gradlew packageJar --no-daemon")


        uses(
            name = "create release",
            action = ActionGhRelease(
                body = "Nightly Build",
                draft = false,
                prerelease = true,
                files = listOf(
                    "build/nestctrl.jar"
                ),
                name = "Nightly",
                tagName = "nightly",
                failOnUnmatchedFiles = true,
//                token = expr { github.token },
                generateReleaseNotes = true,
//                makeLatest = ActionGhRelease.MakeLatest.True,
            )
        )
    }
}

