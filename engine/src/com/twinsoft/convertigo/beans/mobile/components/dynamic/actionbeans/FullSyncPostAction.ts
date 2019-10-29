    /**
     * Function FullSyncPostAction
     *   
     * @param page  , the current page
     * @param props , the object which holds properties key-value pairs
     * @param vars  , the object which holds variables key-value pairs
     */
    FullSyncPostAction(page: C8oPageBase, props, vars) : Promise<any> {
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
                data[rootKey] = C8oCafUtils.merge(props, vars)
                delete data[rootKey]._use_policy
                delete data[rootKey]._id
                delete data[rootKey].c8oGrp
                delete data[rootKey]["noLoading"]
                delete data[rootKey]["tplVersion"]
                if (group != null) {
                    data["c8oGrp"] = group
                }
                data["_use_policy"] = policy
                if (id != null) {
                    data["_id"] = id
                }
                
            } else {
                if (id == null) {
                   delete props._id;
                }
                if (group == null) {
                    delete props.c8oGrp
                }
                data = C8oCafUtils.merge(props, vars)
                delete data["noLoading"]
                delete data["tplVersion"]
            }
            let md:boolean = props.noLoading;
            
            let args = [];
            let version:string = props.tplVersion ? props.tplVersion : '';
            if (version.localeCompare("7.6.0.0") >= 0) {
                args.push("fs://" + rvm, data, null, 500, md)
            } else {
                args.push("fs://" + rvm, data, null, 500)
            }
                
            page.getInstance(Platform).ready().then(() => {     // We may need the CBL plugin so wait for platform ready.
                page.c8o.finalizeInit().then(()=>{              // To be sure that FullSync initialized properly on CBL
                    //page.call("fs://" + rvm, data, null, 500, md)
                    page['call'].apply(page, args)
                    .then((res:any) => {resolve(res)}).catch((error:any) => {reject(error)})
                })
            });
        });
    }
