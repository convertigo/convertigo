    /**
     * Function AnimateAction
     *   
     * @param page  , the current page
     * @param props , the object which holds properties key-value pairs
     * @param vars  , the object which holds variables key-value pairs
     */
    AnimateAction(page: C8oPageBase, props, vars) : Promise<any> {
        return new Promise((resolve, reject)=> {
            let animator : AnimationBuilder = page.getInstance(AnimationService).builder();
            if (animator != null) {
                
                let options = {reject: false};
                
                // booleans
                if (props.useVisibility != null) {
                    options['useVisibility'] = props.useVisibility === true ? true:false;
                }
                if (props.disabled != null) {
                    options['disabled'] = props.disabled === true ? true:false;
                }
                if (props.fixed != null) {
                    options['fixed'] = props.fixed === true ? true:false;
                }
                if (props.pin != null) {
                    options['pin'] = props.pin === true ? true:false;
                }
                
                // strings
                if (props.type != null) {
                    options['type'] = props.type;
                }
                if (props.timingFunction != null) {
                    options['timingFunction'] = props.timingFunction;
                }
                if (props.playState != null) {
                    options['playState'] = props.playState;
                }
                if (props.direction != null) {
                    options['direction'] = props.direction;
                }
                if (props.fillMode != null) {
                    options['fillMode'] = props.fillMode;
                }
                
                // numbers
                if (props.iterationCount != null) {
                    options['iterationCount'] = props.iterationCount === '' ? 1 : props.iterationCount;
                }
                if (props.duration != null) {
                    options['duration'] = props.duration === '' ? 1000 : props.duration;
                }
                if (props.delay != null) {
                    options['delay'] = props.delay === '' ? 0 : props.delay;
                }
                
                animator.setOptions(options);
                
                let animatable = props.animatable;
                let animatables = props.animatables;
                let mode = props.mode == null ? "single":props.mode;
                
                if (mode === "single") {
                    if (animatable != null) {
                        animator.animate(animatable).then(() => {
                            resolve(true);
                        });
                    } else {
                        if (animatables != null) {
                            let first = animatables.first;
                            animator.animate(first.nativeElement).then(() => {
                                resolve(true);
                            });
                        } else {
                            page.router.c8o.log.warn("[MB] AnimateAction: Animatable is not defined");
                            resolve(true);
                        }
                    }
                }
                else if (mode === "all") {
                    if (animatables != null) {
                        animatables.forEach(
                            x => {
                                //console.log(x);
                                animator.animate(x.nativeElement).then(() => {
                                    resolve(true);
                                });
                            }
                        );
                    } else {
                        page.router.c8o.log.warn("[MB] AnimateAction: Animatable is not defined");
                        resolve(true);
                    }
                }
            } else {
                reject("Unable to instanciate the AnimationBuilder");
            }
        });
    }