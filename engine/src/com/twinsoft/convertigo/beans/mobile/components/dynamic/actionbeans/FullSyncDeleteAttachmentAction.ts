    /**
     * Function FullSyncDeleteAttachmentAction
     *   
     * @param page  , the current page
     * @param props , the object which holds properties key-value pairs
     * @param vars  , the object which holds variables key-value pairs
     */
    FullSyncDeleteAttachmentAction(page: C8oPage, props, vars) : Promise<any> {
        let r:string = props.requestable.substring(props.requestable.indexOf('.')+1);
        let v:string = 'delete_attachment';
        let rvm:string = r + '.' + v;
        const params: Object = {};
        
        params["docid"] = props.docid;
        params["name"] = props.name;
        return new Promise((resolve, reject)=>{
            delete props.requestable
            
            if(props.docid == null){
                reject("[MB] FullSyncDeleteAttachmentAction: Missing property: Document ID, " + props.docid);
            }
            if(props.name == null){
                reject("[MB] FullSyncDeleteAttachmentAction: Missing property: Name ," + props.name);
            }
           
            page.getInstance(Platform).ready().then(() => {     // We may need the CBL plugin so wait for platform ready.
                page.c8o.finalizeInit().then(()=>{              // To be sure that FullSync initialized properly on CBL
                    page.call("fs://" + rvm, params, null, 500)
                    .then((res:any) => {
                        resolve(res)
                    })
                    .catch((error:any) => {
                        reject(error)
                    })
                })
            });
        })
    }
