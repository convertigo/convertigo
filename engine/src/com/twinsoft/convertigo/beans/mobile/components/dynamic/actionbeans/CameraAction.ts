    /**
     * Function CameraAction
     *   
     * @param page  , the current page
     * @param props , the object which holds properties key-value pairs
     * @param vars  , the object which holds variables key-value pairs
     */
    CameraAction(page: C8oPageBase, props, vars) : Promise<any> {
        const svgClose = '<svg xmlns="http://www.w3.org/2000/svg" class="ionicon" viewBox="0 0 512 512"><path fill="none" stroke="white" stroke-linecap="round" stroke-linejoin="round" stroke-width="32" d="M368 368L144 144M368 144L144 368" /></svg>';
        const svgOpenFolder = "<svg width='25px' xmlns='http://www.w3.org/2000/svg' class='ionicon' viewBox='0 0 512 512'><path d='M64 192v-72a40 40 0 0140-40h75.89a40 40 0 0122.19 6.72l27.84 18.56a40 40 0 0022.19 6.72H408a40 40 0 0140 40v40' fill='none' stroke='white' stroke-linecap='round' stroke-linejoin='round' stroke-width='32'/><path d='M479.9 226.55L463.68 392a40 40 0 01-39.93 40H88.25a40 40 0 01-39.93-40L32.1 226.55A32 32 0 0164 192h384.1a32 32 0 0131.8 34.55z' fill='none' stroke='white' stroke-linecap='round' stroke-linejoin='round' stroke-width='32'/></svg>";
        const svgCheckmark = "<svg xmlns='http://www.w3.org/2000/svg' class='ionicon' viewBox='0 0 512 512'><path fill='none' stroke='white' stroke-linecap='round' stroke-linejoin='round' stroke-width='32' d='M416 128L192 384l-96-96'/></svg>";
        const svgReturn = "<svg xmlns='http://www.w3.org/2000/svg' class='ionicon' viewBox='0 0 512 512'><path fill='none' stroke='white' stroke-linecap='round' stroke-linejoin='round' stroke-width='32' d='M112 352l-64-64 64-64'/><path d='M64 288h294c58.76 0 106-49.33 106-108v-20' fill='none' stroke='white' stroke-linecap='round' stroke-linejoin='round' stroke-width='32'/></svg>";
        const svgCameraRotate = '<svg xmlns="http://www.w3.org/2000/svg" class="ionicon" viewBox="0 0 512 512"><path d="M350.54 148.68l-26.62-42.06C318.31 100.08 310.62 96 302 96h-92c-8.62 0-16.31 4.08-21.92 10.62l-26.62 42.06C155.85 155.23 148.62 160 140 160H80a32 32 0 00-32 32v192a32 32 0 0032 32h352a32 32 0 0032-32V192a32 32 0 00-32-32h-59c-8.65 0-16.85-4.77-22.46-11.32z" fill="none" stroke="white" stroke-linecap="round" stroke-linejoin="round" stroke-width="32" /><path fill="none" stroke="white" stroke-linecap="round" stroke-linejoin="round" stroke-width="32" d="M124 158v-22h-24v22M335.76 285.22v-13.31a80 80 0 00-131-61.6M176 258.78v13.31a80 80 0 00130.73 61.8" /><path fill="none" stroke="white" stroke-linecap="round" stroke-linejoin="round" stroke-width="32" d="M196 272l-20-20-20 20M356 272l-20 20-20-20" /></svg>';
        const svgCamera = "<svg xmlns='http://www.w3.org/2000/svg' class='ionicon' viewBox='0 0 512 512'><path d='M350.54 148.68l-26.62-42.06C318.31 100.08 310.62 96 302 96h-92c-8.62 0-16.31 4.08-21.92 10.62l-26.62 42.06C155.85 155.23 148.62 160 140 160H80a32 32 0 00-32 32v192a32 32 0 0032 32h352a32 32 0 0032-32V192a32 32 0 00-32-32h-59c-8.65 0-16.85-4.77-22.46-11.32z' fill='none' stroke='white' stroke-linecap='round' stroke-linejoin='round' stroke-width='32'/><circle cx='256' cy='272' r='80' fill='none' stroke='white' stroke-miterlimit='10' stroke-width='32'/><path fill='none' stroke='white' stroke-linecap='round' stroke-linejoin='round' stroke-width='32' d='M124 158v-22h-24v22'/></svg>";;
        const svgFlashOn = "<svg xmlns='http://www.w3.org/2000/svg' class='ionicon' viewBox='0 0 512 512'><path d='M315.27 33L96 304h128l-31.51 173.23a2.36 2.36 0 002.33 2.77h0a2.36 2.36 0 001.89-.95L416 208H288l31.66-173.25a2.45 2.45 0 00-2.44-2.75h0a2.42 2.42 0 00-1.95 1z' fill='none' stroke='white' stroke-linecap='round' stroke-linejoin='round' stroke-width='32'/></svg>";
        const svgFlashOff = "<svg xmlns='http://www.w3.org/2000/svg' class='ionicon' viewBox='0 0 512 512'><path fill='white' d='M432 448a15.92 15.92 0 01-11.31-4.69l-352-352a16 16 0 0122.62-22.62l352 352A16 16 0 01432 448zM294.34 84.28l-22.08 120.84a16 16 0 006.17 15.71 16.49 16.49 0 009.93 3.17h94.12l-38.37 47.42a4 4 0 00.28 5.34l17.07 17.07a4 4 0 005.94-.31l60.8-75.16a16.37 16.37 0 003.3-14.36 16 16 0 00-15.5-12H307.19L335.4 37.63c.05-.3.1-.59.13-.89A18.45 18.45 0 00302.73 23l-92.58 114.46a4 4 0 00.28 5.35l17.07 17.06a4 4 0 005.94-.31zM217.78 427.57l22-120.71a16 16 0 00-6.19-15.7 16.54 16.54 0 00-9.92-3.16h-94.1l38.36-47.42a4 4 0 00-.28-5.34l-17.07-17.07a4 4 0 00-5.93.31L83.8 293.64A16.37 16.37 0 0080.5 308 16 16 0 0096 320h108.83l-28.09 154.36v.11a18.37 18.37 0 0032.5 14.53l92.61-114.46a4 4 0 00-.28-5.35l-17.07-17.06a4 4 0 00-5.94.31z'/></svg>"
        var HIGHEST_POSSIBLE_Z_INDEX = '2147483647';
        var flashMode = false;
        var isABoringBrowser = /constructor/i.test(window.HTMLElement) || (function (p) { return p.toString() === "[object SafariRemoteNotification]"; })(!window['safari'] || (typeof safari !== 'undefined' && window['safari'].pushNotification));
        var btLeft, btMiddle, btRight, btSet, parent, facingMode, btUpright, btSetUp;
        var merge = (d, s) => {
            var k, v;
            for (k in s) {
                v = s[k];
                if (typeof v == 'object') {
                    merge(d[k], v);
                } else {
                    d[k] = v;
                }
            }
            return d;
        };
        
        var initFrame = () => {
            var btStyle = {
                    "background": "transparent",
                    width: '4em',
                    "padding": 0,
                    
                };
            
            btLeft = document.createElement('button');
            btMiddle = document.createElement('button');
            btRight = document.createElement('button');
            btUpright = document.createElement('button');
            btSet = document.createElement('div');
            btSetUp = document.createElement('div');
            parent = document.createElement('div');
            
            merge(parent, {
                className: 'cordova-camera-capture',
                style: {
                    position: 'relative',
                    zIndex: HIGHEST_POSSIBLE_Z_INDEX,
                    width: '100%',
                    height: '100%',
                    'background-color': 'black'
                }
            });
            
            merge(btSet.style, {
                position: 'absolute',
                bottom: '0',
                width: '100%',
                'text-align': 'center',
                "background-color":"#00000040"
            });
            merge(btSetUp.style, {
                position: 'absolute',
                top: '0',
                width: '100%',
                'text-align': 'center',
                "background-color":"transparent"
            });
            
            merge(btLeft.style, btStyle);
            btLeft.style.float = 'left';
            btSet.appendChild(btLeft);
            
            merge(btMiddle.style, btStyle);
            btSet.appendChild(btMiddle);
            
            btRight.innerHTML = svgClose;
            merge(btRight.style, btStyle);
            btRight.style.float = 'right';
            btSet.appendChild(btRight);
            
            
            
            merge(btUpright.style, btStyle);
            btUpright.style.float = 'right';
            btUpright.style.visibility = 'hidden';
            parent.appendChild(btSet);
            btSetUp.appendChild(btUpright);
            parent.appendChild(btSetUp);
            
        };
        
        var resize = (img, imgWidth, imgHeight, opts) => {
            var targetWidth = opts['targetWidth'];
            var targetHeight = opts['targetHeight'];
            var canvas = document.createElement('canvas');
            var ratio = imgWidth * 1.0 / imgHeight;
            
            if (targetWidth != undefined && targetHeight != undefined) {
                var tratio = targetWidth * 1.0 / targetHeight;
                
                if (tratio > ratio) {
                    targetWidth = targetHeight * ratio;
                } else {
                    targetHeight = targetWidth / ratio;
                }
            } else if (targetWidth != undefined) {
                targetHeight = targetWidth / ratio;
            } else if (targetHeight != undefined) {
                targetWidth = targetHeight * ratio;
            } else {
                targetWidth = imgWidth;
                targetHeight = imgHeight;
            }
            
            canvas.width = targetWidth;
            canvas.height = targetHeight;
            canvas.getContext('2d').drawImage(img, 0, 0, targetWidth, targetHeight);

            // convert image stored in canvas to base64 encoded image
            var imageData = canvas.toDataURL(opts['encodingType'] == 0 ? 'image/jpeg' : 'image/png');
            if (opts['destinationType'] == 0) {
                imageData = imageData.substring(imageData.indexOf(',') + 1);                        
            }
            
            return imageData;
        };
        
        var takePicture = (success, error, opts) => {
            if (opts && opts['sourceType'] === 1) {
                capture(success, error, opts);
            } else {
                selectPicture(success, error, opts);
            }
        };
        
        var selectPicture = (success, error, opts) => {
            initFrame();
            
            var input = document.createElement('input');
            var label = document.createElement('label');
            
            input.setAttribute('id', '' + new Date().getTime());
            label.setAttribute('for', input.getAttribute('id'));
            
            btMiddle.appendChild(label);
            label.innerHTML = svgOpenFolder;
            label.style.display = 'inline-block';
            
            input.style.display = 'none';
            input.type = 'file';
            input.name = 'files[]';
            
            btLeft.style.visibility = 'hidden';
            
            var image = document.createElement('img');
            merge(image.style, {
                width: '100%',
                height: '100%',
                'object-fit': 'contain'
            });
            parent.insertBefore(image, btSet);
            
            input.onchange = (inputEvent: any) => {
                var reader = new FileReader();
                reader.onload = (readerEvent: any) => {
                    btLeft.style.visibility = 'visible';
                    btLeft.appendChild(label);
                    btMiddle.innerHTML = svgCheckmark;
                    image.src = readerEvent.target.result;
                    
                    btMiddle.onclick = () => {
                        var imageData = resize(image, image.naturalWidth, image.naturalHeight, opts);                            
                        parent.parentNode.removeChild(parent);
                        success(imageData);
                    }
                };

                reader.readAsDataURL(inputEvent.target.files[0]);
            };
            
            btRight.onclick = () => {
                parent.parentNode.removeChild(parent);
                error('cancel');
            }
            
            parent.appendChild(input);
            document.body.appendChild(parent);            
        };

        var capture = (success, error, opts) => {
            var localMediaStream, i;            
            var camera = [];
            var camera_i = 0;

            initFrame();
            
            var video = document.createElement('video');
            
            merge(video, {
                autoplay: true,
                muted: true,
                playsinline: true,
                width: -1,
                height: -1,
                style: {
                    width: '100%',
                    height: '100%',
                    "objectFit":"cover",
                }
            });
            
            // for ios safari support (seems to be set only with set attribute...)
            video.setAttribute('webkit-playsinline', 'true');
            video.setAttribute('playsinline', 'true');
            
            parent.insertBefore(video, btSet);
            
            var stop = () => {
                // stop video stream, remove video and button.
                // Note that MediaStream.stop() is deprecated as of Chrome 47.
                if (localMediaStream.stop) {
                    localMediaStream.stop();
                } else {
                    localMediaStream.getTracks().forEach((track) => {
                        track.stop();
                    });
                }
            }
            
            btLeft.onclick = () => {
                if (video.paused) {
                    video.play();
                    successCallback(localMediaStream);
                } else {
                    stop();
                    facingMode =  (facingMode == 'user' ? 'environment' : 'user');
                    if(facingMode == "user"){
                        merge(video.style,{
                            "transform": "rotateY(180deg)",
                            "webkitTransform":"rotateY(180deg)", /* Safari and Chrome */
                            "MozTransform":"rotateY(180deg)", /* Firefox */
                        })
                    }
                    else{
                        merge(video.style,{
                            "transform": "unset",
                            "webkitTransform":"unset", /* Safari and Chrome */
                            "MozTransform":"unset", /* Firefox */
                        })
                    }
                    navigator.mediaDevices.getUserMedia({video: {facingMode: facingMode}, audio: false}).then(successCallback).catch(error);
                    video.play();
                }
            }
            
            btMiddle.onclick = ()=> {
                if (video.paused) {
                    var imageData = resize(video, video.videoWidth, video.videoHeight, opts);
                    stop();
                    parent.parentNode.removeChild(parent);
                    success(imageData);
                } else {
                    btLeft.innerHTML = svgReturn;
                    btLeft.style.visibility = 'visible';
                    btMiddle.innerHTML = svgCheckmark;
                    video.pause();
                }
            }
            
            btRight.onclick = ()=> {
                stop();
                parent.parentNode.removeChild(parent);
                error('cancel');
            }
            
            var navigator: any = window.navigator;
            
            var successCallback = (stream) => {
                if (camera.length > 1 || isABoringBrowser) {
                    btLeft.innerHTML = svgCameraRotate;
                    btLeft.style.visibility = 'visible';
                } else {
                    btLeft.style.visibility = 'hidden';
                }
                btMiddle.innerHTML = svgCamera;
                
                localMediaStream = stream;
                video.srcObject = stream;
                
                if (parent.parentElement == null) {
                    document.body.appendChild(parent);
                }
                if(!isABoringBrowser){
                    const track = stream.getVideoTracks()[0];
                    try{
                        var imageCapture = new ImageCapture(track);

                        imageCapture.getPhotoCapabilities()
                        .then(photoCapabilities => {
                            if(photoCapabilities.fillLightMode != undefined){
                                let arrayFlash = photoCapabilities.fillLightMode.filter((x)=>{return x == "off" || x == "flash"});
                                if(arrayFlash.length == 2){
                                    btUpright.style.visibility = 'visible';
                                    btUpright.innerHTML = svgFlashOff;
                                    btUpright.onclick = ()=>{
                                        flashMode = !flashMode;
                                        if(flashMode){
                                            btUpright.innerHTML = svgFlashOn;
                                        }
                                        else{
                                            btUpright.innerHTML = svgFlashOff;
                                        }
                                        track.applyConstraints({
                                            advanced: [{torch: flashMode}]
                                          });
                                    }
                                }
                                else{
                                    btUpright.style.visibility = 'hidden';  
                                }
                            }
                            else{
                                btUpright.style.visibility = 'hidden';  
                            }
                            
                        })
                        .catch((e)=>{
                                 btUpright.style.visibility = 'hidden';
                        })
                    }
                    catch(e){
                        console.log("error", e);
                    }
                }
                

            };

            if (navigator.mediaDevices) {
                navigator.mediaDevices.enumerateDevices().then((di)=> {
                    var search = opts['cameraDirection'] == 1 ? 'front' : 'back';
                    camera = di.filter(e=>e.kind === "videoinput");
                    // user == "front, environment == back
                    facingMode = opts['cameraDirection'] == 1 ? 'user' : 'environment';
                    if(facingMode == "user"){
                        merge(video.style,{
                            "transform": "rotateY(180deg)",
                            "webkitTransform":"rotateY(180deg)", /* Safari and Chrome */
                            "MozTransform":"rotateY(180deg)", /* Firefox */
                        })
                    }
                    else{
                        merge(video.style,{
                            "transform": "unset",
                            "webkitTransform":"unset", /* Safari and Chrome */
                            "MozTransform":"unset", /* Firefox */
                        })
                    }
                    if (camera.length > 0) {
                        navigator.mediaDevices.getUserMedia({video: {facingMode: facingMode}, audio: false}).then(successCallback).catch(error);
                    } else {
                        console.log('Device does not have camera, switch to the file picker');
                        selectPicture(success, error, opts);
                    }
                });
            } else {
                console.log('Browser does not support camera, switch to the file picker');
                selectPicture(success, error, opts);
            }
        };
        
        return new Promise((resolve, reject)=> {
            const camera : Camera = page.getInstance(Camera);
            const options : CameraOptions = {};
            const cameraPopoverOptions = {};
        
            // Getting parameters
            options["quality"] = props.quality;
            if (props.allowEdit != null) {
                options["allowEdit"] = props.allowEdit;
            }
            
            if (props.correctOrientation != null) {
                options["correctOrientation"] = props.correctOrientation;
            }
            
            if (props.saveToPhotoAlbum != null) {
                options["saveToPhotoAlbum"] = props.saveToPhotoAlbum;
            }
            
            if (props.targetHeight != undefined) {
                options["targetHeight"] = props.targetHeight;
            }
            
            if (props.targetWidth != undefined) {
                options["targetWidth"] = props.targetWidth;
            }
            
            switch(props.cameraDirection) {
                case "BACK":
                    options["cameraDirection"] = 0;
                    break;
                case "FRONT":
                    options["cameraDirection"] = 1;
                    break;
                default:
                    options["cameraDirection"] = 0;
                    break;
            }
            switch(props.destinationType) {
                case "DATA_URL":
                    options["destinationType"] = 0;
                    break;
                case "FILE_URI":
                    options["destinationType"] = 1;
                    break;
                case "NATIVE_URI":
                    options["destinationType"] = 3;
                    break;
               case "FILE_URL":
                    options["destinationType"] = 1;
                    break; 
                default:
                    options["destinationType"] = 1;
                    break;
            }
            switch(props.encodingType) {
                case "JPEG":
                    options["encodingType"] = 0;
                    break;
                case "PNG":
                    options["encodingType"] = 1;
                    break;
                default:
                    options["encodingType"] = 0;
                    break;
            }
            switch(props.mediaType) {
                case "PICTURE":
                    options["mediaType"] = 0;
                    break;
                case "VIDEO":
                    options["mediaType"] = 1;
                    break;
                case "ALLMEDIA":
                    options["mediaType"] = 3;
                    break;
            }
            switch(props.sourceType) {
                case "PHOTOLIBRARY":
                    options["sourceType"] = 0;
                    break;
                case "CAMERA":
                    options["sourceType"] = 1;
                    break;
                case "SAVEDPHOTOALBUM":
                    options["sourceType"] = 2;
                    break;
                default:
                    options["sourceType"] = 1;
                    break;
            }
            //CameraPopoverOptions
            if(props.x != null){
                cameraPopoverOptions["x"] = props.x;
            }
            if(props.y != null){
                cameraPopoverOptions["y"] = props.y;
            }
            if(props.width != null){
                cameraPopoverOptions["width"] = props.width;
            }
            if(props.height != null){
                cameraPopoverOptions["height"] = props.height;
            }
            switch (props.arrowDir) {
                case "ARROW_UP":
                    cameraPopoverOptions["sourceType"] = 1;
                    options["cameraPopoverOptions"] = cameraPopoverOptions;
                    break;
                case "ARROW_DOWN":
                    cameraPopoverOptions["sourceType"] = 2;
                    options["cameraPopoverOptions"] = cameraPopoverOptions;
                    break;
                case "ARROW_LEFT":
                    cameraPopoverOptions["sourceType"] = 4;
                    options["cameraPopoverOptions"] = cameraPopoverOptions;
                    break;
                case "ARROW_RIGHT":
                    cameraPopoverOptions["sourceType"] = 8;
                    options["cameraPopoverOptions"] = cameraPopoverOptions;
                    break;
                case "ARROW_ANY":
                    cameraPopoverOptions["sourceType"] = 15;
                    options["cameraPopoverOptions"] = cameraPopoverOptions;
                    break;
            }
            
            // Action
            camera.getPicture(options)
                .then((imageData) => {
                    page.router.c8o.log.debug("[MB] CameraAction: ", imageData.substring(0, Math.min(150, imageData.length)));
                    if (props.destinationType == "FILE_URL") {
                        resolve(new URL(imageData));
                    } else {
                        resolve(imageData);
                    }
                    
                })
                .catch((e) => {
                    if(e == "cordova_not_available") {
                        var bodyBg = document.body.style['background-color'];
                        document.body.style['background-color'] = 'black';
                        
                        takePicture((picture) =>{
                            document.body.style['background-color'] = bodyBg;
                            resolve(picture);
                        }, (e) => {
                            document.body.style['background-color'] = bodyBg;
                            console.log("ko " + e);
                            reject(e);
                        }, options);
                    } else {
                        page.router.c8o.log.error("[MB] CameraAction: ", e);
                        reject(e); 
                    }
                }
            );
        });
    }