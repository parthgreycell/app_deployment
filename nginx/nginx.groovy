node{
  try{
    def PUBLISHTAG = "latest"
    def repoRegion = ""
    
    stage('Building Docker Image'){
      dir('parth') {
        checkout poll: false, scm: [$class: 'GitSCM', branches: [[name: 'master']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'git_token', url: 'git@github.com:parthgreycell/app_deployment.git']]]
        sh """  
        mkdir docker      
        cp nginx/Dockerfile docker/
        ls docker/
        cd docker
        sudo groupadd docker
        sudo usermod -aG docker $USER
        chmod 777 *
        ls -ltr
        sudo docker build -t nginximg:${PUBLISHTAG} .
        #docker build -t nginximg:${PUBLISHTAG} nginx/
        docker images
        """
      }
    }

    stage("Publishing ${PUBLISHTAG}"){
        sh '''
        export AWS_PROFILE=default
   aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin 561279971319.dkr.ecr.us-east-1.amazonaws.com 
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
