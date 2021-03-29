    /**
     * Function CallFullSyncAction
     *   
     * @param page  , the current page
     * @param props , the object which holds properties key-value pairs
     * @param vars  , the object which holds variables key-value pairs
     */
    CallFullSyncAction(page: C8oPageBase, props, vars) : Promise<any> {
        return new Promise((resolve, reject) => {
            let r:string = props.requestable.substring(props.requestable.indexOf('.')+1);
            let v:string = props.verb;
            let m:string = props.marker;
            let rvm:string = r + '.' + v + (m != '' ? '#':'')+ m;
            let md:boolean = props.noLoading;
        
            let args = [];
            let version:string = props.tplVersion ? props.tplVersion : '';
            let greater: any = typeof page["compare"]!== "undefined" ? page["compare"]("7.6.0.0", version) : version.localeCompare("7.6.0.0");
            if (greater) {
                args.push("fs://" + rvm,C8oCafUtils.merge({},vars),null,500, md)
            } else {
                args.push("fs://" + rvm,C8oCafUtils.merge({},vars),null,500)
            }
            //page.call("fs://" + rvm,C8oCafUtils.merge({},vars),null,500, md)
            page['call'].apply(page, args)
            .then((res:any) => {resolve(res)}).catch((error:any) => {reject(error)})
        });
    }
    