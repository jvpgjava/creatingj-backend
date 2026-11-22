pipeline {
    agent any

    options {
        disableConcurrentBuilds()
        timestamps()
        timeout(time: 20, unit: 'MINUTES')
    }

    parameters {
        choice(name: 'DEPLOY_TARGET', choices: ['none', 'staging', 'prod'], description: 'Alvo opcional de deploy sem Docker')
    }

    environment {
        MAVEN_OPTS = '-Xmx1024m'
        RELEASE_ID = "${env.BUILD_NUMBER}-${env.GIT_COMMIT ? env.GIT_COMMIT.take(8) : 'local'}"
    }

    stages {
        stage('Verify Environment') {
            steps {
                sh 'java -version'
                sh 'chmod +x ./mvnw'
            }
        }

        stage('Build, Lint, Tests, ArchUnit and JaCoCo') {
            steps {
                sh './mvnw -B clean verify'
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml,target/failsafe-reports/*.xml'
                }
            }
        }

        stage('Audit Contract and Artifacts for Secrets') {
            steps {
                sh '''
                    set -e
                    if grep -E "(CRJ_API_KEY=|STAC_API_KEY=|passwordHash|-----BEGIN .* PRIVATE KEY-----)" openapi/openapi-v1.yaml; then
                        echo "Secret or forbidden field found in OpenAPI snapshot!"
                        exit 1
                    fi
                '''
            }
        }

        stage('Archive Release Artifacts') {
            steps {
                archiveArtifacts artifacts: 'target/creatingj-backend-*.jar,openapi/openapi-v1.yaml,target/site/jacoco/jacoco.xml', fingerprint: true
            }
        }

        stage('Deploy Release to VPS (systemd + rollback)') {
            when {
                expression { params.DEPLOY_TARGET != 'none' }
            }
            steps {
                withCredentials([sshUserPrivateKey(credentialsId: "creatingj-deploy-${params.DEPLOY_TARGET}", keyFileVariable: 'SSH_KEY', usernameVariable: 'SSH_USER')]) {
                    sh '''
                        set -euo pipefail
                        JAR_FILE=$(ls target/creatingj-backend-*.jar | grep -v original | head -n 1)
                        scp -i "$SSH_KEY" -o StrictHostKeyChecking=yes "$JAR_FILE" "${SSH_USER}@${CREATINGJ_DEPLOY_HOST}:/tmp/creatingj-backend-${RELEASE_ID}.jar"
                        ssh -i "$SSH_KEY" -o StrictHostKeyChecking=yes "${SSH_USER}@${CREATINGJ_DEPLOY_HOST}" \
                            "/opt/creatingj/backend/deploy.sh /tmp/creatingj-backend-${RELEASE_ID}.jar ${RELEASE_ID}"
                    '''
                }
            }
        }
    }
}
