import groovy.transform.Field
// println "utils/util.groovy is being loaded successfully"
@Field
def APPLICATION_CONFIG = [

        "Containers": [ "applicationName": "cedw", "filepath": "test/cedw/etl.tfvars", "type": "terraform" ],
        "Development": [ "applicationName": "nass", "filepath": "test/nass/create-stack-management.template", "type":"cfn" ],
    
]


def UpdateInstance(String GITHUB_USERNAME,String GITHUB_TOKEN,String payload_account,String currententity,String newentity) {
    echo "UpdateInstance method called with ${GITHUB_USERNAME}, ${GITHUB_TOKEN}, ${payload_account}, ${currententity}, ${newentity}"
    def GITHUB_REPO = 'https://github.com/AkshitaBhatnagar/jenkins-test.git'
    // def GITHUB_REPO = 'https://${GITHUB_USERNAME}:${GITHUB_TOKEN}@github.com/jenkins-test.git' 
    def BRANCH = 'main'  // Ensure this is the correct branch name
    def NEWBRANCH = 'turbo_change'
    def COMMIT_MESSAGE = 'Updated Instance type from turbonomic via Jenkins pipeline'
    sh "rm -rf jenkins-test"
    // Clone the repository
    sh "git clone https://${GITHUB_USERNAME}:${GITHUB_TOKEN}@github.com/AkshitaBhatnagar/jenkins-test.git"
    
    dir('jenkins-test') {
        def changesMade = false  // Flag to track if changes are made
        def appConfig = APPLICATION_CONFIG[payload_account]
        if (appConfig) {
            def filePath = appConfig['filepath']
            def fileType = appConfig['type']

            if (fileType == 'terraform') {
                
                // Read the content of the tfvars file
                def tfvarsContent = readFile(filePath)
                
                // pattern to match the instance type based on current entity
                def instanceTypePattern = /"(master|core|task)_instance_type" = "(.*?)"/

                // Replace the matched instance types
                def updatedContent = tfvarsContent.replaceAll(instanceTypePattern) { match ->
                    if (match[2] == currententity) {
                        changesMade = true
                        return "\"${match[1]}_instance_type\" = \"${newentity}\""
                    }
                    return match[0]
                }

                // If the content was modified, write it back to the tfvars file
                if (changesMade) {
                    writeFile(file: filePath, text: updatedContent)
                }
            } 

            if (fileType == 'cfn') {
                
                // Read the content of the template file
                def templateContent = readFile(filePath)
                
                // pattern to match the "InstanceType" based on current entity
                def instanceTypePattern = /"InstanceType":\s*"([^"]+)"/

                // Replace the matched instance type with the new entity
                def updatedTemplateContent = templateContent.replaceAll(instanceTypePattern) { match ->
                    if (match[1] == currententity) {
                        changesMade = true
                        return "\"InstanceType\": \"${newentity}\""
                    }
                    return match[0]
                }

                // If the content was modified, write it back to the template file
                if (changesMade) {
                    writeFile(file: filePath, text: updatedTemplateContent)
                }
            }

            // If any changes were made, commit and push to GitHub
            if (changesMade) {
                echo "Changes detected. Committing and pushing to GitHub."
                sh """
                    git fetch origin
                    git checkout ${BRANCH}
                    git pull origin ${BRANCH}
                    
                    # Check if the turbo_change branch exists, if not, create it
                    # git checkout -b ${NEWBRANCH} || 
                    git checkout ${NEWBRANCH}
                    git pull origin ${NEWBRANCH} --no-rebase
                    git add .
                    git commit -m "${COMMIT_MESSAGE}"
                    git push https://${GITHUB_USERNAME}:${GITHUB_TOKEN}@github.com/AkshitaBhatnagar/jenkins-test.git ${NEWBRANCH}
                """
            } else {
                // No changes, skip commit and push
                echo "No changes detected, skipping commit and push."
            }

        }
    }
}

return this
