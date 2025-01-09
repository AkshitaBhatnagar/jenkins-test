def turbo_util

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
               [key: 'applicationShortName', value: '$.target.tags["nbn:focalPoint:applicationShortName"][0]'],
               [key: 'sdgCode', value: '$.target.tags["nbn:focalPoint:sdgCode"][0]']
            ],
            causeString: 'Triggered by webhook',
            token: 'myturboconnect',  // Optional token for security
            printContributedVariables: true,
            printPostContent: true
            
        )
    }
    stages {
		stage('Load Util') {
            options {
                timeout(time: 60, unit: 'MINUTES')
                retry(1)
            }
            steps {
                script {
                    echo "Loading Util"
                    turbo_util = load 'Utils/Util.groovy'
                }
            }
        }
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
                    try {
                        withCredentials([usernamePassword(credentialsId: 'turbo_token', usernameVariable: 'GITHUB_USERNAME', passwordVariable: 'GITHUB_TOKEN')]) {
                                            
                        // Call the updateInstance method from the util
                    
                            turbo_util.UpdateInstance(GITHUB_USERNAME, GITHUB_TOKEN, account, currententity, newentity)
                        }
                    } catch (Exception e) {
                        currentBuild.result = 'FAILURE'
                        error "Modification in Git failed: ${e.message}" 
                    }

                }
            }
        }
        
    }
}

