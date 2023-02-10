node{
  try{
    def PUBLISHTAG = """sh date +%Y_%m_%d-%H_%M_%S-%A"""
    def repoRegion = ""
    
    stage('Building Docker Image'){
      // dir('BidClips-API-Restheart') {
        sh """
        docker build --file=Dockerfile --tag=nginx:${PUBLISHTAG} nginx/
        """
      // }
    }

    stage("Publishing ${PUBLISHTAG}"){
        sh """
  aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin 561279971319.dkr.ecr.us-east-1.amazonaws.com
  docker tag nginx:${PUBLISHTAG} 561279971319.dkr.ecr.us-east-1.amazonaws.com/nginx:${PUBLISHTAG}
  docker push 561279971319.dkr.ecr.us-east-1.amazonaws.com/nginx:${PUBLISHTAG}
          """
    }
    stage('Cleanup'){
      sh """
      docker image rmi -f nginx:${PUBLISHTAG}
      docker image rmi -f 561279971319.dkr.ecr.${repoRegion}.amazonaws.com/nginx:${PUBLISHTAG}
      """
    }
  }
  
  catch( exec ) {
    echo "FAILURE: ${exec}"
    currentBuild.result = 'FAILURE'
  }

  finally {
    echo 'FINALLY BLOCK!'
    cleanWs()
    if (currentBuild.result == 'UNSTABLE') {
      echo 'I am unstable :/'
    }
    else if (currentBuild.result == 'FAILURE'){
      echo 'FAILURE!'
    }
    else {
      echo 'One way or another, I have finished'
    }
}
}
