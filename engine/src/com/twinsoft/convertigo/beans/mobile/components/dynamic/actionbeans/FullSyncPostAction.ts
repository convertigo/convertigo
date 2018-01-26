    /**
     * Function FullSyncPostAction
     *   
     * @param page  , the current page
     * @param props , the object which holds properties key-value pairs
     * @param vars  , the object which holds variables key-value pairs
     */
    FullSyncPostAction(page: C8oPage, props, vars) : Promise<any> {
        return new Promise((resolve, reject) => {
            let r:string = props.requestable.substring(props.requestable.indexOf('.')+1);
            let v:string = 'post'
            let rvm:string = r + '.' + v
            let rootKey = props.RootKey
            let policy = props._use_policy
            let id     = props._id
            let group  = props.c8oGrp
            
            delete props.requestable
            delete props.RootKey

            let data = {}
            if (rootKey != undefined) {
                data[rootKey] = page.merge(props, vars)
                delete data[rootKey]._use_policy
                delete data[rootKey]._id
                delete data[rootKey].c8oGrp
                if (group != null)
                    data["c8oGrp"] = group
                    
                data["_use_policy"] = policy
                if(id != null){
                    data["_id"]         = id
                }
                
            } else {
                if(id == null){
                   delete props._id;
                }
                if (group == null) {
                    delete props.c8oGrp
                }
                data = page.merge(props, vars)
            }
           
            page.getInstance(Platform).ready().then(() => {     // We may need the CBL plugin so wait for platform ready.
                page.c8o.finalizeInit().then(()=>{              // To be sure that FullSync initialized properly on CBL
                    page.call("fs://" + rvm, data, null, 500)
                    .then((res:any) => {resolve(res)}).catch((error:any) => {reject(error)})
                })
            });
        });
    }
