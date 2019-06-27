    /**
     * Function FullSyncSyncAction
     *   
     * @param page  , the current page
     * @param props , the object which holds properties key-value pairs
     * @param vars  , the object which holds variables key-value pairs
     */
    FullSyncSyncAction(page: C8oPageBase, props, vars) : Promise<any> {
        return new Promise((resolve, reject) => {
            let dir:string = "sync"
            if (props.Direction === "push")  dir = "replicate_push"
            if (props.Direction === "pull")  dir = "replicate_pull"
            let r:string = props.requestable.substring(props.requestable.indexOf('.')+1);
            let v:string = dir;
            let rvm:string = r + '.' + v
            page.getInstance(Platform).ready().then(() => {     // We may need the CBL plugin so wait for platform ready.
                page.c8o.finalizeInit().then(()=>{              // To be sure that FullSync initialized properly on CBL
                    // Is this the first sync on this database ?
                    page.c8o.callJsonObject("fs://" + r + ".get", {
                        docid: "_local/c8o"
                    })
                    .then ((response:any, parameters: any) => {
                        // If this is not a first sync, we do not have to wait for a complete sync before executing
                        // next action, so resolve immediately.
                        page.c8o.log.info("Got a _local/c8o doc ==> Not a first sync, so execute next actions immediately")
                        resolve()
                        return null
                    })
                    .fail((error:any) => {
                        page.c8o.log.info("no _local/c8o doc ==> This is first sync, waiting for full replication before executing next actions...")
                        if (window["cordova"])
                            if (navigator["connection"]["type"] == 'none') {
                                page.c8o.log.error("No network, the app needs the network to initialize at least the first time.")
                                reject("No Network")
                            }
                    })
                    
                    page.c8o.callJsonObject("fs://" + rvm,C8oCafUtils.merge({
                        "continuous": props.Mode == "continuous" ? true:false,
                        "retry": props.Retry == "true" ? true:false,
                        "batch_size": props.BatchSize,
                        "batches_limit": props.BatchesLimit,
                        "heartbeat": props.Mode == "continuous" ? false:10000,
                        "timeout": props.Mode == "continuous" ? 25000:false
                    },vars))
                    .progress((progress: any)=>{
                        page.router.sharedObject.FullSyncSyncAction = {  progress: progress }
                        if(page != undefined && page.didleave == false){
                            page.tick();
                        }
                        return null
                    })
                    .then((response:any, parameters: any) => {
                        // Replication is completed , so mark it as it is.
                        page.c8o.callJsonObject("fs://" + r + ".post", {
                            _id: "_local/c8o",
                            status: "completed",
                            "_use_policy":"override"
                        })
                        .then((resp)=>{
                            return null;
                        })
                        .fail((err)=>{
                            page.c8o.log.error("Failed to override _local/c8o", err); 
                        });
                        resolve(response)
                        return null
                    })
                    .fail((error:any) => {
                        reject(error)
                    })
                    
                })
            })
        });
    }
