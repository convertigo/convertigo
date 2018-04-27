    /**
     * Function PublishEvent
     *   
     * @param page  , the current page
     * @param props , the object which holds properties key-value pairs
     * @param vars  , the object which holds variables key-value pairs
     */
    PublishEventAction(page: C8oPageBase, props, vars) : Promise<any> {
        return new Promise((resolve, reject) => {
            let topic:string = props.topic;
            if (topic == undefined) {
                reject("topic must be set");
            }
            if (topic.length == 0) {
                reject("topic must not be empty");
            }
            
            let data = props.data;
            if (data == undefined) {
                data = {};
            }
            //console.log("publishing: topic:"+topic+", data:"+data);
            page.getInstance(Events).publish(topic,data);
            resolve(true);
        });
    }
