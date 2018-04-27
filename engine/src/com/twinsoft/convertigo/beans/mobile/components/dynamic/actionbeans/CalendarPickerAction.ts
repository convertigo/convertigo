    /**
     * Function CalendarPickerAction
     *   
     * @param page  , the current page
     * @param props , the object which holds properties key-value pairs
     * @param vars  , the object which holds variables key-value pairs
     */
    CalendarPickerAction(page: C8oPageBase, props, vars) : Promise<any> {
        return new Promise(( resolve, reject ) => {
            let options: CalendarModalOptions = {}
            for (var key in props) {
                if (props[key]) {
                    options[key] = props[key]
                }
            }
            options["color"] = props["IonColor"]
            
            let modalCtrl = page.getInstance( ModalController )
            let myCalendar = modalCtrl.create( CalendarModal, {
                options: options
            });
            myCalendar.present();
            myCalendar.onDidDismiss(( date: CalendarResult, type: string ) => {
                resolve(date)
            })
        })
    }
    