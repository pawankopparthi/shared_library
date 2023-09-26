import CICDEnvUtils
import com.apigee.loader.BootStrapConfigLoad
import hudson.AbortException
import hudson.model.Cause
import groovy.json.JsonSlurper
import pom
import com.apigee.boot.Pipeline
//import com.apigee.cicd.service.CIEnvInfoService
//import com.apigee.cicd.service.DeploymentInfoService
//import com.apigee.cicd.service.DefaultConfigService
import com.apigee.cicd.service.FunctionalTestService
//import com.apigee.cicd.service.OrgInfoService

// This Model is used for non java callout Proxies
//
def call() {

  def scmAPILocation
  def scmOauthServerLocation
  def apiManageServerURL
  def apiManageOauthURL
  def scmAccessToken

  def access
  def refresh

  def scmCloneURL

  node ('apigee') {

    withCredentials([
      [$class: 'UsernamePasswordMultiBinding',
        credentialsId: "pawan_token",
        usernameVariable: 'scmUser',
        passwordVariable: 'scmPassword'
      ]
      

    ]) {

      withFolderProperties {

        BootStrapConfigLoad configLoad = new BootStrapConfigLoad();
        try {
          //echo "API_SERVER_LOCATION: ${env.API_SERVER_LOCATION}"
          scmAPILocation = env.API_SCM_LOCATION
          scmOauthServerLocation = env.API_SCM_OAUTH_SERVER
          // apiManageServerURL = env.API_MANAGESERVER_LOCATION
          apiManageOauthURL = env.API_MANAGESERVER_OAUTH_LOCATION
          //configLoad.setupConfig("${env.API_SERVER_LOCATION}")
        } catch (MalformedURLException e) {
          e.printStackTrace();
        }
      }

      deleteDir()
      String proxyRootDirectory = "."

      def javaDirectoryExists = false;

      try {

        //def nodeHome = tool name: DefaultConfigService.instance.tools.nodejs, type: 'Node16'

        //Not required as maven is fixed using runCommand in below function
       // def mvnHome = tool name: DefaultConfigService.instance.tools.maven, type: 'maven'

       // echo "Maven Home is ${mvnHome}"
        def settings_file = "/usr/share/maven/conf/settings.xml"

       def exampleApi = "mvn archetype:generate " +
            "-DarchetypeGroupId=com.hdfc.apigee.archetype.poc " +
            "-DarchetypeArtifactId=api-pass-through-poc " +
            "-DarchetypeVersion=1.0.0-SNAPSHOT " +
            "-DgroupId=com.hdfc.api " +
            "-DartifactId=${params.ApiName} " +
            "-Dpackage=com.hdfc.api " +
            "-DApiName=${params.ApiName} " +
            "-DinteractiveMode=false " +
         "-s /usr/share/maven/conf/settings.xml "
         
        
       
        
        def appUrl = "${env.BUILD_URL}ws"

        echo "API Generated Can be Found Here : ${appUrl}"

        stage("arche-type-generate") {

          dir('target') {
            sh "${exampleApi}"
          }
        }
        stage("create-scm-repo") {
         sh '''
         curl  -L -k POST -u $scmUser:$scmPassword https://api.github.com/orgs/pawansidgs/repos -d '{"name":"'${ApiName}'","public":true}'
         '''
    }

      stage("Code-push") {
        dir("target/${ApiName}") {
          // def defRepURL= scmCloneURL.split("@")[1]
          def scmCloneURLFinal = "https://${env.scmUser}:${env.scmPassword}@github.com/pawansidgs/${Apiname}"
          sh "pwd"
          sh "ls -la"
          sh "git init"
          sh "git add ."
          sh "git config --global user.email  pawan.kopparthi@hdfcbank.com"
          sh "git config --global user.name pawankopparthi"
          sh "git commit -m intial-commit"
          sh "git remote add origin ${scmCloneURLFinal}"
          sh "git push -u origin master"
          sh "git branch develop master"
          sh "git checkout develop"
          sh "git push --set-upstream origin develop"

        }
        dir("target") {
          echo "Cleaning directory after commit"
          sh "rm -rf ./*"
        }
      }

    }
    catch (any) {
      println any.toString()
      currentBuild.result = 'FAILURE'
      //DeploymentInfoService.instance.saveDeploymentStatus("FAILURE", env.BUILD_URL, getUsernameForBuild())
    }
  }
}
}

@NonCPS
def parseJsonText(String json) {
  def object = new JsonSlurper().parseText(json)
  if (object instanceof groovy.json.internal.LazyMap) {
    return new HashMap < > (object)
  }
  return object
}
def getRepoCreated() {
  def userDetails = "-v -u" +
    "username=${env.scmUser}:password=${env.scmPassword} ";
  return userDetails as String;
}

def getUsernameForBuild() {
  def causes = currentBuild.rawBuild.getCauses()
  for (Cause cause in causes) {
    def user;
    if (cause instanceof hudson.model.Cause.UserIdCause) {
      hudson.model.Cause.UserIdCause userIdCause = cause;
      user = userIdCause.getUserName()
      return user
    }
  }
  return null
}

def isBuildCauseUserAction() {
  def causes = currentBuild.rawBuild.getCauses()
  for (Cause cause in causes) {
    if (cause instanceof hudson.model.Cause.UserIdCause) return true
  }
  return false
}

def getPom() {
  return new pom();
}

/*def runCommand(String command) {
  if (!isUnix()) {
    println command
    if (command.trim().toLowerCase().startsWith("mvn")) {
       def mavenToolName = 'Maven 3.9.4'
      def mavenTool = tool name: mavenToolName, type: 'hudson.tasks.Maven'
      withEnv(["M2_HOME=${mavenTool}"]) {
        sh "${mavenTool}/bin/mvn ${command}"
      }
    } else {

      bat returnStdout: true, script: "${command}"
    }
  } else {
    println command
    if (command.trim().toLowerCase().startsWith("mvn")) {
       def mavenToolName = 'Maven 3.9.4'
      def mavenTool = tool name: mavenToolName, type: 'hudson.tasks.Maven'
       withEnv(["M2_HOME=${mavenTool}"]) {
        sh "${mavenTool}/bin/mvn ${command}"
      }
    } else {
      sh returnStdout: true, script: command
    }

  }
}*/
