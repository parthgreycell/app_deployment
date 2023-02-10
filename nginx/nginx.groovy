node{
  try{
    properties([
      parameters([
        [$class: 'ListSubversionTagsParameterDefinition', credentialsId: 'munjal-gc-un-pw', defaultValue: '', maxTags: '', name: 'TagName', reverseByDate: true, reverseByName: false, tagsDir: 'https://github.com/BidClips/BidClips-API-Restheart.git', tagsFilter: '']
      ])
    ])
    def PUBLISHTAG = ""
    def repoRegion = ""

    stage('Preparation'){
       if (''.equals(TagName)){
        echo "[FAILURE] TagName parameter is required - Failed to build, value provided : ${TagName}"
        currentBuild.result = 'FAILURE'
        throw new RuntimeException("required parameter missing : ${TagName}");
      }
      dir('BidClips-API-Restheart') {
        if (TagName.equals('trunk')) {
          TagName = 'branches/master'
          checkout poll: false, scm: [$class: 'GitSCM', branches: [[name: 'master']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'munjal-gc', url: 'git@github.com:BidClips/BidClips-API-Restheart.git']]]
          PUBLISHTAG = sh(
            script: 'echo $(git log -1 --pretty=%h)',
            returnStdout: true
          ).trim()
          repoRegion = "us-east-1"

        }
      }
    }
    }

    stage('Building Docker Image'){
      dir('BidClips-API-Restheart') {
        sh """
        docker build --file=Dockerfile --tag=bidclips-api-restheart:${PUBLISHTAG} .
        """
      }
    }
    stage("Publishing ${PUBLISHTAG}"){
        sh """
  // export AWS_PROFILE=bidclips-eks
  aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin 561279971319.dkr.ecr.us-east-1.amazonaws.com
  docker tag bidclips-api-restheart:${PUBLISHTAG} 561279971319.dkr.ecr.us-east-1.amazonaws.com/bidclips-api-restheart:${PUBLISHTAG}
  docker push 561279971319.dkr.ecr.us-east-1.amazonaws.com/bidclips-api-restheart:${PUBLISHTAG}
          """
    }
    stage('Cleanup'){
      sh """
      docker image rmi -f bidclips-api-restheart:${PUBLISHTAG}
      docker image rmi -f 561279971319.dkr.ecr.${repoRegion}.amazonaws.com/bidclips-api-restheart:${PUBLISHTAG}
      """
      dir('BidClips-Infrastructure') {
        deleteDir()
      }
      dir('BidClips-API-Restheart') {
        deleteDir()
      }
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
