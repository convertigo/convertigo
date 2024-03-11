    /**
     * Function IonicActionSheetAction
     *   
     * @param page  , the current page
     * @param props , the object which holds properties key-value pairs
     * @param vars  , the object which holds variables key-value pairs
     */
    IonicActionSheetAction(page: C8oPageBase, props, vars) : Promise<any> {
        
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
        
        const presentSheet = async (resolve) => {
            let num = 0;
            let buttons = [];
            if (props.button1 != null) {
                buttons[num] = {};
                buttons[num]['text'] = props.button1;
                if (props.value1 != null) {
                    buttons[num]['handler'] = () => { 
                        Promise.resolve(props.value1).then((data) => actionSheet.dismiss(data));
                        return false;
                    };
                }
                if (props.cancel1 != null) {
                    buttons[num]['role'] = props.cancel1;
                }
                if (props.css1 != null) {
                    buttons[num]['cssClass'] = props.css1;
                }
                num += 1;
            }
            if (props.button2 != null) {
                buttons[num] = {};
                buttons[num]['text'] = props.button2;
                if (props.value2 != null) {
                    buttons[num]['handler'] = () => { 
                        Promise.resolve(props.value2).then((data) => actionSheet.dismiss(data));
                        return false;
                    };
                }
                if (props.cancel2 != null) {
                    buttons[num]['role'] = props.cancel2;
                }
                if (props.css2 != null) {
                    buttons[num]['cssClass'] = props.css2;
                }
                num +=1;
            }
            if (props.button3 != null) {
                buttons[num] = {};
                buttons[num]['text'] = props.button3;
                if (props.value3 != null) {
                    buttons[num]['handler'] = () => { 
                        Promise.resolve(props.value3).then((data) => actionSheet.dismiss(data));
                        return false;
                    };
                }
                if (props.cancel3 != null) {
                    buttons[num]['role'] = props.cancel3;
                }
                if (props.css3 != null) {
                    buttons[num]['cssClass'] = props.css3;
                }
            }
            
            let actionSheetController = page.getInstance(ActionSheetController)
            const actionSheet = await actionSheetController.create({
                mode              : props.mode ? props.mode : undefined,
                header            : props.header,
                subHeader         : props.subHeader,
                keyboardClose     : props.keyboardClose,
                backdropDismiss   : props.backdropDismiss,
                animated          : props.animated,
                enterAnimation    : props.enterAnimation ? props.enterAnimation : undefined,
                leaveAnimation    : props.leaveAnimation ? props.leaveAnimation : undefined,
                cssClass          : props.cssClass ? props.cssClass : undefined,
                translucent       : props.translucent,
                buttons           : buttons,
                
              	id				  : props.id ? props.id : undefined,
              	htmlAttributes	  : props.htmlAttributes ? props.htmlAttributes : undefined
            });

            actionSheet.onDidDismiss().then((data) => {
                page.c8o.log.debug("[MB] ActionSheet  dismissed: " + toString(data));
                resolve(data)
            })
            
            return await actionSheet.present();
          }
        
        
        return new Promise((resolve, reject)=> {
            Promise.resolve(presentSheet(resolve))
            .then(() => {
                page.c8o.log.debug("[MB] ActionSheet displayed ");
            }).catch((error:any) => {reject(error)})
        });
    }