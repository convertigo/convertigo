    /**
     * Function ifAction
     *   
     * @param page  , the current page
     * @param props , the object which holds properties key-value pairs
     * @param vars  , the object which holds variables key-value pairs
     */
    IfAction(page: C8oPage, props, vars) : Promise<any> {
        return new Promise((resolve, reject) => {
            if (props.condition) {
                resolve(true)
            } else {
                return false;
            }
        })
    }
    