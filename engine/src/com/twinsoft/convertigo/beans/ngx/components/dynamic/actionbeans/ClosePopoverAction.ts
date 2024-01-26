	/*
     * @param page  , the current page
     * @param props , the object which holds properties key-value pairs
     * @param vars  , the object which holds variables key-value pairs
     */
    ClosePopoverAction(page: C8oPageBase, props, vars) : Promise<any> {
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
        
        const closePopover = async () => {
            let popoverCtrl = page.getInstance(PopoverController)
            await popoverCtrl.dismiss(props.data);
        }
        
        return new Promise((resolve, reject) => {
            Promise.resolve(closePopover())
            .then((data) => {
                resolve(data);
            }).catch((error:any) => {
				//reject(error)
				page.c8o.log.warn("[MB] ClosePopoverAction: bypass error " + toString(error));
				resolve();
			})
      	});
    }
