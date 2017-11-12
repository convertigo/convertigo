    /**
     * Function FullSyncSyncAction
     *   
     * @param page  , the current page
     * @param props , the object which holds properties key-value pairs
     * @param vars  , the object which holds variables key-value pairs
     */
    FullSyncSyncAction(page: C8oPage, props, vars) : Promise<any> {
        return new Promise((resolve, reject) => {
            let dir:string = "sync"
            if (props.Direction === "push")  dir = "replicate_push"
            if (props.Direction === "pull")  dir = "replicate_pull"
            let r:string = props.requestable.substring(props.requestable.indexOf('.')+1);
            let v:string = dir;
            let rvm:string = r + '.' + v
            page.getInstance(Platform).ready().then(() => {     // We may need the CBL plugin so wait for platform ready.
                page.c8o.finalizeInit().then(()=>{              // To be sure that FullSync initialized properly on CBL
                    page.c8o.callJsonObject("fs://" + rvm,page.merge({
                        "continuous": props.Mode == "continuous" ? true:false,
                        "retry": props.Retry == "true" ? true:false,
                        "batch_size": props.BatchSize,
                        "batches_limit": props.BatchesLimit
                    },vars))
                    .progress((progress: any)=>{
                        page.router.sharedObject.FullSyncSyncAction = {  progress: progress }
                        if(page != undefined && page.didleave == false){
                            page.tick();
                        }
                        return null
                    })
                    .then((response:any, parameters: any) => {
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
