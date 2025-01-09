pipeline {
    agent any
    triggers {
        GenericTrigger(
            genericVariables: [
               [key: 'uuid', value: '$.uuid'],
               [key: 'name', value: '$.target.displayName'],
               [key: 'details', value: '$.details'],
               [key: 'account', value: '$.target.aspects.cloudAspect.businessAccount.displayName'],
               [key: 'currententity', value: '$.currentEntity.displayName'],
               [key: 'newentity', value: '$.newEntity.displayName'],
               [key: 'applicationShortName', value: '$.target.tags["Name"][0]'],
               [key: 'sdgCode', value: '$.target.tags["red-hat-clustertype"][0]']
            ],
            causeString: 'Triggered by webhook',
            token: 'append',  // Optional token for security
            printContributedVariables: true,
            printPostContent: true
        )
    }
    stages {
        stage('Inspect Environment Variables') {
            steps {
                script {
                    // Print all environment variables to inspect
                    echo "All environment variables: ${env}"
                }
            }
        }
        stage('Print JSON Payload') {
            steps {
                script {
                    // Print extracted variables
                    echo "Action UUID: ${env.uuid}"
                    echo "Name: ${env.name}"
                    echo "Account: ${env.account}"
                    echo "Action Details: ${env.details}"
                    echo "Current Entity: ${env.currententity}"
                    echo "New Entity: ${env.newentity}"
                    echo "Application Short Name: ${env.applicationShortName}"
                    echo "SDG Code: ${env.sdgCode}"
                }
            }
        }
        stage('Update GitHub File') {
            steps {
                script {
                    // Define GitHub repository and file details
                    def GITHUB_REPO = 'https://github.com/AkshitaBhatnagar/jenkins-test.git'
                    def BRANCH = 'main'  // Ensure this is the correct branch name
                    
                    // Remove any existing jenkins-test directory to avoid conflicts
                    sh "rm -rf jenkins-test"
                    
                    // Clone the repository (no credentials needed for public repos)
                    sh "git clone ${GITHUB_REPO}"

                    // Change to the repository directory
                    dir('jenkins-test') {
                        def changesMade = false  // Flag to track if changes are made

                        // Check if account is 092835335818
                        if (env.account == '0928353358') {
                            // Path to the tfvars file
                            def tfvarsFilePath = 'test/cedw/etl.tfvars'
                            
                            // Read the content of the tfvars file
                            def tfvarsContent = readFile(tfvarsFilePath)
                            
                            // Create a dynamic pattern to match the instance type based on current entity
                            def instanceTypePattern = /"(master|core|task)_instance_type" = "(.*?)"/

                            // Replace the matched instance types
                            def updatedContent = tfvarsContent.replaceAll(instanceTypePattern) { match ->
                                if (match[2] == env.currententity) {
                                    changesMade = true
                                    return "\"${match[1]}_instance_type\" = \"${env.newentity}\""
                                }
                                return match[0]
                            }

                            // If the content was modified, write it back to the tfvars file
                            if (changesMade) {
                                writeFile(file: tfvarsFilePath, text: updatedContent)
                            }
                        } 

                        // Check if account is 09283533123
                        if (env.account == '092835335818') {
                            // Path to the stack management template file
                            def templateFilePath = 'test/nass/create-stack-management.template'
                            
                            // Read the content of the template file
                            def templateContent = readFile(templateFilePath)
                            
                            // Create a dynamic pattern to match the "InstanceType" based on current entity
                            def instanceTypePattern = /"InstanceType":\s*"([^"]+)"/

                            // Replace the matched instance type with the new entity
                            def updatedTemplateContent = templateContent.replaceAll(instanceTypePattern) { match ->
                                if (match[1] == env.currententity) {
                                    changesMade = true
                                    return "\"InstanceType\": \"${env.newentity}\""
                                }
                                return match[0]
                            }

                            // If the content was modified, write it back to the template file
                            if (changesMade) {
                                writeFile(file: templateFilePath, text: updatedTemplateContent)
                            }
                        }

                        // If any changes were made, commit and push to GitHub
                        if (changesMade) {
                            echo "Changes detected. Committing and pushing to GitHub."
                            sh """
                                git add .
                                git commit -m "Updated InstanceType values in relevant files"
                                git push https://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@github.com/AkshitaBhatnagar/jenkins-test.git ${BRANCH}
                            """
                        } else {
                            echo "No changes to commit."
                        }
                    }
                }
            }
        }
    }
}
