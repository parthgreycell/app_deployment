node{
  try{
    def PUBLISHTAG = "agahsgdh"
    def repoRegion = ""
    
    stage('Building Docker Image'){
      dir('parth') {
        checkout poll: false, scm: [$class: 'GitSCM', branches: [[name: 'master']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'git_token', url: 'git@github.com:parthgreycell/app_deployment.git']]]
        sh """        
        cd nginx
        ls
        # docker build --file=Dockerfile --tag=nginx:${PUBLISHTAG} nginx/
        pwd
        docker build -t nginximg:${PUBLISHTAG} Dockerfile
        docker images
         export AWS_PROFILE=default
  sudo aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin 561279971319.dkr.ecr.us-east-1.amazonaws.com 
        """
      }
    }

    stage("Publishing ${PUBLISHTAG}"){
        sh '''
  docker tag nginx:${PUBLISHTAG} 561279971319.dkr.ecr.us-east-1.amazonaws.com/nginx:${PUBLISHTAG}
  docker push 561279971319.dkr.ecr.us-east-1.amazonaws.com/nginx:${PUBLISHTAG}
          '''
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
