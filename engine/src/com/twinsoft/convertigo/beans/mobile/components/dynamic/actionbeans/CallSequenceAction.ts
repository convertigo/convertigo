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
            page.call(rm,C8oCafUtils.merge({__localCache_priority: props.cachePolicy, __localCache_ttl: props.cacheTtl},vars),null,td, md)
            .then((res:any) => {resolve(res)}).catch((error:any) => {reject(error)})
        });
    }