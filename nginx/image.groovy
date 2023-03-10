node{
  try{
     properties([
    parameters([
        [$class: 'ListSubversionTagsParameterDefinition', credentialsId: 'git_token', defaultValue: '', maxTags: '', name: 'TagName', reverseByDate: true, reverseByName: false, tagsDir: 'https://github.com/parthgreycell/app_deployment.git', tagsFilter: '']
      ])
    ])

    def PUBLISHTAG = ""
    def IMAGE = ""
    def repoRegion = "us-east-1"

    stage('Preparation'){
       if (''.equals(TagName)){
        echo "[FAILURE] TagName parameter is required - Failed to build, value provided : ${TagName}"
        currentBuild.result = 'FAILURE'
        throw new RuntimeException("required parameter missing : ${TagName}");
      }

      dir('app_deployment') {
        if (TagName.startsWith('tags')) {
          checkout poll: false, scm: [$class: 'GitSCM', branches: [[name: 'refs/${TagName}']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'git_token', url: 'git@github.com:parthgreycell/app_deployment.git']]]
          PUBLISHTAG = TagName.split('/')[1]
          IMAGE = "helloimg"
          REPO = "tag"
          repoRegion = "us-east-1"

          sh """  
          mkdir /home/greycell/hello
          ls
        cp mysql/Dockerfile /home/greycell/hello
        ls
        docker build -t helloimg:${PUBLISHTAG} /home/greycell/hello/
        docker images
        rm -rf /home/greycell/hello
        """
        }

        if (TagName.startsWith('branches')) {
          def branch = TagName.split('/')[1]
          checkout poll: false, scm: [$class: 'GitSCM', branches: [[name: branch]], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'git_token', url: 'git@github.com:parthgreycell/app_deployment.git']]]
          PUBLISHTAG = sh(
            script: 'echo $(git log -1 --pretty=%h)',
            returnStdout: true
          ).trim()

          IMAGE = "pythonimg"
          REPO = "branch"
          repoRegion = "us-east-1"

          sh """  
          mkdir /home/greycell/python
          ls
        cp image/Dockerfile /home/greycell/python
        ls /home/greycell/python
        docker build -t pythonimg:${PUBLISHTAG} /home/greycell/python/
        rm -rf /home/greycell/python
        """
        }

        if (TagName.equals('trunk')) {
          TagName = 'branches/master'
          checkout poll: false, scm: [$class: 'GitSCM', branches: [[name: 'master']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'git_token', url: 'git@github.com:parthgreycell/app_deployment.git']]]
          PUBLISHTAG = sh(
            script: 'echo $(git log -1 --pretty=%h)',
            returnStdout: true
          ).trim()

          IMAGE = "nginximg"
          REPO = "nginx"
          repoRegion = "us-east-1"
          
          sh """  
          mkdir /home/greycell/docker
        cp nginx/Dockerfile /home/greycell/docker
        docker build -t nginximg:${PUBLISHTAG} /home/greycell/docker/
        rm -rf /home/greycell/docker
        """
        }
      }
    }

    stage("Publishing ${PUBLISHTAG}"){
        sh """
        export AWS_PROFILE=default
   aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin 561279971319.dkr.ecr.us-east-1.amazonaws.com 
  docker tag ${IMAGE}:${PUBLISHTAG} 561279971319.dkr.ecr.us-east-1.amazonaws.com/${REPO}:${PUBLISHTAG}
  docker push 561279971319.dkr.ecr.us-east-1.amazonaws.com/${REPO}:${PUBLISHTAG}
          """
    }

    stage('Cleanup'){
      sh """
      docker image rmi -f ${IMAGE}:${PUBLISHTAG}
      docker image rmi -f 561279971319.dkr.ecr.${repoRegion}.amazonaws.com/${REPO}:${PUBLISHTAG} 
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
