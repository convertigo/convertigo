    /**
     * Function CallSequenceAction
     *   
     * 
     * @param props , the object which holds properties key-value pairs
     * @param vars  , the object which holds variables key-value pairs
     */
    CallSequenceAction(props, vars) : Promise<any> {
        return new Promise((resolve, reject) => {
            let r:string = props.requestable; let m:string = props.marker;
            let rm:string = r + (m != '' ? '#':'')+ m;
            this.call(rm,this.merge({__localCache_priority: props.cachePolicy, __localCache_ttl: props.cacheTtl},vars),null,500)
            .then((res:any) => {resolve(res)}).catch((error:any) => {reject(error)})
        });
    }