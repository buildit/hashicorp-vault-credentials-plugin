@Library('buildit')

def pomUtil = new pom()
def shellUtil = new shell()
def gitUtil = new git()
def toolsUtil = new tools()

def pomVersion = ""
def artifactId = ""
def branch = "master"
def repositoryUrl = "https://github.com/buildit/hashicorp-vault-credentials-plugin.git"
def gitCredentialsId = "global.github"
def repositoryCredentialsId = "global.bitbucket"

try {
    currentBuild.result = "SUCCESS"

    stage('initialise') {
        node() {

            toolsUtil.configureMaven("maven-3")
            toolsUtil.configureJava("jdk-8")

            git url: repositoryUrl, credentialsId: gitCredentialsId, branch: branch

            pomVersion = pomUtil.version(pwd() + "/pom.xml")
            artifactId = pomUtil.artifactId(pwd() + "/pom.xml")
        }
    }

    stage('create package') {
        node() {
            def commitId = shellUtil.pipe("git rev-parse HEAD")

            sh("mvn clean package")

            withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: gitCredentialsId, usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]) {
                def authenticatedUrl = gitUtil.authenticatedUrl(repositoryUrl, env.USERNAME, env.PASSWORD)
                echo("setting remote to authenticated url : ${authenticatedUrl}")
                sh("git remote set-url origin ${authenticatedUrl} &> /dev/null")
                sh("git tag -af ${pomVersion} -m \"Built version: ${pomVersion}\" ${commitId}")
                sh("git push --tags")
            }
        }
    }

    stage('promote package') {
        node() {
            withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: repositoryCredentialsId, usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]) {
                def credentials = "'${env.USERNAME}':'${env.PASSWORD}'"
                sh("curl -v -u ${credentials} -T target/*.hpi \"https://bintray.com/buildit/maven/${artifactId}/${pomVersion}/${artifactId}-${pomVersion}.hpi?publish=1\"")
            }
        }
    }

    stage('increment version') {
        node() {
            def majorVersion = pomUtil.majorVersion(pwd() + "/pom.xml").toInteger()
            def minorVersion = pomUtil.minorVersion(pwd() + "/pom.xml").toInteger()
            def patchVersion = pomUtil.patchVersion(pwd() + "/pom.xml").toInteger()
            def newVersion = "${majorVersion}.${minorVersion + 1}.0"
            if (patchVersion > 0) {
                newVersion = "${majorVersion}.${minorVersion}.${patchVersion + 1}"
            }

            withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: gitCredentialsId, usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]) {
                sh("mvn versions:set -DnewVersion=${newVersion} versions:commit")
                sh("git add pom.xml")
                sh("git commit -m'Bumping version to ${newVersion}'")
                sh("git push origin")
            }
        }
    }
}
catch (err) {
    echo(err.toString())
    currentBuild.result = "FAILURE"
    node() {
        if(!pomVersion.equals("")){
            withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: gitCredentialsId, usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]) {
                // delete the tag off origin
                sh("git push origin :refs/tags/${pomVersion}")
                sh("git fetch --tags --prune")
            }
        }
    }
    throw err
}
finally {
    stage('cleanup') {
        node() {
            sh("git remote set-url origin ${repositoryUrl} &> /dev/null")
        }
    }
}
