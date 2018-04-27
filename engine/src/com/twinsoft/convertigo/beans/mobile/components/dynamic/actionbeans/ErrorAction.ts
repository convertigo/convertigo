    /**
     * Function ErrorAction
     *   
     * @param page  , the current page
     * @param props , the object which holds properties key-value pairs
     * @param vars  , the object which holds variables key-value pairs
     */
    ErrorAction(page: C8oPageBase, props, vars) : Promise<any> {
        return new Promise((resolve, reject)=> {
            reject(new Error(props.message));
        });
    }