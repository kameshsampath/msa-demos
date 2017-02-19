#!/usr/bin/groovy
@Library('github.com/fabric8io/fabric8-pipeline-library@v2.2.311')

def localItestPattern = ""
try {
  localItestPattern = ITEST_PATTERN
} catch (Throwable e) {
  localItestPattern = "*KT"
}

def localFailIfNoTests = ""
try {
  localFailIfNoTests = ITEST_FAIL_IF_NO_TEST
} catch (Throwable e) {
  localFailIfNoTests = "false"
}

def versionPrefix = ""
try {
  versionPrefix = VERSION_PREFIX
} catch (Throwable e) {
  versionPrefix = "1.0"
}

def canaryVersion = "${versionPrefix}.${env.BUILD_NUMBER}"

def fabric8Console = "${env.FABRIC8_CONSOLE ?: ''}"
def utils = new io.fabric8.Utils()
def label = "buildpod.${env.JOB_NAME}.${env.BUILD_NUMBER}".replace('-', '_').replace('/', '_')

mavenNode{
  def envStage = utils.environmentNamespace('staging')
  def envProd = utils.environmentNamespace('production')

  git 'https://github.com/kameshsampath/msa-demos.git'

  echo 'NOTE: running pipelines for the first time will take longer as build and base docker images are pulled onto the node'
  container(name: 'maven') {

    stage 'Build Release'
    mavenCanaryRelease {
      version = canaryVersion
    }

    stage 'Integration Testing'
    mavenIntegrationTest {
      environment = 'Testing'
      failIfNoTests = localFailIfNoTests
      itestPattern = localItestPattern
    }

    stage 'Rollout Staging'
    kubernetesApply(environment: envStage)

    stage 'Approve'
    approve {
      room = null
      version = canaryVersion
      console = fabric8Console
      environment = 'Staging'
    }

    stage 'Rollout Production'
    kubernetesApply(environment: envProd)

  }
}
