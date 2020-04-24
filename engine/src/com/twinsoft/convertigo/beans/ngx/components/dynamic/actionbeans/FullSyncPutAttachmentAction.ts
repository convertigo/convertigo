    /**
     * Function FullSyncPutAttachmentAction
     *   
     * @param page  , the current page
     * @param props , the object which holds properties key-value pairs
     * @param vars  , the object which holds variables key-value pairs
     */
    FullSyncPutAttachmentAction(page: C8oPageBase, props, vars) : Promise<any> {
        let r:string = props.requestable.substring(props.requestable.indexOf('.') + 1);
        let v:string = 'put_attachment';
        let rvm:string = r + '.' + v;
        const params: Object = {};
        
        params["docid"] = props.docid;
        params["name"] = props.name;
        params["content_type"] = props.content_type;
        
        return new Promise((resolve, reject) => {
            if (typeof props.content == "string") {
                if (new RegExp('^[a-z]+://').test(props.content)) {
                    props.content = new URL(props.content);
                } else {
                    props.content = props.content.replace(new RegExp("^data:.*?base64,"), "");
                }
            }
            if (props.content instanceof URL) {
                props.content.search = "";
                let file: File = page.getInstance(File);
                let fileName = props.content.href.substring(props.content.href.lastIndexOf("/") + 1);
                let path =  props.content.href.substring(0,props.content.href.lastIndexOf("/") + 1);
                file.readAsArrayBuffer(path, fileName)
                .then((res)=>{
                    params["content"] = new Blob([new Uint8Array(res)], { type: props.content_type });
                   resolve();
                })
            } else {
                params["content"] = props.content;
                resolve();
            }
        }).then(() => {
            return new Promise((resolve, reject) => {
                delete props.requestable

                if(props.docid == null){
                    reject("[MB] FullSyncPutAttchmentAction: Missing property: Document ID, " + props.docid);
                }
                if(props.name == null){
                    reject("[MB] FullSyncPutAttchmentAction: Missing property: Name ," + props.name);
                }
                if(props.content_type == null){
                    reject("[MB] FullSyncPutAttchmentAction: Missing property: Content Type, " + props.content_type);
                }
                if(props.content == null){
                    reject("[MB] FullSyncPutAttchmentAction: Missing property: Content, " + props.content);
                }
                let md:boolean = props.noLoading;
                
                let args = [];
                let version:string = props.tplVersion ? props.tplVersion : '';
                let greater: any = typeof page["compare"]!== "undefined" ? page["compare"]("7.6.0.0", version) : version.localeCompare("7.6.0.0");
                if (greater) {
                    args.push("fs://" + rvm, params, null, 500, md)
                } else {
                    args.push("fs://" + rvm, params, null, 500)
                }
                
                page.getInstance(Platform).ready().then(() => {     // We may need the CBL plugin so wait for platform ready.
                    page.c8o.finalizeInit().then(()=>{              // To be sure that FullSync initialized properly on CBL
                        //page.call("fs://" + rvm, params, null, 500, md)
                        page['call'].apply(page, args)
                        .then((res:any) => {
                            resolve(res)
                            })
                            .catch((error:any) => {
                                reject(error)
                            })
                    })
                });
            })
        })
        
    }
