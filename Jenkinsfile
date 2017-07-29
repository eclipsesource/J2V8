node {
  stage('Checkout') {
    checkout scm
  }

  stage('Fetching target platform') {
    sh 'curl -o node.out-7_6_0.tar.gz https://nodejs.org/dist/v7.6.0/node-v7.6.0-linux-x64.tar.gz'
  }

  stage('Building') {
    sh './buildAll.sh -r /data/instances/j2v8-jenkins/workspace/J2V8-nightly'
  } 

  stage('Package AAR') {
    sh './gradlew clean build'
  }

  stage('Results') {
      junit 'build/test-results/**/*.xml'
      archive 'build/outputs/aar/*.aar'
   }
}
