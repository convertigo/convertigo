
    /**
     * Function TextToSpeechAction
     *   
     * @param page  , the current page
     * @param props , the object which holds properties key-value pairs
     * @param vars  , the object which holds variables key-value pairs
     */
    
    TextToSpeechAction(page: C8oPageBase, props, vars) : Promise<any> {
        return new Promise((resolve, reject) => {
            
            // Check text property
            if (props['text'] === undefined) {
                page.router.c8o.log.error("[MB] TextToSpeechAction: text property must be defined");
                reject("text_undefined");
                return;
            }
            
            // Create option object
            let options:any = {
                    text: props['text'],
                    locale: props['locale'],
                    rate: props['rate'],
                    pitch: props['pitch'],
                    volume: props['volume']
            };
            
            // If we are in device mode
            if (window['cordova'] != undefined) {
                
                const tts:TextToSpeech = page.getInstance(TextToSpeech);
                const globalization:Globalization = page.getInstance(Globalization);
                
                // Set local preference in case of non defined by user
                if ((options.locale == undefined) || (options.locale == '')) {
                    options.locale = 'en-US';
                    globalization.getPreferredLanguage().then(result => {
                        options.locale = result.value.substring(0, 2).toLowerCase();
                    });                
                }
                
                // Do the job
                tts.speak({
                    text: options.text,
                    locale: options.locale,
                    rate: options.rate
                }).then(() => {
                    page.router.c8o.log.debug("[MB] TextToSpeechAction: text spoken: " + props.text);
                    resolve(true);
                })
                .catch((error: any) => {
                    page.router.c8o.log.error("[MB] TextToSpeechAction: ", error);
                    reject(error);
                });  
            } 
            else {
                if ('speechSynthesis' in window) {
                    
                    // Set local preference in case of non defined by user
                    if ((options.locale == undefined) || (options.locale == '')) {
                        options.locale = navigator['language'] || navigator['userLanguage'];
                    }
                    
                    // Cancel the previous speech
                    speechSynthesis.cancel();               
                    var utterance  = new SpeechSynthesisUtterance(options.text);
                    
                   /**
                    * because of the async loading of the languages
                    * we must wait until they are loaded
                    */
                    var waitTimer = setInterval(() => {
                        var voices = window.speechSynthesis.getVoices();

                        if (voices.length !== 0) {
                            /**
                             * voices are there, stop the loop
                             */
                            clearInterval(waitTimer);

                            var loc = null;

                            for (var i=0, len=voices.length; i<len; i++) {
                                var voice = voices[i];

                               /**                            
                                * check for different possible syntaxes
                                * two letters locale i.e fr, en, es etc...
                                * five letters locale i.e fr-FR, en-US, es-ES etc...
                                * assume length > 5 is a voice.name instead of locale
                                */
                                if (options.locale.length > 5) {   // assume it is a voice name 
                                    if (voice.name.indexOf(options.locale) == 0) { 
                                        loc = {index:i, lang:voice.lang};
                                        break;
                                    }
                                } else {
                                    if (voice.lang.indexOf(options.locale) == 0) { 
                                        loc = {index:i, lang:voice.lang};
                                        break;
                                    }
                                } 
                            }
                            
                            if (loc != null) {
                                var voices = window.speechSynthesis.getVoices();
                                
                                utterance.voice = voices[loc.index]; // Note: some voices don't support altering params
                                utterance.lang = loc.lang;
                
                                utterance.volume = options.volume; // 0 to 1
                                utterance.rate = options.rate; // 0.1 to 10
                                utterance.pitch = options.pitch; //0 to 2
                    
                                try {
                                    speechSynthesis.speak(utterance);
                                    page.router.c8o.log.debug("[MB] TextToSpeechAction: text spoken: " + props.text);
                                    resolve(true);
                                } catch(error) {
                                    page.router.c8o.log.error("[MB] TextToSpeechAction: ", error);
                                    reject(error);
                                }
                            } else {
                                const error = "locale_unavailable";
                                page.router.c8o.log.error("[MB] TextToSpeechAction: "+error+ ": " + options.locale);
                                reject(error);
                            }                            
                        }
                    }, 500);
                } else {
                    const error = "textToSpeech_unavailable";
                    page.router.c8o.log.error("[MB] TextToSpeechAction: "+ error);
                    reject(error);
                }
            }
        });
    }
        
