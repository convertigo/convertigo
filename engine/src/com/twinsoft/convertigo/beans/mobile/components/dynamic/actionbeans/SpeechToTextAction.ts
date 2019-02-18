    /**
     * Function SpeechToTextAction
     *   
     * @param page  , the current page
     * @param props , the object which holds properties key-value pairs
     * @param vars  , the object which holds variables key-value pairs
     */
    
    SpeechToTextAction(page: C8oPageBase, props, vars) : Promise<any> {
        
        let options = {
                locale: props['locale'],            // default "en-US"
				language: props['locale'],          // default "en-US"
                matches: props['matches'],          // default 5, on iOS: maximum number of matches
                prompt: props['prompt'],            // default "", Android only
                showPopup: props['showPopup'],      // default true, Android only
                showPartial : props['showPartial']  // default false, IOs|Browser only
        }
        
        
        let speech = function(page: C8oPageBase, resolve, reject) {
            page.router.c8o.log.debug("[MB] SpeechToTextAction: startListening...");
            if (window['cordova'] != undefined) {
                
                if ((options.locale == undefined) || (options.locale == '')) {
                    options.locale = 'en-US';
                }
                if ((options.language == undefined) || (options.language == '')) {
                    options.language = 'en-US';
                }
                if ((options.matches == undefined) || (options.matches == '')) {
                    options.matches = 5;
                }
                
                const plt:Platform = page.getInstance(Platform);
                const stt:SpeechRecognition = page.getInstance(SpeechRecognition);
                
                let queue = [];
                let timer;
                
                // Check availability
                stt.isRecognitionAvailable()
                .then((available: boolean) => {
                    page.router.c8o.log.debug("[MB] SpeechToTextAction, isRecognitionAvailable:"+ available);
                    
                    // Check permission
                    stt.hasPermission()
                    .then((hasPermission: boolean) => {
                        page.router.c8o.log.debug("[MB] SpeechToTextAction, hasPermission:"+ hasPermission);
                        
                        // Request permission
                        if (!hasPermission) {
                            stt.requestPermission()
                              .then(
                                () => {
                                    page.router.c8o.log.debug("[MB] SpeechToTextAction, requestPermission: Acces granted");
                                },
                                () => {
                                    page.router.c8o.log.debug("[MB] SpeechToTextAction, requestPermission: Acces denied");
                                    reject("Acces denied");
                                }
                              )
                        }
                        
                        // Start listening
                        stt.startListening(options)
                        .subscribe(
                          (matches: Array<string>) => {
                              page.router.c8o.log.debug("[MB] SpeechToTextAction, startListening: "+matches);
                              
                              if (options.showPartial && plt.is('ios')) {
                                  if (timer) {
                                      clearTimeout(timer);
                                  }
                                  
                                  queue.push(matches);
                                  
                                  timer = setTimeout(() => {
                                      page.router.c8o.log.debug("[MB] SpeechToTextAction, stopListening...");
                                      stt.stopListening();
                                      
                                      let transcripts = "";
                                      for (var i = 0; i < queue.length; ++i) {
                                          transcripts += queue[i];
                                      }
                                      resolve(transcripts);
                                  }, 3000);
                                  
                              } else {
                                  resolve(matches);
                              }
                          },
                          (onerror) => {
                              page.router.c8o.log.debug("[MB] SpeechToTextAction, startListening: "+onerror);
                              reject(onerror);
                          }
                        )
                    })
                })            
                .catch((e) => {
                    if(e == "cordova_not_available"){
                        if(!props.mockedResponse){
                            props.mockedResponse = "cordova isn't available";
                        }
                        page.router.c8o.log.debug("[MB] SpeechToTextAction: cordova isn't available: using mocked response: " + props.mockedResponse);
                        resolve(props.mockedResponse);
                    } 
                    else{
                        page.router.c8o.log.error("[MB] SpeechToTextAction: ", e);
                        reject(e); 
                    }
                });
                
            } else if ('SpeechRecognition' in window || 'webkitSpeechRecognition' in window) {
                const {webkitSpeechRecognition} = (window as any);
                let recognition = new webkitSpeechRecognition();
                
                if ((options.locale == undefined) || (options.locale == '')) {
                    options.locale = 'en-US';
                }
                if ((options.matches == undefined) || (options.matches == '')) {
                    options.matches = 1;
                }
                
                recognition.lang = options.locale;
                recognition.maxAlternatives = options.matches;
                recognition.continuous = options.showPartial;
                
                let queue = [];
                
                recognition.onstart = (event) => {
                    page.router.c8o.log.debug("[MB] SpeechToTextAction: onstart");
                }
                
                recognition.onend = (event) => {
                    page.router.c8o.log.debug("[MB] SpeechToTextAction: onend");
                    if (recognition.continuous) {
                        let transcripts = "";
                        for (var i = 0; i < queue.length; ++i) {
                            transcripts += queue[i];
                        }
                        resolve(transcripts);
                    }
                }
                
                recognition.onerror = (event) => {
                    page.router.c8o.log.debug("[MB] SpeechToTextAction: onerror: "+ event.error);
                    reject(event.error);
                }
                
                recognition.onresult = (event) => {
                    page.router.c8o.log.debug("[MB] SpeechToTextAction: onresult");
                    let transcripts = "";
                    for (var i = event.resultIndex; i < event.results.length; ++i) {
                      if (event.results[i].isFinal) {
                        transcripts += event.results[i][0].transcript;
                      } else {
                        transcripts += event.results[i][0].transcript;
                      }
                    }
                    page.router.c8o.log.debug("[MB] SpeechToTextAction: "+ transcripts);
                    if (recognition.continuous)
                        queue.push(transcripts);
                    else
                        resolve(transcripts);
                    
                }
                recognition.start();
            } else {
                const error = "SpeechToText unavailable";
                page.router.c8o.log.error("[MB] SpeechToTextAction: "+ error);
                reject(error);
            }
        }
        
        return new Promise((resolve, reject) => {
            speech(page, resolve, reject);
        });
    }

        
