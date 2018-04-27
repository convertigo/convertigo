    /**
     * Function SetGlobalAction
     *   
     * @param page  , the current page
     * @param props , the object which holds properties key-value pairs
     * @param vars  , the object which holds variables key-value pairs
     */
    SetGlobalAction(page: C8oPageBase, props, vars) : Promise<any> {
        return new Promise((resolve, reject) => {
            page.router.sharedObject[props.Property] = props.Value
            resolve(props.Value)
        })
    }
    