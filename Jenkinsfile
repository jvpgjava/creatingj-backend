pipeline {
    agent any

    options {
        disableConcurrentBuilds()
        timestamps()
        timeout(time: 20, unit: 'MINUTES')
    }

    environment {
        MAVEN_OPTS = '-Xmx1024m'
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

        stage('Archive Artifacts') {
            steps {
                archiveArtifacts artifacts: 'target/creatingj-backend-*.jar,openapi/openapi-v1.yaml,target/site/jacoco/jacoco.xml', fingerprint: true
            }
        }
    }
}
