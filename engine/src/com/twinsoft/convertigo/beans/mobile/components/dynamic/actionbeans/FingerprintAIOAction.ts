    /**
     * Function FingerprintAIOAction
     *   
     * @param page  , the current page
     * @param props , the object which holds properties key-value pairs
     * @param vars  , the object which holds variables key-value pairs
     */
    FingerprintAIOAction(page: C8oPageBase, props, vars) : Promise<any> {
        return new Promise((resolve, reject) => {
            const fingerprint : FingerprintAIO  = page.getInstance(FingerprintAIO);
            let params : any =  {"clientId": ""};
            if(props.clientID == null) {
                reject("[MB] FingerprintAIOAction: client ID must be set");
            }
            else{
                params["clientId"] = props.clientID;
            }
            if(props.clientSecret != null){
                params["clientSecret"] = props.clientSecret;
            }
            if(props.disableBackup != null){
                params["disableBackup"] = props.disableBackup;
            }
            if(props.localizedFallbackTitle != null){
                params["localizedFallbackTitle"] = props.localizedFallbackTitle;
            }
            if(props.localizedReason != null){
                params["localizedReason"] = props.localizedReason;
            }
            
            fingerprint.isAvailable()
            .then((response)=>{
                fingerprint.show(params)
                .then((result: any) => {
                    page.router.c8o.log.debug("[MB] FingerprintAIOAction: ", result);
                    resolve(result);
                })
                .catch((error: any) => {
                    page.router.c8o.log.error("[MB] FingerprintAIOAction: ", error);
                    reject(error);
                });
            })
            .catch((error)=>{
                page.router.c8o.log.debug("[MB] FingerprintAIOAction: ", error);
                reject(error);
            });             
        });
    }
    
    /**
    "FingerprintAIOAction" : {
    "classname": "com.twinsoft.convertigo.beans.mobile.components.UIDynamicAction",
    "displayName": "FingerprintAIO",
    "label": "Fingerprint AIO",
    "description": "Defines a <i>Fingerprint AIO</i> action component. | Use simple fingerprint authentication.<br/><u>Supported platorm(s):</u> <ul><li>Android</li><li>iOS</li></ul><br/>For more information : <a target='_blank' href='https://ionicframework.com/docs/v3/native/fingerprint-aio/'>Fingerprint AIO</a>",
    "group": "Native Actions",
      "icon16": "fingerprintaction_color_16x16.png",
      "icon32": "fingerprintaction_color_32x32.png",
    "properties": {
      "clientID": {
          "label": "client ID",
          "description": "Key for platform keychain",
          "category": "@fingerprint options",
          "type": "string",
          "value": false
        },
        "clientSecret": {
          "label": "client Secret",
          "description": "Secret password. Only for android",
          "category": "@fingerprint options",
          "type": "string",
          "value": false
        },
        "disableBackup": {
          "label": "disable Backup",
          "description": "Disable 'use backup' option. Only for android (optional)",
          "category": "@fingerprint options",
          "type": "boolean",
          "value": "false",
          "values": ["false", "true"]
        },
        "localizedFallbackTitle": {
          "label": "localized Fallback Title",
          "description": "Title of fallback button. Only for iOS",
          "category": "@fingerprint options",
          "type": "string",
          "value": false
        },
        "localizedReason": {
          "label": "localized Reason",
          "description": "Description in authentication dialogue. Only for iOS",
          "category": "@fingerprint options",
          "type": "string",
          "value": false
        }
        
    },
    "config": {
      "action_ts_imports": [
          {"from":"@ionic-native/fingerprint-aio","components":["FingerprintAIO"]}
      ],
      "module_ts_imports": [
          {"from":"@ionic-native/fingerprint-aio","components":["FingerprintAIO"]}
      ],
      "module_ng_imports": [
      ],
      "module_ng_providers": [
          "FingerprintAIO"
      ],
      "package_dependencies": [
          {"package":"@ionic-native/fingerprint-aio", "version":"4.5.2"}
      ],
      "cordova_plugins": [
        {
          "plugin":"cordova-plugin-fingerprint-aio",
          "version":"1.3.3"
        }
    ]
  }
},
    **/