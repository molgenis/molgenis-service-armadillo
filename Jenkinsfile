pipeline {
    agent {
        kubernetes {
            // the shared pod template defined in the Jenkins server config
            inheritFrom 'shared'
            // maven jdk17 pod template defined in molgenis/molgenis-jenkins-pipeline repository
            yaml libraryResource("pod-templates/maven-jdk17.yaml")
        }
    }
    environment {
        REPOSITORY = 'molgenis/armadillo'
        LOCAL_REPOSITORY = "${LOCAL_REGISTRY}/${REPOSITORY}"
        CHART_VERSION = '0.15.1'
        TIMESTAMP = sh(returnStdout: true, script: "date -u +'%F_%H-%M-%S'").trim()
    }
    stages {
        stage('Prepare') {
            when {
                anyOf {
                    allOf {
                        changeRequest()
                        branch 'PR-*'
                    }
                    branch 'master'
               }
            }
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
                        env.GITHUB_USER = sh(script: 'vault read -field=username secret/ops/token/github', returnStdout: true)
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
                allOf {
                    changeRequest()
                    branch 'PR-*'
               }
            }
            environment {
                // PR-1234-231
                TAG = "PR-${CHANGE_ID}-${BUILD_NUMBER}"
            }
            stages {
                stage('Build [ PR ]') {
                    steps {
                        container('java') {
                            script {
                                sh "./gradlew test --no-daemon jacocoMergedReport shadowJar ci \
                                -Dsonar.login=${SONAR_TOKEN} -Dsonar.organization=molgenis -Dsonar.host.url=https://sonarcloud.io \
                                -Dorg.ajoberstar.grgit.auth.username=${GITHUB_TOKEN} -Dorg.ajoberstar.grgit.auth.password"
                                def props = readProperties file: 'build/ci.properties'
                                env.TAG_NAME = props.tagName
                           }
                        }
                    }
                    post {
                        always {
                            junit '**/target/surefire-reports/**.xml'
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
                            dir('armadillo') {
                                script {
                                    sh "mvn -q -B jib:build -Ddockerfile.tag=${TAG} -Ddockerfile.repository=${LOCAL_REPOSITORY}"
                                    sh "mvn -q -B jib:build -Ddockerfile.tag=dev -Ddockerfile.repository=${LOCAL_REPOSITORY}"
                                }
                            }
                        }
                    }
                }
                stage("Deploy to dev [ master ]") {
                    steps {
                        milestone(ordinal: 100, label: 'deploy to armadillo.dev.molgenis.org')
                        container('rancher') {
                            sh "rancher apps upgrade --set 'server.image.tag=${TAG}' --set 'server.image.repository=${LOCAL_REPOSITORY}' armadillo ${CHART_VERSION}"
                        }
                    }
                }
                stage('Prepare Release [ master ]') {
                    steps {
                        timeout(time: 40, unit: 'MINUTES') {
                            input(message: 'Prepare to release?')
                        }
                        container('maven') {
                            sh "mvn -q -B release:prepare -Dmaven.test.redirectTestOutputToFile=true -Darguments=\"-q -B -Dmaven.test.redirectTestOutputToFile=true\""
                        }
                    }
                }
                stage('Perform release [ master ]') {
                    steps {
                        container('maven') {
                            sh "mvn -q -B release:perform -Darguments=\"-q -B -Dmaven.test.redirectTestOutputToFile=true\""
                        }
                    }
                }
            }
        }
    }
}
