    /**
     * Function SpeechToEventAction
     *   
     * @param page  , the current page
     * @param props , the object which holds properties key-value pairs
     * @param vars  , the object which holds variables key-value pairs
     */
   SpeechToEventAction(page: C8oPageBase, props, vars) : Promise<any> {
	   
		let c8oSpeech = (function () {
			function c8oSpeech() {
				this.recognition = null;
				this.listening = false;
			}
			
			c8oSpeech.prototype.isListening = function () {
				return this.listening;
			};
			
			c8oSpeech.prototype.startListening = function(page: C8oPageBase, options, resolve, reject) {
				const plt:Platform = page.getInstance(Platform);
				
				const p = new Promise((_resolve, _reject) => {
					
					let queue = [];
					let timer;
					
					this.recognition.startListening(options)
					.subscribe(
					  (matches: Array<string>) => {
						  // case Android with continuous mode
						  if (options.showPartial && plt.is('android')) {
							  if (timer) {
								  clearTimeout(timer);
							  }
							  
							  page.router.c8o.log.debug("[MB] SpeechToEventAction, got matches: "+matches);
							  queue.length = 0;
							  queue.push(matches[0]);
							  
							  timer = setTimeout(() => {
								  let transcripts = "";
								  for (var i = 0; i < queue.length; ++i) {
									  transcripts += queue[i];
								  }
							      page.getInstance(Events).publish("speechReceived",transcripts);
							     _resolve(true);
							  }, 2000);
						  }
						  // Other cases
						  else {
							  page.router.c8o.log.debug("[MB] SpeechToEventAction, got matches: "+matches);
							  page.getInstance(Events).publish("speechReceived",matches);
							  _resolve(true);
						  }
					  },
					  (onerror) => {
						  page.router.c8o.log.debug("[MB] SpeechToEventAction, error occured: "+onerror);
						  _reject(onerror);
					  }
					)
				});
				
				p.then((res) => {
					// restart listening (Android only)
					if (plt.is('android')) {
						setTimeout(() => {
							if (this.listening) {
								this.startListening(page, options);
							}
						}, 500);
					}
					
					// case start
					if (resolve) {
						this.listening = true;
						resolve(res);
					}
					// case restart
					return true;
				})
				.catch((err) => {
					// case start
					if (reject) {
						reject(err);
					}
					
					// case restart
					if (plt.is('android')) {
						if (err === "No match" || err === "No speech input" || err === "Didn't understand, please try again.") {
							// restart listening (Android only)
							setTimeout(() => {
								if (this.listening) {
									this.startListening(page, options);
								}
							}, 10);
						} else {
							this.listening = false; // end: do not restart
						}
					}
					return false;
				});
			}
			
			c8oSpeech.prototype.start = function(page: C8oPageBase, options, resolve, reject) {
				page.router.c8o.log.debug("[MB] SpeechToEventAction: starting...");
				if (window['cordova'] != undefined) {
					if (this.recognition == null) {
						this.recognition = page.getInstance(SpeechRecognition);
						
						if ((options.locale == undefined) || (options.locale == '')) {
							options.locale = 'en-US';
						}
						if ((options.language == undefined) || (options.language == '')) {
							options.language = 'en-US';
						}
						if ((options.matches == undefined) || (options.matches == '')) {
							options.matches = 5;
						}
						
					}
					
					// Check availability
					this.recognition.isRecognitionAvailable()
					.then((available: boolean) => {
						page.router.c8o.log.debug("[MB] SpeechToEventAction, isRecognitionAvailable:"+ available);
						
						// Check permission
						this.recognition.hasPermission()
						.then((hasPermission: boolean) => {
							page.router.c8o.log.debug("[MB] SpeechToEventAction, hasPermission:"+ hasPermission);
							
							// Request permission
							if (!hasPermission) {
								this.recognition.requestPermission()
								  .then(
									() => {
										page.router.c8o.log.debug("[MB] SpeechToEventAction, requestPermission: Acces granted");
									},
									() => {
										page.router.c8o.log.debug("[MB] SpeechToEventAction, requestPermission: Acces denied");
										reject("Acces denied");
									}
								  )
							}
							
							// Start listening
							this.startListening(page, options, resolve, reject);
						})
					})            
					.catch((e) => {
						this.listening = false;
						if(e == "cordova_not_available"){
							if(!props.mockedResponse){
								props.mockedResponse = "cordova isn't available";
							}
							page.router.c8o.log.debug("[MB] SpeechToEventAction: cordova isn't available: using mocked response: " + props.mockedResponse);
							resolve(props.mockedResponse);
						} 
						else{
							page.router.c8o.log.error("[MB] SpeechToEventAction: ", e);
							reject(e); 
						}
					});
					
				} else if ('SpeechRecognition' in window || 'webkitSpeechRecognition' in window) {
					const {webkitSpeechRecognition} = (window as any);
					if (this.recognition == null) {
						this.recognition = new webkitSpeechRecognition();
						
						if ((options.locale == undefined) || (options.locale == '')) {
							options.locale = 'en-US';
						}
						if ((options.matches == undefined) || (options.matches == '')) {
							options.matches = 1;
						}
						
						this.recognition.lang = options.locale;
						this.recognition.maxAlternatives = options.matches;
						this.recognition.continuous = true;
						
						this.recognition.onstart = (event) => {
							page.router.c8o.log.debug("[MB] SpeechToEventAction: onstart");
							this.listening = true;
						}
						
						this.recognition.onend = (event) => {
							page.router.c8o.log.debug("[MB] SpeechToEventAction: onend");
							this.listening = false;
							resolve(true);
						}
						
						this.recognition.onerror = (event) => {
							page.router.c8o.log.debug("[MB] SpeechToEventAction: onerror: "+ event.error);
							this.listening = false;
							reject(event.error);
						}
						
						this.recognition.onresult = (event) => {
							page.router.c8o.log.debug("[MB] SpeechToEventAction: onresult");
							let transcripts = "";
							for (var i = event.resultIndex; i < event.results.length; ++i) {
							  if (event.results[i].isFinal) {
								transcripts += event.results[i][0].transcript;
							  } else {
								transcripts += event.results[i][0].transcript;
							  }
							}
							page.getInstance(Events).publish("speechReceived",transcripts);
							resolve(true);
						}
					}
					
					this.recognition.start();
				} else {
					const error = "SpeechToEvent unavailable";
					page.router.c8o.log.error("[MB] SpeechToEventAction: "+ error);
					reject(error);
				}
			};
			
			c8oSpeech.prototype.stop = function(page: C8oPageBase, options, resolve, reject) {
				page.router.c8o.log.debug("[MB] SpeechToEventAction: stopping...");
				if (this.recognition != null) {
					if (window['cordova'] != undefined) {
						const plt:Platform = page.getInstance(Platform);
						
						this.listening = false;
						if (plt.is('ios')) {
							this.recognition.stopListening();
						}
					}
					else if ('SpeechRecognition' in window || 'webkitSpeechRecognition' in window) {
						const {webkitSpeechRecognition} = (window as any);
						this.recognition.stop();
					}
				} else {
					const error = "c8oSpeech not initialized";
					page.router.c8o.log.error("[MB] SpeechToEventAction: "+ error);
					reject(error);
				}
			}
			
			return c8oSpeech;
		})();
		
        let options = {
				command: props['command'],			// default "start"
                locale: props['locale'],            // default "en-US"
				language: props['locale'],          // default "en-US"
                matches: props['matches'],			// default 5, on iOS: maximum number of matches
                prompt: "",            				// default "", Android only
                showPopup: false,      				// default true, Android only
                showPartial : true  				// default false, iOS|Browser only
        }
		
        
        return new Promise((resolve, reject) => {
			// Create c8oSpeech if needed (store in sharedObject)
			let _c8oSpeech = page.router.sharedObject["c8oSpeech"];
			if (!_c8oSpeech) {
				_c8oSpeech = new c8oSpeech();
				page.router.sharedObject["c8oSpeech"] = _c8oSpeech;
				page.router.c8o.log.error("[MB] SpeechToEventAction: c8oSpeech created.");
			}
			
			// Run command (start or stop)
			if (options.command === "start") {
				if (_c8oSpeech.isListening()) {
					page.router.c8o.log.error("[MB] SpeechToEventAction: c8oSpeech already started.");
					resolve(true);
				} else {
					_c8oSpeech.start(page, options, resolve, reject);
				}
			} else if (options.command === "stop") {
				if (!_c8oSpeech.isListening()) {
					page.router.c8o.log.error("[MB] SpeechToEventAction: c8oSpeech already stopped.");
					resolve(true);
				} else {
					_c8oSpeech.stop(page, options, resolve, reject);
				}
			} else {
				const error = "Unknown command: "+ options.command;
				page.router.c8o.log.error("[MB] SpeechToEventAction: "+ error);
				reject(error);
			}
        });
    }

        
