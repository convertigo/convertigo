    /**
     * Function ToastAction
     *   
     * @param page  , the current page
     * @param props , the object which holds properties key-value pairs
     * @param vars  , the object which holds variables key-value pairs
     */
    ToastAction(page: C8oPageBase, props, vars) : Promise<any> {
        return new Promise((resolve, reject)=>{
            const obj : any = {};
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
            
       })        
}
