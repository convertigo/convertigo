    /**
     * Function ToastAction
     *   
     * @param page  , the current page
     * @param props , the object which holds properties key-value pairs
     * @param vars  , the object which holds variables key-value pairs
     */
    ToastAction(page: C8oPage, props, vars) : Promise<any> {
        return page.routerProvider.toastCtrl.create({message: props.message, duration: props.duration, position: props.position}).present();
    }
