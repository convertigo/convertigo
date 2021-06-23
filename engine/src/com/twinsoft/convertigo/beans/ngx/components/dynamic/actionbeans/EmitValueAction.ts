    /**
     * Function EmitValueAction
     *   
     * @param page  , the current page
     * @param props , the object which holds properties key-value pairs
     * @param vars  , the object which holds variables key-value pairs
     */
    EmitValueAction(page: C8oPageBase, props, vars) : Promise<any> {
        let q:string = props.compvar; // qname of variable
        let p:string = q.substring(q.lastIndexOf('.')+1);
        let version:string = props.tplVersion ? props.tplVersion : '';
        
		if (page[p] && page[p+"Change"]) {
        	page[p+"Change"].emit(page[p]);
		}
        return Promise.resolve();
    }