#!groovy
pipeline {
  agent any
  stages {
    // Checkout the repository
    stage('Checkout from GitHub') {
        steps {
            checkout([$class: 'GitSCM',
                branches: [[name: 'master']],
                doGenerateSubmoduleConfigurations: false,
                extensions: [
                    [$class: 'CleanCheckout']
                ],
                submoduleCfg: [],
                userRemoteConfigs: [[credentialsId: '42cd9098-46d6-4322-bded-d51f0aad8a68', url: 'git@github.com:amido/profile-attribute-decision-auth-tree-node.git']]
            ])
        }
      }

      stage('Dependency Check') {
        steps {
            //try {
                sh "mvn dependency-check:aggregate"
          //  } catch (ignored) {
              // Could be because the CVE update failed, or CVSS limit reached
          //      message: "OWASP dependency check failed see the report for the errors. Error Msg: ${ignored.message}"
          //  } finally {
                publishHTML(target: [
                    alwaysLinkToLastBuild: true,
                    keepAll              : true,
                    reportDir            : "target/",
                    reportFiles          : 'dependency-check-report.html',
                    reportName           : 'Dependency Check Report'
                ])
          //  }
        }
      }

      stage('Build') {
        steps {
            sh "mvn clean package"
        }
      }

      stage('Deploy') {
        steps {
            sh "mvn deploy"
        }
      }
  }
}
