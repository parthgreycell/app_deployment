node {
  try{
    properties(
      [
        parameters(
          [
            [
              $class: 'ListSubversionTagsParameterDefinition',
              credentialsId: 'git_token',
              defaultValue: '',
              maxTags: '',
              name: 'TagName',
              reverseByDate: true,
              reverseByName: false,
              tagsDir: 'https://github.com/parthgreycell/app_deployment.git',
              tagsFilter: ''
            ]
          ]
        )
      ]
    )


    if (''.equals(TagName)){
      echo "[FAILURE] TagName parameter is required - Failed to build, value provided : ${TagName}"
      currentBuild.result = 'FAILURE'
      throw new RuntimeException("required parameter missing : TagName");
    }
        build(
          job: "publish",
          parameters: [
            [
                $class: 'ListSubversionTagsParameterValue',
                name: 'TagName',
                tag: TagName,
                tagsDir: 'https://github.com/parthgreycell/app_deployment.git',
            ]
          ],
          quietPeriod: 5
        )
  }

  catch( exec ){
    echo "FAILURE: ${exec}"
    currentBuild.result = 'FAILURE'
  }

  finally {
    cleanWs()
    //Ref: https://jenkins.io/doc/pipeline/tour/post/
    echo 'FINALLY BLOCK!'
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