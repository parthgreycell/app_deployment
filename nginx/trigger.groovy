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

    def PROJECTS = [
      ["publish", "https://github.com/parthgreycell/app_deployment.git"],
    ]

    if (''.equals(TagName)){
      echo "[FAILURE] TagName parameter is required - Failed to build, value provided : ${TagName}"
      currentBuild.result = 'FAILURE'
      throw new RuntimeException("required parameter missing : TagName");
    }

    for (String PROJECT in PROJECTS) {
        echo "$PROJECT[1]"
      def PROJECT_NAME = PROJECT[1].split('/parthgreycell/')[1].split('.git')[0]
      stage("Publish ${PROJECT_NAME} from [${TagName}]") {
        println("Project: ${PROJECT_NAME} [${TagName}]\nRepository: ${PROJECT[1]}\n");
        build(
          job: PROJECT[0],
          parameters: [
            [
              $class: 'ListSubversionTagsParameterValue',
              name: 'TagName',
              tag: TagName,
              tagsDir: PROJECT[1]
            ]
          ],
          quietPeriod: 5
        )
      }
    }
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