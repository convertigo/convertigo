    /**
     * Function FileChooserAction
     *   
     * @param page  , the current page
     * @param props , the object which holds properties key-value pairs
     * @param vars  , the object which holds variables key-value pairs
     */
    FileEncryptionAction(page: C8oPageBase, props, vars) : Promise<any> {
        return new Promise((resolve, reject) => {
            const fileEncryption : FileEncryption = page.getInstance(FileEncryption);
            if(props.filePath == undefined && props.secretkey == undefined) {
                reject("[MB] FileEncryptionAction: filePath and secretkey must be defined");
            }
            fileEncryption.encrypt(props.filePath, props.secretKey)
            .then((response)=> {
                page.router.c8o.trace("[MB] FileEncryptionAction: ", response);
                resolve(response);
            })
            .catch((error)=> {
                page.router.c8o.log.error("[MB] FileEncryptionAction: ", error);
               reject(error); 
            });
            
        });
    }
    
    /* ISSUE IONIC see https://github.com/ionic-team/ionic-native/issues/1907
    
    "FileEncryptionAction" : {
    "classname": "com.twinsoft.convertigo.beans.mobile.components.UIDynamicAction",
    "displayName": "File Encryption",
    "label": "FileEncryption",
    "description": "Simple file encryption",
    "group": "File Actions",
    "icon16": "",
    "icon32": "",
    "properties": {
        "filePath": {
          "label": "filePath",
          "description": "The file path",
          "category": "@File parameters",
          "type": "string",
          "value": true
        },
        "secretKey": {
          "label": "secretkey",
          "description": "The secret key",
          "category": "@File parameters",
          "type": "string",
          "value": true
        }
    },
    "config": {
      "action_ts_imports": [
          {"from":"@ionic-native/file-encryption","components":["FileEncryption"]}
      ],
      "module_ts_imports": [
          {"from":"@ionic-native/file-encryption","components":["FileEncryption"]}
      ],
      "module_ng_imports": [
      ],
      "module_ng_providers": [
          "FileEncryption"
      ],
      "package_dependencies": [
          {"package":"@ionic-native/file-encryption", "version":"4.4.2"}
      ],
      "cordova_plugins": [
        {
          "plugin":"cordova-safe",
          "version":"2.0.1"
        }
    ]
  }
},
    
    */