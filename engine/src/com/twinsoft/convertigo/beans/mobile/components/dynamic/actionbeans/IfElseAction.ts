    /**
     * Function IfElseAction
     *   
     * @param page  , the current page
     * @param props , the object which holds properties key-value pairs
     * @param vars  , the object which holds variables key-value pairs
     */
    IfElseAction(page: C8oPageBase, props, vars) : Promise<any> {
        return new Promise((resolve, reject) => {
            let ok = (props.condition) ? true : false;
            if (props.negate) {
                ok = !ok;
            }
            if (ok) {
                resolve(true);
            } else {
                resolve(false);
            }
        })
    }
    