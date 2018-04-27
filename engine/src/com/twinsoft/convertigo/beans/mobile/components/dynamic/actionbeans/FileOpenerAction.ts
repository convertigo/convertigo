    /**
     * Function FileOpenerAction
     *   
     * @param page  , the current page
     * @param props , the object which holds properties key-value pairs
     * @param vars  , the object which holds variables key-value pairs
     */
    FileOpenerAction(page: C8oPageBase, props, vars) : Promise<any> {
        return new Promise((resolve, reject) => {
            const fileOpener : FileOpener = page.getInstance(FileOpener);
            const path: string = props.filePath;
            if(path == undefined) {
                reject("filePath must be set");
            }
            if(window["cordova"] != undefined){
                window["resolveLocalFileSystemURL"](path, (entry)=> {
                    entry.file((file)=> {
                        fileOpener.open(path, file.type)
                        .then((uri) => {
                            page.router.c8o.log.debug("[MB] FileOpenerAction: File is opened");
                            resolve(true);
                        })
                        .catch((e) =>{
                            page.router.c8o.log.error("[MB] FileOpenerAction: Failed to open file", e);
                            reject(e);
                        });
                    });
                  });
            }
            else{
                page.router.c8o.log.debug("[MB] FileOpenerAction: cordova isn't available: using mocked response: " + props.mockedResponse);
                resolve(props.mockedResponse);
            }
        });
    }