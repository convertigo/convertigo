    /**
     * Function ToastAction
     *   
     * 
     * @param props , the object which holds properties key-value pairs
     * @param vars  , the object which holds variables key-value pairs
     */
    ToastAction(props, vars) : Promise<any> {
        return this.routerProvider.toastCtrl.create({message: props.message, duration: props.duration, position: props.position}).present();
    }
