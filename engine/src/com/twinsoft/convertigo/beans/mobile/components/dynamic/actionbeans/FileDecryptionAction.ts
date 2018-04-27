    /**
     * Function FileChooserAction
     *   
     * @param page  , the current page
     * @param props , the object which holds properties key-value pairs
     * @param vars  , the object which holds variables key-value pairs
     */
    FileDecryptionAction(page: C8oPageBase, props, vars) : Promise<any> {
        return new Promise((resolve, reject) => {
            const fileEncryption : FileEncryption = page.getInstance(FileEncryption);
            if(props.filePath == undefined && props.secretkey == undefined) {
                reject("[MB] FileDecryptionAction: filePath and secretkey must be defined");
            }
            fileEncryption.decrypt(props.filePath, props.secretKey)
            .then((response)=> {
                page.router.c8o.debug("[MB] FileDecryptionAction: ", response);
                resolve(response);
            })
            .catch((e)=> {
                if(e == "cordova_not_available"){
                    page.router.c8o.log.debug("[MB] FileDecryptionAction :" + e);
                } 
                else{
                    page.router.c8o.log.error("[MB] FileDecryptionAction: ", e);
                    reject(e); 
                }   
            });
        });
    }
    
    /* ISSUE IONIC see https://github.com/ionic-team/ionic-native/issues/1907
    
   "FileDecryptionAction" : {
      "classname": "com.twinsoft.convertigo.beans.mobile.components.UIDynamicAction",
      "displayName": "File decryption",
      "label": "FileDecryption",
      "description": "Simple file decryption",
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
            "label": "secretKey",
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