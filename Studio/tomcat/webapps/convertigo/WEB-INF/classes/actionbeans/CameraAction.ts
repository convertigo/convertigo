    /**
     * Function FileOpenerAction
     *   
     * @param page  , the current page
     * @param props , the object which holds properties key-value pairs
     * @param vars  , the object which holds variables key-value pairs
     */
    CameraAction(page: C8oPage, props, vars) : Promise<any> {
        return new Promise((resolve, reject)=> {
            const camera : Camera = page.getInstance(Camera);
            const options : CameraOptions = {};
            const cameraPopoverOptions = {};
        
            // Getting parameters
            options["quality"] = props.quality;
            if(props.allowEdit != null) {
                options["allowEdit"] = props.allowEdit;
            }
            if(props.correctOrientation != null){
                options["correctOrientation"] = props.correctOrientation;
            }
            
            if(props.saveToPhotoAlbum != null){
                options["saveToPhotoAlbum"] = props.saveToPhotoAlbum;
            }
            if(props.targetHeight != undefined){
                options["targetHeight"] = props.targetHeight;
            }
            if(props.targetHeight != undefined){
                options["targetHeight"] = props.targetHeight;
            }
            switch(props.cameraDirection) {
                case "BACK":
                    options["cameraDirection"] = 0;
                    break;
                case "FRONT":
                    options["cameraDirection"] = 1;
                    break;
                default:
                    options["cameraDirection"] = 0;
                    break;
            }
            switch(props.destinationType) {
                case "DATA_URL ":
                    options["destinationType"] = 0;
                    break;
                case "FILE_URI":
                    options["destinationType"] = 1;
                    break;
                case "NATIVE_URI ":
                    options["destinationType"] = 3;
                    break;
                default:
                    options["destinationType"] = 1;
                    break;
            }
            switch(props.encodingType) {
                case "JPEG ":
                    options["encodingType"] = 0;
                    break;
                case "PNG":
                    options["encodingType"] = 1;
                    break;
                default:
                    options["encodingType"] = 0;
                    break;
            }
            switch(props.mediaType) {
                case "PICTURE ":
                    options["mediaType"] = 0;
                    break;
                case "VIDEO":
                    options["mediaType"] = 1;
                    break;
                case "ALLMEDIA  ":
                    options["mediaType"] = 3;
                    break;
            }
            switch(props.sourceType) {
                case "PHOTOLIBRARY ":
                    options["sourceType"] = 0;
                    break;
                case "CAMERA ":
                    options["sourceType"] = 1;
                    break;
                case "SAVEDPHOTOALBUM ":
                    options["sourceType"] = 2;
                    break;
                default:
                    options["sourceType"] = 1;
                    break;
            }
            //CameraPopoverOptions
            if(props.x != null){
                cameraPopoverOptions["x"] = props.x;
            }
            if(props.y != null){
                cameraPopoverOptions["y"] = props.y;
            }
            if(props.width != null){
                cameraPopoverOptions["width"] = props.width;
            }
            if(props.height != null){
                cameraPopoverOptions["height"] = props.height;
            }
            switch (props.arrowDir) {
                case "ARROW_UP":
                    cameraPopoverOptions["sourceType"] = 1;
                    options["cameraPopoverOptions"] = cameraPopoverOptions;
                    break;
                case "ARROW_DOWN":
                    cameraPopoverOptions["sourceType"] = 2;
                    options["cameraPopoverOptions"] = cameraPopoverOptions;
                    break;
                case "ARROW_LEFT ":
                    cameraPopoverOptions["sourceType"] = 4;
                    options["cameraPopoverOptions"] = cameraPopoverOptions;
                    break;
                case "ARROW_RIGHT ":
                    cameraPopoverOptions["sourceType"] = 8;
                    options["cameraPopoverOptions"] = cameraPopoverOptions;
                    break;
                case "ARROW_ANY ":
                    cameraPopoverOptions["sourceType"] = 15;
                    options["cameraPopoverOptions"] = cameraPopoverOptions;
                    break;
            }
            // Action
            camera.getPicture(options)
                .then((imageData) => {
                    page.router.c8o.log.debug("[MB] CameraAction: ", imageData);
                    resolve(imageData);
                })
                .catch((e) => {
                    if(e == "cordova_not_available"){
                        page.router.c8o.log.debug("[MB] CameraAction :" + e);
                    } 
                    else{
                        page.router.c8o.log.error("[MB] CameraAction: ", e);
                        reject(e); 
                    }
                });
        });
    }