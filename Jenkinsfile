pipeline {
    agent any
    tools { jdk 'temurin-25' }
    stages {
        stage('Smoke') {
            steps {
                sh 'java -version'
                sh './mvnw -v'
            }
        }
    }
}