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
            timeout(time: 60, unit: 'MINUTES')  // Timeout set to 60 minutes
            retry(1)                            // Retry once if the stage fails
        }
        steps {
            script {
                echo "Loading Util"
                echo "Current workspace: ${pwd()}"
                echo "Listing files in the workspace:"
                
                // List all files recursively in the workspace to verify file location
                sh 'ls -R'

                
	    // Attempt to load the util.groovy script
	       turbo_util = load 'utils/util.groovy'
	       echo "Loaded turbo_util: ${turbo_util}"
	       echo "Loaded turbo_util: ${turbo_util}"
	       
               echo "Methods in turbo_util: ${turbo_util.metaClass.methods.collect { it.name }}"


	    // Check if the method exists in the loaded object
	       if (turbo_util) {
		   echo "UpdateInstance method exists: ${turbo_util.UpdateInstance != null}"
	    }  else {
		 echo "Failed to load turbo_util correctly"
	    }
	 
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
                    	    echo "UpdateInstance method called with ${GITHUB_USERNAME}, ${GITHUB_TOKEN}, ${payload_account}, ${currententity}, ${newentity}"
                            turbo_util.UpdateInstance(env.GITHUB_USERNAME, env.GITHUB_TOKEN, env.account, env.currententity, env.newentity)
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

