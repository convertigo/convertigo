    /**
     * Function CallSequenceAction
     *   
     * @param page  , the current page
     * @param props , the object which holds properties key-value pairs
     * @param vars  , the object which holds variables key-value pairs
     */
    CallSequenceAction(page: C8oPage, props, vars) : Promise<any> {
        return new Promise((resolve, reject) => {
            let r:string = props.requestable; let m:string = props.marker;
            let rm:string = r + (m != '' ? '#':'')+ m;
            page.call(rm,page.merge({__localCache_priority: props.cachePolicy, __localCache_ttl: props.cacheTtl},vars),null,500)
            .then((res:any) => {resolve(res)}).catch((error:any) => {reject(error)})
        });
    }