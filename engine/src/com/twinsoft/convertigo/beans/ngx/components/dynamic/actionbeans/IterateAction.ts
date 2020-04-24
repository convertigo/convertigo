    /**
     * Function IterateAction
     *   
     * @param page  , the current page
     * @param props , the object which holds properties key-value pairs
     * @param vars  , the object which holds variables key-value pairs
     * @param doLoop, the doLoop callback function
     */
    IterateAction(page: C8oPageBase, props, vars, doLoop) : Promise<any> {
        
        const iterate = async () => {
            let arr = [];
            let obj = props.items;
            if (obj != null && obj !== undefined) {
                if (Array.isArray(obj)) {
                    arr = obj
                } else {
                    arr = Array.from(obj, (v, k) => k)
                }
            }
            for (let i = 0; i < arr.length; i++) {
                try {
                    await doLoop(page, arr[i], i)
                    page.c8o.log.trace("[MB] IterateAction : loop "+ (i+1) + " done")
                } catch (error) {
                    throw new Error(error.message ? error.message : error)
                }
            }
            return "done";
        }
        
        return new Promise((resolve, reject) => {
            Promise.resolve(iterate())
            .then((res:any) => {resolve(res)}).catch((error:any) => {reject(error)})
        });
    }