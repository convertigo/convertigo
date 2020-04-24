    /**
     * Function FileOpenerAction
     *   
     * @param page  , the current page
     * @param props , the object which holds properties key-value pairs
     * @param vars  , the object which holds variables key-value pairs
     */
    CameraAction(page: C8oPageBase, props, vars) : Promise<any> {
        var HIGHEST_POSSIBLE_Z_INDEX = '2147483647';
        
        var btLeft, btMiddle, btRight, btSet, parent;
        
        var merge = function (d, s) {
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
        
        var initFrame = function () {
            var btStyle = {
                width: '25px',
                height: '25px',
                'border-radius': '100%',
                'margin-left': '2%',
                'margin-right': '2%',
                'font-weight': 'bolder',
                padding: 0,
                border: '#333 solid 2px'
            };
            
            btLeft = document.createElement('button');
            btMiddle = document.createElement('button');
            btRight = document.createElement('button');
            btSet = document.createElement('div');
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
                bottom: '10%',
                width: '100%',
                transform: 'scale(3)',
                'text-align': 'center'
            });
            
            merge(btLeft.style, btStyle);
            btSet.appendChild(btLeft);
            
            merge(btMiddle.style, btStyle);
            btSet.appendChild(btMiddle);
            
            btRight.innerHTML = '❌';
            merge(btRight.style, btStyle);
            btSet.appendChild(btRight);
            
            parent.appendChild(btSet);
        };
        
        var resize = function (img, imgWidth, imgHeight, opts) {
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
        
        var takePicture = function (success, error, opts) {
            if (opts && opts['sourceType'] === 1) {
                capture(success, error, opts);
            } else {
                selectPicture(success, error, opts);
            }
        };
        
        var selectPicture = function (success, error, opts) {
            initFrame();
            
            var input = document.createElement('input');
            var label = document.createElement('label');
            
            input.setAttribute('id', '' + new Date().getTime());
            label.setAttribute('for', input.getAttribute('id'));
            
            btMiddle.appendChild(label);
            label.innerHTML = '📂';
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
            
            input.onchange = function (inputEvent: any) {
                var reader = new FileReader();
                reader.onload = function (readerEvent: any) {
                    btLeft.style.visibility = 'visible';
                    btLeft.appendChild(label);
                    btMiddle.innerHTML = '👍';
                    
                    image.src = readerEvent.target.result;
                    
                    btMiddle.onclick = function () {
                        var imageData = resize(image, image.naturalWidth, image.naturalHeight, opts);                            
                        parent.parentNode.removeChild(parent);
                        success(imageData);
                    }
                };

                reader.readAsDataURL(inputEvent.target.files[0]);
            };
            
            btRight.onclick = function () {
                parent.parentNode.removeChild(parent);
                error('cancel');
            }
            
            parent.appendChild(input);
            document.body.appendChild(parent);            
        };

        var capture = function (success, error, opts) {
            var localMediaStream, i;            
            var camera = [];
            var camera_i = 0;

            initFrame();
            
            var video = document.createElement('video');
            
            merge(video, {
                autoplay: true,
                muted: true,
                width: -1,
                height: -1,
                style: {
                    width: '100%',
                    height: '100%'
                }
            });
            
            parent.insertBefore(video, btSet);
            
            var stop = function() {
                // stop video stream, remove video and button.
                // Note that MediaStream.stop() is deprecated as of Chrome 47.
                if (localMediaStream.stop) {
                    localMediaStream.stop();
                } else {
                    localMediaStream.getTracks().forEach(function (track) {
                        track.stop();
                    });
                }
            }
            
            btLeft.onclick = function () {
                if (video.paused) {
                    video.play();
                    successCallback(localMediaStream);
                } else {
                    camera_i = (camera_i + 1) % camera.length;
                    console.log('Using camera: ' + camera[camera_i].label);
                    stop();
                    navigator.mediaDevices.getUserMedia({video: {deviceId: {exact: camera[camera_i].deviceId}}, audio: false}).then(successCallback).catch(error);
                    video.play();
                }
            }
            
            btMiddle.onclick = function () {
                if (video.paused) {
                    var imageData = resize(video, video.videoWidth, video.videoHeight, opts);
                    stop();
                    parent.parentNode.removeChild(parent);
                    success(imageData);
                } else {
                    btLeft.innerHTML = '↩';
                    btLeft.style.visibility = 'visible';
                    btMiddle.innerHTML = '👍';
                    video.pause();
                }
            }
            
            btRight.onclick = function () {
                stop();
                parent.parentNode.removeChild(parent);
                error('cancel');
            }
            
            var navigator: any = window.navigator;
            
            var successCallback = function (stream) {
                if (camera.length > 1) {
                    btLeft.innerHTML = '♻';
                    btLeft.style.visibility = 'visible';
                } else {
                    btLeft.style.visibility = 'hidden';
                }
                btMiddle.innerHTML = '📷';
                
                localMediaStream = stream;
                video.srcObject = stream;
                
                if (parent.parentElement == null) {
                    document.body.appendChild(parent);
                }
            };

            if (navigator.mediaDevices) {
                navigator.mediaDevices.enumerateDevices().then(function (di) {
                    var search = opts['cameraDirection'] == 1 ? 'front' : 'back';
                    for (i in di) {
                        if (di[i].kind == 'videoinput') {
                            camera.push(di[i]);
                            if (di[i].label.toLowerCase().indexOf(search) != -1) {
                                camera_i = camera.length - 1;
                            }
                        }
                    }
                    if (camera.length > 0) {
                        navigator.mediaDevices.getUserMedia({video: {deviceId: {exact: camera[camera_i].deviceId}}, audio: false}).then(successCallback).catch(error);
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
                        
                        takePicture(function (picture) {
                            document.body.style['background-color'] = bodyBg;
                            resolve(picture);
                        }, function (e) {
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