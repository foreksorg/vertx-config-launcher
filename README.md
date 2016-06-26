# Vert.x Config Launcher

This project aims to provide easy way to launch vert.x instances. It just reads config file as Freemarker Template and 
renders it with JVM System Properties. 

Also because we think defining default behaviour for application may result 
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

>Verticle spesific config goes under deploymentOptions/config
this will be reached as JsonObject by Verticle 
when config() called inside the Verticle

>deploymentOptions.config.instances -> spesify how many instance will be deployed

>deploymentOptions.config.ha -> set true if verticle intented to be High Available

>deploymentOptions.config.worker -> If this verticle is doing long running tasks
                                    then this should be true, so that we let WorkerEventPool
                                    to handle these tasks
                                    
>deploymentOptions.config.multiThreaded -> // If worker is true then Verticle may be called from different threads. 
                                              Don't set this true if you don't know what you are doing
                                              in ideal situation each Verticle should be called by only one thread
					

Please refer to documentation for other options 
which are extraClasspath, isolatedClasses, isolationGroup, maxWorkerExecuteTime
multiThreaded, worker, workerPoolName, workerPoolSize
 
>vertxOptions.clustered -> If this field is true than vertx will run clustered so cluster.xml must provide

>vertxOptions.clusterHost -> cluster host name, (OPTIONAL you can pass things like this"${nodeip}")

>vertxOptions.quorumSize -> Untill quorum size satisfied verticle is not gonna be deployed

>vertxOptions.haEnabled -> Set true if Vert.x should be High Available

>vertxOptions.definition -> If vertx is ha than this can be grouped under this key

>vertxOptions.eventLoopPoolSize -> Main Event Loop pool size, should be equal to CPU core size

>vertxOptions.workerPoolSize -> If Vert.x has a lot of long running tasks
                                Then Worker Event Pool should handle those
                                And this can be greater than CPU core size
                                
Other options are addressResolverOptions, blockedThreadCheckInterval, clusterPingInterval, clusterPort
clusterPublicHost, clusterPublicPort, eventBusOptions, internalBlockingPoolSize,
maxEventLoopExecuteTime, maxWorkerExecuteTime, metricsOptions, warningExceptionTime              

```json
{
	"verticles": {
		"com.foreks.feed.tip.filereader.FirstVerticle": {
			"deploymentOptions": {
				"config": {
					
				}
				},
				"instances": 1, 
				"ha": true,
				"worker": false, 
				"multiThreaded": false 
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
		"clustered": true, 
		"clusterHost": "${nodeip}",
		"quorumSize": 1,
		"haEnabled": true, 
		"haGroup": "definition",
		"eventLoopPoolSize": 4,
		"workerPoolSize": 12,
	}
}

```


License
----

MIT



   [DeploymentOptions]: <http://vertx.io/docs/apidocs/io/vertx/core/DeploymentOptions.html>
   [VertxOptions]: <http://vertx.io/docs/apidocs/io/vertx/core/VertxOptions.html>