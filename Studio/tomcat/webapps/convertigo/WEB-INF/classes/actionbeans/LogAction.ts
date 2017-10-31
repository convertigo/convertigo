    /**
     * Function logAction
     *   
     * @param page  , the current page
     * @param props , the object which holds properties key-value pairs
     * @param vars  , the object which holds variables key-value pairs
     */
    LogAction(page: C8oPage, props, vars) : Promise<any> {
        return Promise.resolve(console.log(props.message));
    }