    
    /**
     * Function TextToSpeechAction
     *   
     * @param page  , the current page
     * @param props , the object which holds properties key-value pairs
     * @param vars  , the object which holds variables key-value pairs
     */
    
    findLocaleCode(localeOrName) {
        var voices = window.speechSynthesis.getVoices();
        
        if (voices == null) {
            // give a second chance
            setTimeout(function() { 
                voices = window.speechSynthesis.getVoices();
            }, 250);
        }
        
        if (voices != null) {
            for (var i=0, len=voices.length; i<len; i++) try {
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
            } catch (e) {
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
            
                setTimeout(async function() { 
                    await tts.speak({
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
                }, 0);                               
            } else {
                if ('speechSynthesis' in window) {
                    if ((options.locale == undefined) || (options.locale == '')) {
                        options.locale = navigator['language'] || navigator['userLanguage'];
                    }
                    
                    speechSynthesis.cancel();                    
                    var utterance  = new SpeechSynthesisUtterance(options.text);
                    var voices = window.speechSynthesis.getVoices();
                    
                    var loc = this.findLocaleCode(options.locale);
                    
                    if (loc != null) {
                        utterance.voice = voices[loc.index]; // Note: some voices don't support altering params
                        utterance.lang = loc.lang;
        
                        utterance.volume = options.volume; // 0 to 1
                        utterance.rate = options.rate; // 0.1 to 10
                        utterance.pitch = options.pitch; //0 to 2
            
                        setTimeout(async function() { await speechSynthesis.speak(utterance); }, 0);

                        utterance.onstart = function(e) {
                            // console.log(options.text + " has been spoken");
                        };
                        
                        utterance.onend = function(e) {
                            // console.log(options.text + " has been spoken");
                        };
                    } else {
                        reject("[MB] TextToSpeechAction: cannot find locale");
                    }
                } else {
                    reject("[MB] TextToSpeechAction: browser cannot do TextToSpeech");
                }
            }
        });
    }
        
