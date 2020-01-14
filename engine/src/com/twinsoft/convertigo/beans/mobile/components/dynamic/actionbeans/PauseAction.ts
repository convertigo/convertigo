    /**
     * Function PauseAction
     *   
     * @param page  , the current page
     * @param props , the object which holds properties key-value pairs
     * @param vars  , the object which holds variables key-value pairs
     */
    PauseAction(page: C8oPageBase, props, vars) : Promise<any> {
        return new Promise((resolve, reject)=> {
            if (props.duration != null && props.duration > 0) {
                setTimeout(() => {resolve()}, props.duration);
            } else {
                resolve()
            }
        });
    }