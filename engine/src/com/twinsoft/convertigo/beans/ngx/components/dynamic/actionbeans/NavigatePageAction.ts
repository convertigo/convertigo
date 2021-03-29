    /**
     * Function NavigatePageAction
     *   
     * @param page  , the current page
     * @param props , the object which holds properties key-value pairs
     * @param vars  , the object which holds variables key-value pairs
     */
    NavigatePageAction(page: C8oPageBase, props, vars) : Promise<any> {
        return new Promise((resolve, reject) => {
			let url = ""
			let path = props.url ? props.url : ""
			let queryParams = props.data
			let segments = path.split("/")
			segments.forEach(function (segment) {
				let segval = segment
				if (segment.startsWith(":")) {
					let key = segment.substring(1)
					if (props.data[key]) {
						segval = props.data[key]
						delete queryParams[key]
					}
				}
				url = url + "/" + segval
			})

			if (typeof page["angularRouter"] !== "undefined") {
	            page["angularRouter"]["navigate"]([url], { queryParams: queryParams })
	            .then((res:any) => {
	                resolve(res)
	            }).catch((error:any) => {
	                reject(error)
	            })
			} else {
				reject(new Error("angularRouter is undefined"))
			}
        });
    }