    /**
     * Function FileChooserAction
     *   
     * @param page  , the current page
     * @param props , the object which holds properties key-value pairs
     * @param vars  , the object which holds variables key-value pairs
     */
    FileChooserAction(page: C8oPage, props, vars) : Promise<any> {
        return new Promise((resolve, reject) => {
            const fileChooserI : FileChooser = page.getInstance(FileChooser);
            const filePathI : FilePath = page.getInstance(FilePath);
            fileChooserI.open()
            .then((uri) => {
                filePathI.resolveNativePath(uri)
                .then((filepath) => {
                    page.router.c8o.log.debug("[MB] FileChooserAction : filepath is " + filepath);
                    resolve(filepath);
                })
                .catch((e) => {
                    if(e == "cordova_not_available"){
                        page.router.c8o.log.debug("[MB] FileChooserAction: " + e);
                    } 
                    else{
                        page.router.c8o.log.error("[MB] FileChooserAction :", e);
                        reject(e);
                    }
                });
            })
            .catch((e) => {
                if(e == "cordova_not_available"){
                    page.router.c8o.log.debug("[MB] FileChooserAction: " + e);
                } 
                else{
                    page.router.c8o.log.error("[MB] FileChooserAction :", e);
                    reject(e);
                }
            });
        });
    }