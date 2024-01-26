    /**
     * Function CloseLoadingAction
     *   
     * @param page  , the current page
     * @param props , the object which holds properties key-value pairs
     * @param vars  , the object which holds variables key-value pairs
     */
    CloseLoadingAction(page: C8oPageBase, props, vars) : Promise<any> {
        
        function toString(data) {
            if (data) {
                try {
                    return JSON.stringify(data);
                } catch(e) {
                    return data.toString();
                }
            } else {
               return ""; 
            }
        }
        
        const closeLoading = async () => {
            let loadingController = page.getInstance(LoadingController)
            await loadingController.dismiss(props.data);
        }
        
        return new Promise((resolve, reject) => {
            Promise.resolve(closeLoading())
            .then((data) => {
                resolve(data);
            }).catch((error:any) => {
				//reject(error)
				page.c8o.log.warn("[MB] CloseLoadingAction: bypass error " + toString(error));
				resolve();
			})
        });
    }