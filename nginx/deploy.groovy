node("built-in"){
  try{
    properties([
      buildDiscarder(logRotator(numToKeepStr: '10')),
      disableConcurrentBuilds(abortPrevious: false),
      disableResume(),
      parameters([
        choice(choices: ['dev'], description: '', name: 'DeployEnv'),
        [$class: 'ListSubversionTagsParameterDefinition', credentialsId: 'git_token', defaultValue: '', maxTags: '', name: 'TagName', reverseByDate: true, reverseByName: false, tagsDir: 'https://github.com/parthgreycell/app_deployment.git', tagsFilter: '']
      ])
    ])
    def DEPLOYTAG = ""
    def dockerImageWithTag = ""
    def repoRegion = [
      "dev": "ap-southeast-1"
    ]
    def bootstrapper = [
      "dev": "***"
    ]
    stage('Preparation'){
      if (''.equals(TagName)){
        echo "[FAILURE] TagName parameter is required - Failed to build, value provided : ${TagName}"
        currentBuild.result = 'FAILURE'
        throw new RuntimeException("required parameter missing : ${TagName}");
      }
      if (TagName.startsWith('tags')) {
        DEPLOYTAG = TagName.split('/')[1]
        repoRegion = "us-east-1"
        IMAGE = "helloimg"
        REPO = "tag"
      }
      else{
        dir('app_deployment') {
          if (TagName.startsWith('branches')) {
            branch = TagName.split('/')[1]
            checkout poll: false, scm: [$class: 'GitSCM', branches: [[name: branch]], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'git_token', url: 'git@github.com:parthgreycell/app_deployment.git']]]

            IMAGE = "pythonimg"
            REPO = "branch"
          }
          if (TagName.equals('trunk')) {
            TagName = 'branches/master'
            checkout poll: false, scm: [$class: 'GitSCM', branches: [[name: 'master']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'git_token', url: 'git@github.com:parthgreycell/app_deployment.git']]]
          }
          DEPLOYTAG = sh(
            script: 'echo $(git log -1 --pretty=%h)',
            returnStdout: true
          ).trim()
          repoRegion = "us-east-1"
          IMAGE = "nginximg"
          REPO = "nginx"
          deleteDir()
        }
      }

      
        dockerImageWithTag="561279971319.dkr.ecr.${repoRegion}.amazonaws.com/bidclips-api-restheart:${DEPLOYTAG}".replace(':','\\:')

      
        dir("app_deployment"){
        checkout poll: false, scm: [$class: 'GitSCM', branches: [[name: 'master']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'git_token', url: 'git@github.com:parthgreycell/app_deployment.git']]]
        sh """
cd nginx/
sed -i 's#REPLACEME_DOCKER_IMAGE_WITH_TAG#$dockerImageWithTag#g' deployment.yaml
scp deployment.yaml ec2-user@18.140.71.163:/home/ec2-user/deployment.yaml
        """
      }
    }

    stage("Deploying ${DEPLOYTAG}"){
      sh """
ssh -tt ec2-user@*** /bin/bash << EOA
export AWS_DEFAULT_REGION="${repoRegion}"
sleep 5;
kubectl  apply -f deployment.yaml
rm deployment.*
sleep 5;
kubectl -n app-stack get deploy | grep deployment
exit
EOA
      """
    }

    stage("cleanup"){
      dir("app_deployment"){
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
