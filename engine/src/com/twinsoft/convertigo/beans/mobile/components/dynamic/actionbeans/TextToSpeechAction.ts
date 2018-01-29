
    /**
     * Function TextToSpeechAction
     *   
     * @param page  , the current page
     * @param props , the object which holds properties key-value pairs
     * @param vars  , the object which holds variables key-value pairs
     */
    
    findLocaleCode(localeOrName) {
        var voices = window.speechSynthesis.getVoices();

        for (var i=0, len=voices.length; i<len; i++) {
            var voice = voices[i];

            console.log(voice.name);

            if (localeOrName.length > 5) {   // assume it is a voice name 
                if (voice.name.indexOf(localeOrName) == 0) { 
                    return {index:i, lang:voice.lang};
                }
            } else {
                if (voice.lang.indexOf(localeOrName) == 0) { 
                    return {index:i, lang:voice.lang};
                }
            } 
        }

        return null;
    };
    
    TextToSpeechAction(page: C8oPage, props, vars) : Promise<any> {
        return new Promise((resolve, reject) => {
            if (props['text'] === undefined) {
                reject("[MB] TextToSpeechAction: text cannot be undefined");
                return;
            }
    
            let options:any = {
                    text: props['text'],
                    locale: props['locale'],
                    rate: props['rate'],
                    pitch: props['pitch'],
                    volume: props['volume']
            };
            
            if (window['cordova'] != undefined) {
                const tts:TextToSpeech = page.getInstance(TextToSpeech);
                const globalization:Globalization = page.getInstance(Globalization);
            
                if ((options.locale == undefined) || (options.locale == '')) {
                    options.locale = 'en-US';
                    globalization.getPreferredLanguage().then(result => {
                        options.locale = result.value.substring(0, 2).toLowerCase();
                    });                
                }
            
                tts.speak({
                    text: options.text,
                    locale: options.locale,
                    rate: options.rate
                }).then((result: any) => {
                    page.router.c8o.log.debug("[MB] TextToSpeechAction: ", result);
                    resolve(result);
                })
                .catch((error: any) => {
                    page.router.c8o.log.error("[MB] TextToSpeechAction: ", error);
                    reject(error);
                });  
            } else {
                if ('speechSynthesis' in window) {
                    if ((options.locale == undefined) || (options.locale == '')) {
                        options.locale = navigator['language'] || navigator['userLanguage'];
                    }
                    
                    speechSynthesis.cancel();                    
                    var utterance  = new SpeechSynthesisUtterance(options.text);
                    
                   /**
                    * because of the async loading of the languages
                    * we must wait until they are loaded
                    */
                    var waitTimer = setInterval(function() {
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
                                    resolve("[MB] TextToSpeechAction: spoke " + options.text);
                                } catch(e) {
                                    page.router.c8o.log.debug("[MB] TextToSpeechAction: ",  e);
                                    reject("[MB] TextToSpeechAction: " + e);
                                }
                            } else {
                                reject("[MB] TextToSpeechAction: cannot find locale");
                            }                            
                        }
                    }, 500);
                } else {
                    reject("[MB] TextToSpeechAction: browser cannot do TextToSpeech");
                }
            }
        });
    }
        
