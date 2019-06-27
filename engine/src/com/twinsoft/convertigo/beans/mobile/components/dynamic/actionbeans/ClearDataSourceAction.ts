    /**
     * Function ClearDataSourceAction
     *   
     * @param page  , the current page
     * @param props , the object which holds properties key-value pairs
     * @param vars  , the object which holds variables key-value pairs
     */
    ClearDataSourceAction(page: C8oPageBase, props, vars) : Promise<any> {
        return new Promise((resolve, reject) => {
            page.router.c8o.log.debug("[MB] ClearDataSourceAction: requestables = "+ props.requestables);
            let requestables:string[] = Array.isArray(props.requestables) ? props.requestables : props.requestables.split(",");
            if (requestables.length > 0) {
                page.getInstance(Platform).ready().then(() => {     // We may need the CBL plugin so wait for platform ready.
                    page.c8o.finalizeInit().then(()=>{              // To be sure that FullSync initialized properly on CBL
                        if (page.deleteListen(requestables)) {
                            resolve(true);
                        } else {
                           resolve(false);
                        }
                    })
                });
            } else {
                resolve(false);
            }
        });
    }
