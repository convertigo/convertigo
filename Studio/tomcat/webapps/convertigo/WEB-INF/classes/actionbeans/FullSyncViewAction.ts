    /**
     * Function FullSyncViewAction
     *   
     * @param page  , the current page
     * @param props , the object which holds properties key-value pairs
     * @param vars  , the object which holds variables key-value pairs
     */
    FullSyncViewAction(page: C8oPage, props, vars) : Promise<any> {
        return new Promise((resolve, reject) => {
            // fsview is in the form project.database.designdoc.view
            let tokens = props.fsview.split('.')
            
            let r:string = tokens[1]     
            let v:string = "view";
            let m:string = props.marker;
            let rvm:string = r + '.' + v + (m != '' ? '#':'')+ m;
            let ddoc:string = tokens[2]
            let view:string = tokens[3]
            
            let options = {ddoc:"", view:""}
            
            for (let key in props) {
                if (key != "marker" && key!= "fsview")
                    if (props[key] != null) {
                        if (props[key] == "true") 
                            options[key] = true
                        else if (props[key] == "false") 
                            options[key] = false
                        else
                            options[key] = props[key]
                    }
            }
            
            options.ddoc = ddoc; options.view = view
            
            page.getInstance(Platform).ready().then(() => {     // We may need the CBL plugin so wait for platform ready.
                page.c8o.finalizeInit().then(()=>{              // To be sure that FullSync initialized properly on CBL
                    page.call("fs://" + rvm, options, null, 500)
                    .then((res:any) => {resolve(res)}).catch((error:any) => {reject(error)})
                })
            });
        });
    }
