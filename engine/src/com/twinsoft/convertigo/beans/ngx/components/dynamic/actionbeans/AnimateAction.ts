    /**
     * Function AnimateAction
     *   
     * @param page  , the current page
     * @param props , the object which holds properties key-value pairs
     * @param vars  , the object which holds variables key-value pairs
     */
    AnimateAction(page: C8oPageBase, props, vars) : Promise<any> {
        
        var getNativeElement = function (ob) {
            let nativeElement;
            if (ob.classList != undefined) {
                nativeElement = ob;
            } else if (ob.nativeElement != undefined) {
                nativeElement = ob.nativeElement;
            } else if (ob._elementRef != undefined) {
                nativeElement = ob._elementRef.nativeElement;
            }
            return nativeElement;
        };
        
		var animate = function(nativeElement:any, props:any, resolve) {
			nativeElement.style.WebkitAnimation = "none";
			nativeElement.style.animation = "none";
			setTimeout(() => {
				//object.style.animation = "name duration timingFunction delay iterationCount direction fillMode playState"
				let s = "";//s = "zoomOutUp 4s 2";
				s += props.type;
				s += props.duration == null ? 			" 0ms" 		:	" "+props.duration+ "ms";
				s += props.timingFunction == null ? 	" ease" 	: 	" "+props.timingFunction;
				s += props.delay == null ? 				" 0ms" 		: 	" "+props.delay+ "ms";
				s += props.iterationCount == null ? 	" 1" 		: 	" "+props.iterationCount;
				s += props.direction == null ? 			" normal" 	: 	" "+props.direction;
				s += props.fillMode == null ? 			" none" 	: 	" "+props.fillMode;
				s += props.playState == null ? 			" running" 	: 	" "+props.playState;
                
				page.router.c8o.log.debug("[MB] AnimateAction: animation is '"+ s + "'");
				
				nativeElement.style.WebkitAnimation = s;
				nativeElement.style.animation = s;
				page.tick();
				
                let delay = props.delay == null ? 0:Number(props.delay);
                let count = props.iterationCount == null ? 1: (isNaN(Number(props.iterationCount)) ? null:Number(props.iterationCount));
                let duration = props.duration == null ? 0:Number(props.duration);
                
                let timeout = count == null ? null: (delay + count*duration);
				if (timeout != null) {
                    setTimeout(() => {
                       resolve(true);
                    }, timeout);
				}
			}, 10);			
        }
                
		return new Promise((resolve, reject)=> {
                let animatable = props.animatable;
                let animatables = props.animatables;
                let mode = props.mode == null ? "single":props.mode;
                
			let delay = props.delay === "" ? 0: props.delay;
			let animation = props.type === "" ? null: props.type;
			
			if (animation) {
                if (mode === "single") {
                    let nativeElement;
                    if (animatable != null) {
                        nativeElement = getNativeElement(animatable);
                    } else if (animatables != null) {
                        nativeElement = getNativeElement(animatables.first);
                    }
                    
                    if (nativeElement != undefined) {
						animate(nativeElement, props, resolve);
                    } else {
                        page.router.c8o.log.warn("[MB] AnimateAction: Animatable is not defined");
                        resolve(true);
                    }
                    
                }
                else if (mode === "all") {
                    if (animatables != null) {
                        animatables.forEach(
                            x => {
                                let nativeElement = getNativeElement(x);
                                if (nativeElement != undefined) {
									animate(nativeElement, props, resolve);
                                } else {
                                    page.router.c8o.log.warn("[MB] AnimateAction: Animatable is not defined");
                                    resolve(true);
                                }
                            }
                        );
                    } else {
                        page.router.c8o.log.warn("[MB] AnimateAction: Animatable is not defined");
                        resolve(true);
                    }
                }
            } else {
				page.router.c8o.log.warn("[MB] AnimateAction: No animation specified");
				resolve(true);
            }
        });
    }