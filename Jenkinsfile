node {
  stage('Checkout') {
    checkout scm
  }

  stage('Fetching target platform') {
    sh 'curl -O http://download.eclipsesource.com/j2v8/v8/node.out-7_4_0.tar.gz'
  }

  stage('Building') {
    sh './buildAll.sh'
  } 

}
