    /**
     * Function ShowLoadingAction
     *   
     * @param page  , the current page
     * @param props , the object which holds properties key-value pairs
     * @param vars  , the object which holds variables key-value pairs
     */
    ShowLoadingAction(page: C8oPageBase, props, vars) : Promise<any> {
        
        function toString(data) {
            if (data) {
                try {
                    return JSON.stringify(data);
                } catch(e) {
                    return data.toString();
                }
            } else {
               return "no data"; 
            }
        }
        
        const openLoading = async (resolve) => {
            let loadingController = page.getInstance(LoadingController)
            const loading  = await loadingController.create({
              mode              : props.mode ? props.mode : undefined,
              spinner           : props.spinner,
              message           : props.message,
              duration          : props.duration,
              keyboardClose     : props.keyboardClose,
              showBackdrop      : props.showBackdrop,
              backdropDismiss   : props.backdropDismiss,
              animated          : props.animated,
              enterAnimation    : props.enterAnimation ? props.enterAnimation : undefined,
              leaveAnimation    : props.leaveAnimation ? props.leaveAnimation : undefined,
              cssClass          : props.cssClass ? props.cssClass : undefined,
              translucent       : props.translucent,
              
              id				: props.id ? props.id : undefined,
              htmlAttributes	: props.htmlAttributes ? props.htmlAttributes : undefined
            });

            loading.onDidDismiss().then((data) => {
                page.c8o.log.debug("[MB] Loading  dismissed: " + toString(data));
                resolve(data)
            })

            return await loading.present();
        }
        
        return new Promise((resolve, reject) => {
            /*try {
                if(page.global["_c8o_loaders"] != undefined){
                    resolve();
                }
                else{
                    let content = props.content != undefined ? props.content: "";
                    page.global["_c8o_loaders"] = page.loadingCtrl.create({content: content});
                    page.global["_c8o_loaders"].present();
                    resolve();
                }
            }
            catch(err) {
                reject(err)
            }*/
            
            Promise.resolve(openLoading(resolve))
            .then(() => {
                page.c8o.log.debug("[MB] Loading displayed: " + toString(props.message));
                resolve();
            }).catch((error:any) => {reject(error)})
            
        });
    }