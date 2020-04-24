    /**
     * Function CallSequenceAction
     *   
     * @param page  , the current page
     * @param props , the object which holds properties key-value pairs
     * @param vars  , the object which holds variables key-value pairs
     */
    CallSequenceAction(page: C8oPageBase, props, vars) : Promise<any> {
        return new Promise((resolve, reject) => {
            if (props.timeout) {
                setTimeout(() => {
                    reject("Connection timeout. Maybe no connection or network too slow.");
                }, props.timeout);
            }
            let r:string = props.requestable; let m:string = props.marker;
            let rm:string = r + (m != '' ? '#':'')+ m;
            let td:number = props.threshold || 500;
            let md:boolean = props.noLoading;
            let parameters = {__localCache_priority: props.cachePolicy, __localCache_ttl: props.cacheTtl};
            
            let args = [];
            let version:string = props.tplVersion ? props.tplVersion : '';
            let greater: any = typeof page["compare"]!== "undefined" ? page["compare"]("7.6.0.0", version) : version.localeCompare("7.6.0.0");
            if (greater) {
                if (typeof page["compare"]!== "undefined" && page["compare"]("7.7.0.11", version)) {
                    parameters["__disableAutologin"] = props.noAutoLogin;
                }
                args.push(rm,C8oCafUtils.merge(parameters,vars),null,td, md)
            } else {
                args.push(rm,C8oCafUtils.merge(parameters,vars),null,td)
            }
            //page.call(rm,C8oCafUtils.merge({__localCache_priority: props.cachePolicy, __localCache_ttl: props.cacheTtl},vars),null,td, md)
            page['call'].apply(page, args)
            .then((res:any) => {resolve(res)}).catch((error:any) => {reject(error)})
         });
    }