import org.apache.commons.csv.*

def call(body) {
    body()

    pipeline {
        agent any
        tools {
            git 'Default'
        }

        stages {
            stage('checkout and deploy') {
                steps {
                    // checkout using git
                    // git branch: env.gitlabTargetBranch, credentialsId: '216620ce-5a09-425c-b73e-75c6078057cc', url: env.gitlabSourceRepoHttpUrl

                    // Tests

                    // Code quality checks

                    // read csv with portals
                    script {
                        def portals = readCSV file: 'builds/PortalList.csv', text: '', format: CSVFormat.DEFAULT.withHeader()

                        for (int i = 0; i < portals.size(); ++i) {
                            echo "publishing to portal: ${portals[i].get('Portal')}"
                            echo "using ssh configuration: ${portals[i].get('ConfigName')}"
                            echo "publishing to remote directory: ${portals[i].get('RemoteDir')}"

                            // deploy using ssh to all portal dirs
                            sshPublisher(publishers: [sshPublisherDesc(configName: portals[i].get('ConfigName'),
                                    transfers: [sshTransfer(cleanRemote: false, excludes: '',
                                            execCommand: '', execTimeout: 120000,
                                            flatten: false, makeEmptyDirs: false, noDefaultExcludes: false,
                                            patternSeparator: '[, ]+', remoteDirectory: portals[i].get('RemoteDir'),
                                            remoteDirectorySDF: false, removePrefix: '', sourceFiles: '**/*')],
                                    usePromotionTimestamp: false, useWorkspaceInPromotion: false, verbose: false)])

                            sh "exit 1"
                        }
                    }
                }
            }
        }
        post {
            failure {
                emailext from: 'thomasmosigfrey@googlemail.com', body: "${env.JOB_NAME}\\n${env.BUILD_URL}", recipientProviders: [developers(), upstreamDevelopers()], subject: "Failed: ${env.JOB_NAME} ${env.BUILD_NUMBER}", to: 'thomas.frey@edv-frey.de'
            }
            always {
                emailext body: """${env.JOB_NAME}
${env.BUILD_URL}""", recipientProviders: [developers(), upstreamDevelopers()], subject: "RUN: ${env.JOB_NAME} ${env.BUILD_NUMBER}", to: 'thomas.frey@edv-frey.de'
                cleanWs cleanWhenAborted: false, cleanWhenFailure: false, cleanWhenNotBuilt: false, notFailBuild: true
            }
        }
    }
}
