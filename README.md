# Vert.x Config Launcher

This project aims to provide easy way to launch vert.x instances. It just reads config file as Freemarker Template and 
renders it with JVM System Properties. Because we think defining default behaviour for application may result 
inconsistencies at production environment, we did not read default-cluster.xml file by default if application intendet to
launch as clustered. cluster.xml file must be provided explicitly by defining -Dcluster-xml jvm argument.

and launches it.

### build.gradle Config

Set your application's mainClass as launcher's


```groovy
plugins {
	id 'application'
	
}


mainClassName = 'com.foreks.vertx.launcher.VertxConfigLauncher'

/* !!!OPTIONAL!!!
 Place your default jvm args, 
 you can pass JVM Property and use it with your config file 
 check out below clusterHost field for how we use it
*/
applicationDefaultJvmArgs = [
	'-Dnodeip=127.0.0.1',
	'-Dcluster-xml=conf/cluster.xml' // if Vert.x is clustered,
	                                 // Than cluster-xml JVM arg must be provided
	                                 
	'-Dconf=conf/config.json',       // applicaton config file checkout below
	'-Dlog4j.configuration=file:conf/log4j.xml',
	...
	
	
	]

```

Place this dependency inside your build.gradle

```groovy

dependencies {
    
    compile "com.foreks.vertx:vertx-config-launcher:1.0.0"
    
}

```

### conf/config.json

Config file should include VertxOptions and Verticle's DeploymentOptions in following way

```json
{
	"verticles": {
		"com.foreks.feed.tip.filereader.FirstVerticle": {
			"deploymentOptions": {
				"config": {
					//application spesific config here
					//this will be reached as JsonObject by Verticle 
					//when config() called inside the Verticle
				}
				},
				"instances": 1, //spesify how many instance will be deployed
				"ha": true, // set true if verticle intented to be High Available
				"worker": false, // If this verticle is doing long running tasks
				                 // then this should be true, so that we let WorkerEventPool
				                 // to handle these tasks
				"multiThreaded": false // If worker is true then Verticle may be called from different threads. 
                                       // Don't set this true if you don't know what you are doing
                                       // in ideal situation each Verticle should be called by only one thread
                 
                 //Please refer to documentation for other options 
                 //which are extraClasspath, isolatedClasses, isolationGroup, maxWorkerExecuteTime
                 //multiThreaded, worker, workerPoolName, workerPoolSize
		},
		"com.foreks.feed.tip.filereader.SecondVerticle": {
        			"deploymentOptions": {
        				"config": {
        					"fileOpenOptions":{
        						"read":true,
        						"write":false
        					},
        					"filePath":"data/input.log",
        					"address":"tip-file-reader"
        				}
        				},
        				"instances": 1,
        				"ha": true,
        				"worker": false,
        				"multiThreaded": false
        		}
	},
	"vertxOptions": {
		"clustered": true, //If this field is true than vertx will run clustered so cluster.xml must provided
		"clusterHost": "${nodeip}" // cluster host name, (OPTIONAL you can pass things like this"${nodeip}")
		"quorumSize": 1, // Untill quorum size satisfied verticle is not gonna be deployed
		"haEnabled": true, //Set true if Vert.x should be High Available
		"haGroup": "definition", // If vertx is ha than this can be grouped under this key
		"eventLoopPoolSize": 4, // Main Event Loop pool size, should be equal to CPU core size
		"workerPoolSize": 12 // If Vert.x has a lot of long running tasks
		                     // Then Worker Event Pool should handle those
		                     // And this can be greater than CPU core size
		//other options are addressResolverOptions, blockedThreadCheckInterval, clusterPingInterval, clusterPort
		//clusterPublicHost, clusterPublicPort, eventBusOptions, internalBlockingPoolSize,
		//maxEventLoopExecuteTime, maxWorkerExecuteTime, metricsOptions, warningExceptionTime
	}
}

```


License
----

MIT



   [DeploymentOptions]: <http://vertx.io/docs/apidocs/io/vertx/core/DeploymentOptions.html>
   [VertxOptions]: <http://vertx.io/docs/apidocs/io/vertx/core/VertxOptions.html>