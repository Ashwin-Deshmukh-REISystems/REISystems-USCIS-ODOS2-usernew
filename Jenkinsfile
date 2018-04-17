// version var for entire scope
def version = ''

node ('') {
  env.DEV_PROJECT = "dev"
  env.APP_NAME = "api" //"${env.JOB_NAME}".replaceAll(/-?${env.PROJECT_NAME}-?/, '').replaceAll(/-?pipeline-?/, '')
  // these are defaults that will help run openshift automation
  // env.OCP_API_SERVER = "${env.OPENSHIFT_API_URL}"
  // env.OCP_TOKEN = readFile('/var/run/secrets/kubernetes.io/serviceaccount/token').trim()
  env.sonar = ""
}

// Run on docker nodes
node (''){

  env.PATH = "${tool 'M3'}/bin:${env.PATH}"
  env.PATH = "${tool 'oc-3.6.1'}:${env.PATH}"
  
  def app_image

  //Use try catch to set build success criteria, set true to start
  currentBuild.result = "SUCCESS"

  stage('Checkout') {
    try {
      checkout scm
      
      // Keep git hash in var
      sh "git rev-parse HEAD > commit-id"
      env.GIT_COMMIT = readFile('commit-id')
      sh 'rm -f commit-id'

      //if ("${env.BRANCH_NAME}" != "master") {
        version = "0.0.1-unstable${env.BUILD_NUMBER}"
      //} else {
      //  version = "0.1.0"
      //}
      env.VERSION=version
    } catch (err) {
      echo "Error encountered: ${err}"
      throw err
    }      
  }
  
  stage('Build') {
    try {
      sh "mvn -Dmaven.test.skip=true -B clean package"
    } catch (err) {
      echo "Error encountered: ${err}"
      throw err
    }
  }

  stage('SonarQube Code Analysis') {
    try {
      withCredentials([string(credentialsId: 'sonarqube-token', variable: 'SONAR_TOKEN')]) {
        sh "mvn -B -X sonar:sonar -Dsonar.login=$SONAR_TOKEN -Dsonar.host.url=http://sonarqube.uscis-fads.local"
      }

      timeout(time: 2, unit: 'MINUTES') { // Just in case something goes wrong, pipeline will be killed after a timeout
        def qg = waitForQualityGate() // Reuse taskId previously collected by withSonarQubeEnv
        if (qg.status != 'OK') {
          echo "SonarQube webhook not working."
          //error "Pipeline aborted due to quality gate failure: ${qg.status}"
        }
      }
    } catch (err) {
      echo "Error encountered: ${err}"
      throw err
    }
  }

  stage 'Fortify Security Code Analysis'
  try {
    sh "rm -rf maven-sca"
    sh "ln -sf /opt/HPE_Security/Fortify_SCA_and_Apps_17.20/plugins/maven maven-sca"
    sh "ln -sf /opt/HPE_Security/Fortify_SCA_and_Apps_17.20/bin/sourceanalyzer sourceanalyzer"
    sh "tar -xvzf maven-sca/maven-plugin-bin.tar.gz"
    sh "./mvnw install:install-file -Dfile=pom.xml -DpomFile=maven-sca/pom.xml"
    sh "./mvnw install:install-file -Dfile=xcodebuild/pom.xml -DpomFile=maven-sca/xcodebuild/pom.xml"
    sh "./mvnw install:install-file -Dfile=sca-maven-plugin/sca-maven-plugin-17.20.jar -DpomFile=maven-sca/sca-maven-plugin/pom.xml"
    sh "./sourceanalyzer -b ${env.BUILD_ID} -clean"
    sh "./sourceanalyzer -b ${env.BUILD_ID} ./mvnw target"
    sh "./sourceanalyzer -b ${env.BUILD_ID} -scan -f result.fpr"
  } catch (err) {
      throw err
  }

  stage('Build Docker Image') {
    try {
      app_image = docker.build("uscis-odos/user")
      docker.withRegistry('https://nexus.uscis-fads.local:9443', 'jenkins-nexus-auth') {
        app_image.push(version)
        app_image.push("dev")
      }
    } catch (err) {
      echo "Error encountered: ${err}"
      throw err
    }
  }
  
  stage('Twistlock Scan') {
    try {
      twistlockScan ca: '', cert: '', compliancePolicy: 'high', dockerAddress: 'unix:///var/run/docker.sock', gracePeriodDays: 0, ignoreImageBuildTime: true, image: "uscis-odos/user:${version}", key: '', logLevel: 'true', policy: 'high', requirePackageUpdate: false, timeout: 10
    } catch (err) {
      echo "Error encountered: ${err}"
      //throw err
    }
  }

  stage('Deploy to Dev') {
    openshift.withCluster( 'dev' ) {
      //openshift.verbose()
      try {
        def result = openshift.raw('import-image nexus.uscis-fads.local:9443/uscis-odos/user:dev --insecure=true --confirm')
        echo "Result: ${result.out}"
      } catch ( err ) {
        echo "Error encountered: ${err}"
        throw err
      }
    }
  }

}
