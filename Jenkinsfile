pipeline {
    agent {
        kubernetes {
            label 'molgenis-jdk11'
        }
    }
    environment {
        REPOSITORY = 'molgenis/datashield-service'
        LOCAL_REPOSITORY = "${LOCAL_REGISTRY}/${REPOSITORY}"
        CHART_VERSION = '0.0.1'
        TIMESTAMP = sh(returnStdout: true, script: "date -u +'%F_%H-%M-%S'").trim()
    }
    stages {
        stage('Prepare') {
            steps {
                script {
                    env.GIT_COMMIT = sh(script: 'git rev-parse HEAD', returnStdout: true).trim()
                }
                container('vault') {
                    script {
                        sh "mkdir ${JENKINS_AGENT_WORKDIR}/.rancher"
                        sh "mkdir ${JENKINS_AGENT_WORKDIR}/.m2"
                        sh(script: "vault read -field=value secret/ops/jenkins/rancher/cli2.json > ${JENKINS_AGENT_WORKDIR}/.rancher/cli2.json")
                        sh(script: "vault read -field=value secret/ops/jenkins/maven/settings.xml > ${JENKINS_AGENT_WORKDIR}/.m2/settings.xml")
                        env.GITHUB_TOKEN = sh(script: "vault read -field=value secret/ops/token/github", returnStdout: true)
                        env.SONAR_TOKEN = sh(script: 'vault read -field=value secret/ops/token/sonar', returnStdout: true)
                    }
                }
                dir("${JENKINS_AGENT_WORKDIR}/.m2") {
                    stash includes: 'settings.xml', name: 'maven-settings'
                }
                dir("${JENKINS_AGENT_WORKDIR}/.rancher") {
                    stash includes: 'cli2.json', name: 'rancher-config'
                }
            }
        }
        stage('Steps [ PR ]') {
            when {
                changeRequest()
            }
            environment {
                // PR-1234-231
                TAG = "PR-${CHANGE_ID}-${BUILD_NUMBER}"
            }
            stages {
                stage('Build [ PR ]') {
                    steps {
                        container('maven') {
                            script {
                                env.PREVIEW_VERSION = sh(script: "grep version pom.xml | grep SNAPSHOT | cut -d'>' -f2 | cut -d'<' -f1", returnStdout: true).trim() + "-${env.TAG}"
                            }
                            sh "mvn -q -B versions:set -DnewVersion=${PREVIEW_VERSION} -DgenerateBackupPoms=false -T1C"
                            sh "mvn -q -B clean install -Dmaven.test.redirectTestOutputToFile=true -DskipITs -T1C -DargLine='-XX:TieredStopAtLevel=1 -noverify'"
                            // Fetch the target branch, sonar likes to take a look at it
                            sh "git fetch --no-tags origin ${CHANGE_TARGET}:refs/remotes/origin/${CHANGE_TARGET}"
                            sh "mvn -q -B sonar:sonar -Dsonar.login=${env.SONAR_TOKEN} -Dsonar.github.oauth=${env.GITHUB_TOKEN} -Dsonar.pullrequest.base=${CHANGE_TARGET} -Dsonar.pullrequest.branch=${BRANCH_NAME} -Dsonar.pullrequest.key=${env.CHANGE_ID} -Dsonar.pullrequest.provider=GitHub -Dsonar.pullrequest.github.repository=molgenis/molgenis-service-datashield -Dsonar.ws.timeout=120 -T1C"
                        }
                    }
                    post {
                        always {
                            junit '**/target/surefire-reports/**.xml'
                        }
                    }
                }
                stage('Push to registries [ PR ]') {
                    steps {
                        container('maven') {
                            script {
                                sh "mvn -q -B dockerfile:build dockerfile:tag dockerfile:push -Ddockerfile.tag=${TAG} -Ddockerfile.repository=${LOCAL_REPOSITORY} -T1C"
                            }
                        }
                    }
                }
            }
        }
        stage('Steps [ master ]') {
            when {
                branch 'master'
            }
            environment {
                TAG = "dev-${TIMESTAMP}"
            }
            stages {
                stage('Build [ master ]') {
                    steps {
                        container('maven') {
                            sh "mvn -q -B clean install -Dmaven.test.redirectTestOutputToFile=true -DskipITs"
                            sh "mvn -q -B sonar:sonar -Dsonar.login=${SONAR_TOKEN} -Dsonar.ws.timeout=120"
                        }
                    }
                    post {
                        always {
                            junit '**/target/surefire-reports/**.xml'
                        }
                    }
                }
                stage('Push to registries [ master ]') {
                    steps {
                        container('maven') {
                            script {
                                sh "mvn -q -B dockerfile:build dockerfile:tag dockerfile:push -Ddockerfile.tag=${TAG} -Ddockerfile.repository=${LOCAL_REPOSITORY}"
                                sh "mvn -q -B dockerfile:tag dockerfile:push -Ddockerfile.tag=dev -Ddockerfile.repository=${LOCAL_REPOSITORY}"
                            }
                        }
                    }
                }
                stage("Deploy to dev [ master ]") {
                    steps {
                        milestone(ordinal: 100, label: 'deploy to datashield.dev.molgenis.org')
                        container('rancher') {
                            sh "rancher apps upgrade --set image.tag=${TAG} datashield ${CHART_VERSION}"
                        }
                    }
                }
            }
        }
    }
    post {
        success {
            hubotSend(message: 'Build success', status: 'INFO', site: 'slack-pr-app-team')
        }
        failure {
            hubotSend(message: 'Build failed', status: 'ERROR', site: 'slack-pr-app-team')
        }
    }
}