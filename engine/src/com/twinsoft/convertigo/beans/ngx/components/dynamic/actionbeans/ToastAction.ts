    /**
     * Function ToastAction
     *   
     * @param page  , the current page
     * @param props , the object which holds properties key-value pairs
     * @param vars  , the object which holds variables key-value pairs
     */
    ToastAction(page: C8oPageBase, props, vars) : Promise<any> {
        
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
        
        const openToast = async (resolve) => {
            
            let buttons = undefined;
            if (props.showCloseButton) {
                buttons = [
                    {
                        text: props.closeButtonText ? props.closeButtonText : 'Close',
                        role: 'cancel',
                        handler: () => {
                          //console.log('Cancel clicked');
                        }
                    }                           
                ]
            }
            
            let toastController = page.getInstance(ToastController)
            const toast = await toastController.create({
              mode              : props.mode ? props.mode : undefined,
              color             : props.color,
              header            : props.header,
              message           : props.message,
              duration          : props.duration,
              position          : props.position,
              keyboardClose     : props.keyboardClose,
              animated          : props.animated,
              enterAnimation    : props.enterAnimation ? props.enterAnimation : undefined,
              leaveAnimation    : props.leaveAnimation ? props.leaveAnimation : undefined,
              cssClass          : props.cssClass ? props.cssClass : undefined,
              translucent       : props.translucent,
              buttons           : buttons,
              
              id				: props.id ? props.id : undefined,
              icon				: props.icon ? props.icon : undefined,
              htmlAttributes  : props.htmlAttributes ? props.htmlAttributes : undefined
            });

            toast.onDidDismiss().then((data) => {
                page.c8o.log.debug("[MB] Toast dismissed: " + toString(data));
                resolve(data)
            })

            toast.present();
          }

        return new Promise((resolve, reject)=>{
            /*const obj : any = {};
            obj["message"] = props.message;
            obj["duration"] = props.duration;
            obj["position"] = props.position;
            
            if(props.cssClass != null){
                obj["cssClass"] = props.cssClass
            }
            if(props.showCloseButton != null){
                obj["showCloseButton"] = props.showCloseButton
            }
            if(props.closeButtonText != null){
                obj["closeButtonText"] = props.closeButtonText
            }
            if(props.dismissOnPageChange != null){
                obj["dismissOnPageChange"] = props.dismissOnPageChange
            }
            
            let toast = page.routerProvider.toastCtrl.create(obj);
            toast.present();
            toast.onDidDismiss(() => {
                resolve();
              });
            */
            
            Promise.resolve(openToast(resolve))
            .then(() => {
                page.c8o.log.debug("[MB] Toast displayed: " + toString(props.message));
            }).catch((error:any) => {reject(error)})
       })        
}
