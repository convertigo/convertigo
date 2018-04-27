    /**
     * Function logAction
     *   
     * @param page  , the current page
     * @param props , the object which holds properties key-value pairs
     * @param vars  , the object which holds variables key-value pairs
     */
    LogAction(page: C8oPageBase, props, vars) : Promise<any> {
        page.c8o.log[props.level](props.message);
        return Promise.resolve();
    }