    /**
     * Function SMSAction
     *   
     * @param page  , the current page
     * @param props , the object which holds properties key-value pairs
     * @param vars  , the object which holds variables key-value pairs
     */
    SMSAction(page: C8oPageBase, props, vars) : Promise<any> {
        return new Promise((resolve, reject)=> {
            const sms : SMS = page.getInstance(SMS);
            let options: any = {android:{}};
            if(props.replaceLineBreaks != null){
                options.replaceLineBreaks = props.replaceLineBreaks;
            }
            if(props.intent != null){
                props.intent == true ? options.android.intent = 'INTENT' : null; 
                
            }
            sms.send(props.phoneNumber, props.message)
            .then((buttonIndex: number) => {
                page.router.c8o.log.debug("[MB] SMSAction: " + buttonIndex);
                resolve(buttonIndex);
            })
            .catch((err)=>{
                page.router.c8o.log.error("[MB] SMSAction: ", err);
                reject(err);
            })
        });
    }
    
    /**
    "SMSAction" : {
    "classname": "com.twinsoft.convertigo.beans.mobile.components.UIDynamicAction",
    "displayName": "SMS",
    "label": "SMS",
    "description": "Defines an <i>SMS</i> action component. | The SMS Action allows you to send sms.<br/><u>Supported platorm(s):</u> <ul><li>Android</li><li>iOS</li><li>Windows Phone 8</li></ul><br/>For more information : <a target='_blank' href='https://ionicframework.com/docs/v3/native/sms/'>SMS</a>",
    "group": "Native Actions",
      "icon16": "smsaction_color_16x16.png",
      "icon32": "smsaction_color_32x32.png",
    "properties": {
      "phoneNumber": {
          "label": "phone Number",
          "description": "The phone number to send sms",
          "category": "@options",
          "type": "string",
          "value": false
        },
        "message": {
          "label": "message",
          "description": "The message to send",
          "category": "@options",
          "type": "string",
          "value": false
        },
        "replaceLineBreaks": {
          "label": "replace Line Breaks",
          "description": "Set to true to replace \n by a new line. Default: false",
          "category": "@sms options",
          "type": "boolean",
          "value": "false",
          
        },
        "intent": {
          "label": "intent",
          "description": "Set to true to send SMS with the native android SMS messaging. Set to false empty will send the SMS without opening any app.",
          "category": "@android options",
          "type": "boolean",
          "value": "false",
          "values": [
              "true",
              "false"
              ]
        }
        
    },
    "config": {
      "action_ts_imports": [
          {"from":"@ionic-native/sms","components":["SMS"]}
      ],
      "module_ts_imports": [
          {"from":"@ionic-native/sms","components":["SMS"]}
      ],
      "module_ng_imports": [
      ],
      "module_ng_providers": [
          "SMS"
      ],
      "package_dependencies": [
          {"package":"@ionic-native/sms", "version":"4.5.2"}
      ],
      "cordova_plugins": [
        {
          "plugin":"cordova-sms-plugin",
          "version":"0.1.11"
        }
    ]
  }
},
    
    *//