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
            
            let data = {}
        
            let _data = {}
            _data = C8oCafUtils.merge(_data, props)
            _data = C8oCafUtils.merge(_data, vars)
            
            delete _data.requestable
            delete _data.RootKey
            
            if (rootKey != undefined) {
                data[rootKey] = C8oCafUtils.merge({}, _data)
                data["_use_policy"] = policy
                if (group != null) {
                    data["c8oGrp"] = group
                }
                if (id != null) {
                    data["_id"] = id
                }
                delete data[rootKey]._use_policy
                delete data[rootKey]._id
                delete data[rootKey].c8oGrp
                
                delete data[rootKey]["stack"]
                delete data[rootKey]["noLoading"]
                delete data[rootKey]["tplVersion"]
                delete data[rootKey]["actionName"]
                delete data[rootKey]["actionFunction"]
            } else {
                data = C8oCafUtils.merge({}, _data)
                if (id == null) {
                   delete data._id;
                }
                if (group == null) {
                    delete data.c8oGrp
                }
                
                delete data["stack"]
                delete data["noLoading"]
                delete data["tplVersion"]
                delete data["actionName"]
                delete data["actionFunction"]
            }
        
            let md:boolean = props.noLoading;
            
            let args = [];
            let version:string = props.tplVersion ? props.tplVersion : '';
            let greater: any = typeof page["compare"]!== "undefined" ? page["compare"]("7.6.0.0", version) : version.localeCompare("7.6.0.0");
            if (greater) {
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
