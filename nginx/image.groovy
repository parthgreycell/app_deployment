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
          IMAGE = "mysqlimg"
          repoRegion = "us-east-1"

          sh """  
          mkdir /home/greycell/mysql
          ls
        cp mysql/Dockerfile /home/greycell/mysql
        ls
        docker build -t mysqlimg:${PUBLISHTAG} /home/greycell/mysql/
        docker images
        rm -rf /home/greycell/mysql
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
  docker tag ${IMAGE}:${PUBLISHTAG} 561279971319.dkr.ecr.us-east-1.amazonaws.com/nginx:${PUBLISHTAG}
  docker push 561279971319.dkr.ecr.us-east-1.amazonaws.com/nginx:${PUBLISHTAG}
          """
    }

    stage('Cleanup'){
      sh """
      docker image rmi -f nginximg:${PUBLISHTAG}
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
