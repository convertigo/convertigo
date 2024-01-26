	/*
     * @param page  , the current page
     * @param props , the object which holds properties key-value pairs
     * @param vars  , the object which holds variables key-value pairs
     */
    CloseModalAction(page: C8oPageBase, props, vars) : Promise<any> {
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
        
        const closeModal = async () => {
            let modalController = page.getInstance(ModalController);
            await modalController.dismiss(props.data);
        }
        
        return new Promise((resolve, reject) => {
            Promise.resolve(closeModal())
            .then((data) => {
                resolve(data);
            }).catch((error:any) => {
				//reject(error)
				page.c8o.log.warn("[MB] CloseModalAction: bypass error " + toString(error));
				resolve();
			})
        });
    }
    