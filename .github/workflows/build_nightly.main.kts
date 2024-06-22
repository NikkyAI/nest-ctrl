#!kotlin

@file:Repository("https://repo.maven.apache.org/maven2/")
@file:DependsOn("io.github.typesafegithub:github-workflows-kt:2.1.1")

@file:Repository("https://bindings.krzeminski.it")

@file:DependsOn("actions:checkout:v4")
@file:DependsOn("actions:setup-java:v3")
@file:DependsOn("IsaacShelton:update-existing-release:v1.3.4")
@file:DependsOn("gradle:actions__setup-gradle:v3")

import io.github.typesafegithub.workflows.actions.actions.CheckoutV4
import io.github.typesafegithub.workflows.actions.actions.SetupJava
import io.github.typesafegithub.workflows.actions.gradle.ActionsSetupGradle
import io.github.typesafegithub.workflows.actions.isaacshelton.UpdateExistingRelease
import io.github.typesafegithub.workflows.domain.RunnerType.UbuntuLatest
import io.github.typesafegithub.workflows.domain.triggers.Push
import io.github.typesafegithub.workflows.dsl.expressions.expr
import io.github.typesafegithub.workflows.dsl.workflow
import io.github.typesafegithub.workflows.yaml.ConsistencyCheckJobConfig

workflow(
    name = "Test workflow",
    on = listOf(Push()),
    sourceFile = __FILE__,
    consistencyCheckJobConfig = ConsistencyCheckJobConfig.Configuration(
        condition = null,
        env = emptyMap()
    ) {

    }
) {
    job(id = "test_job", runsOn = UbuntuLatest) {
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

        run(command = "./gradlew packageJar")

        uses(
            name = "create release",
            action = UpdateExistingRelease(
                token = expr { github.token },
                release = "Nightly",
                tag = "nightly",
                files = "build/nestctrl.jar",
                prerelease = "true",
                replace = "true",
//                updateTag = "true"
            )
        )
    }
}

