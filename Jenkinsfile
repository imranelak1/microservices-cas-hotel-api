pipeline {
  agent any
  stages {
    stage('Sources') { steps { checkout scm } }
    stage('Tests Java') { steps { sh 'mvn -B clean verify' } }
    stage('Tests Apollo') { steps { dir('graphql-api') { sh 'npm ci && npm test' } } }
    stage('Client React') { steps { dir('frontend') { sh 'npm ci && npm run build' } } }
    stage('Images') { steps { sh 'docker compose -f compose.yml -f compose-apps.yml build' } }
  }
  post { always { junit '**/target/surefire-reports/TEST-*.xml' } }
}
