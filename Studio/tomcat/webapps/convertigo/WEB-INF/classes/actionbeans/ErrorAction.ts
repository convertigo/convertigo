    /**
     * Function ErrorAction
     *   
     * 
     * @param props , the object which holds properties key-value pairs
     * @param vars  , the object which holds variables key-value pairs
     */
    ErrorAction(props, vars) : Promise<any> {
        return Promise.reject(props.message);
    }