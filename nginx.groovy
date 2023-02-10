node{
  try{
    properties([
      parameters([
        [$class: 'ListSubversionTagsParameterDefinition', credentialsId: 'munjal-gc-un-pw', defaultValue: '', maxTags: '', name: 'TagName', reverseByDate: true, reverseByName: false, tagsDir: 'https://github.com/BidClips/BidClips-Mainstreet-API.git', tagsFilter: '']
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
    //   dir('BidClips-Mainstreet-API') {
    //     if (TagName.equals('trunk')) {
    //       TagName = 'branches/master'
    //       checkout poll: false, scm: [$class: 'GitSCM', branches: [[name: 'master']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'munjal-gc', url: 'git@github.com:BidClips/BidClips-Mainstreet-API.git']]]
    //       PUBLISHTAG = sh(
    //         script: 'echo $(git log -1 --pretty=%h)',
    //         returnStdout: true
    //       ).trim()
    //       repoRegion = "ap-southeast-1"
    //     }
    //   }

    //   dir('BidClips-Infrastructure') {
    //     // Cloning Infra repo for configurations
    //     checkout poll: false, scm: [$class: 'GitSCM', branches: [[name: "master"]], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'munjal-gc', url: 'git@github.com:BidClips/BidClips-Infrastructure.git']]]
    //   }
    // }
    stage('Building Docker Image'){
      dir('BidClips-Mainstreet-API') {
        sh """
        rm src/main/resources/logback-spring.xml
        cp ../BidClips-Infrastructure/common/BidClips-Mainstreet-API/logback.xml src/main/resources/logback-spring.xml
        sudo update-alternatives --set java /usr/lib/jvm/java-1.8.0-openjdk-1.8.0.282.b08-1.amzn2.0.1.x86_64/jre/bin/java
        sudo update-alternatives --set javac /usr/lib/jvm/java-1.8.0-openjdk-1.8.0.282.b08-1.amzn2.0.1.x86_64/bin/javac
        ./gradlew bootJar -Pprod jibDockerBuild
        sudo update-alternatives --set java /usr/lib/jvm/java-11-openjdk-11.0.11.0.9-1.amzn2.0.1.x86_64/bin/java
        sudo update-alternatives --set javac /usr/lib/jvm/java-11-openjdk-11.0.11.0.9-1.amzn2.0.1.x86_64/bin/javac
        """
      }
    }

    stage("Publishing ${PUBLISHTAG}"){
      sh """
export AWS_PROFILE=bidclips-eks
aws ecr get-login-password --region ${repoRegion} | docker login --username AWS --password-stdin 566570633830.dkr.ecr.${repoRegion}.amazonaws.com
docker tag bidclipsmainstreetapi 566570633830.dkr.ecr.${repoRegion}.amazonaws.com/bidclips-mainstreet-api:${PUBLISHTAG}
docker push 566570633830.dkr.ecr.${repoRegion}.amazonaws.com/bidclips-mainstreet-api:${PUBLISHTAG}
        """
    }
    stage('Cleanup'){
      sh """
      docker image rmi -f bidclipsmainstreetapi
      docker image rmi -f 566570633830.dkr.ecr.${repoRegion}.amazonaws.com/bidclips-mainstreet-api:${PUBLISHTAG}
      """
      dir('BidClips-Infrastructure') {
        deleteDir()
      }
      dir('BidClips-Mainstreet-API') {
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
