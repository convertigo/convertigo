    /**
     * Function SwitchAction
     *   
     * @param page  , the current page
     * @param props , the object which holds properties key-value pairs
     * @param vars  , the object which holds variables key-value pairs
     * @param cases,  the case callback functions
     */
    SwitchAction(page: C8oPageBase, props, vars, cases) : Promise<any> {
		
		const pseudoSwitch = async (page, props, vars) => {
			// note: use 'for()' instead of 'forEach()' to able doSwitch to handle thrown error from async fn
			if (cases) {
				let stack = props.stack ? props.stack : {}
				let scope = stack['root'] ? stack['root'].scope : {}
				let expression = props.expression
				let params = vars
				
				let doDefault = true
				
				if (cases[expression]) {
					doDefault = false
					let arr = cases[expression]
					for (let i = 0; i < arr.length; i++) {
						let fn = arr[i]
						await fn(page);
					}
				} else if (expression == true) {
					for (let key of Object.keys(cases)) {
						try {
							let ekey = key.replace('this.', 'page.')
							ekey = ekey.replace(/(params\d+)/, 'scope.$1')
							if (eval(ekey) == true) {
								doDefault = false
								let arr = cases[key]
								for (let i = 0; i < arr.length; i++) {
									let fn = arr[i]
									await fn(page);
								}
								break
							}
						} catch (e) {}
					}
				}
				
				if (doDefault && cases['__default__']) {
					let arr = cases['__default__']
					for (let i = 0; i < arr.length; i++) {
						let fn = arr[i]
						await fn(page);
					}
				}
		   	}
		}
		
        const doSwitch = async () => {
			try {
				await pseudoSwitch(page, props, vars)
        	} catch (error) {
            	throw new Error(error.message ? error.message : error)
        	}
            return "done";
        }

        return new Promise((resolve, reject) => {
            Promise.resolve(doSwitch())
            .then((res:any) => {resolve(res)}).catch((error:any) => {reject(error)})
        });
    }