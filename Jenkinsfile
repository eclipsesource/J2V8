node {
  stage('Checkout') {
    checkout scm
  }

  stage('Fetching target platform') {
    sh 'curl -O http://download.eclipsesource.com/j2v8/v8/node.out-7_4_0.tar.gz'
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
