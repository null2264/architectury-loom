package net.fabricmc.loom.test.integration.forge

import net.fabricmc.loom.test.util.GradleProjectTestTrait
import spock.lang.Specification
import spock.lang.Unroll

import static net.fabricmc.loom.test.LoomTestConstants.DEFAULT_GRADLE
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class FmlLogConfigTest extends Specification implements GradleProjectTestTrait {
    @Unroll
    def "use fml logger config (minecraft #minecraft, forge #forge)"() {
        setup:
        def gradle = gradleProject(project: 'forge/loggerConfig', version: DEFAULT_GRADLE)
        gradle.gradleProperties << """
        
        minecraft_version=$minecraft
        forge_version=$forge
        """.stripIndent()

        when:
        def result = gradle.run(task: 'generateLog4jConfig')

        then:
        result.task(':generateLog4jConfig').outcome == SUCCESS
        def logFile = new File(gradle.projectDir, '.gradle/loom-cache/log4j.xml')
        logFile.text.contains('forge.logging')

        where:
        minecraft | forge
        '1.19.4'  | '45.0.43'
        '1.18.1'  | '39.0.63'
        '1.17.1'  | '37.0.67'
        '1.16.5'  | '36.2.4'
        '1.14.4'  | '28.2.23'
    }
}
