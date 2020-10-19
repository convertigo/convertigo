
// https://www.npmjs.com/package/fs-extra

module.exports = {

    copyAssets :{
        src: ['{{SRC}}/assets/**/*'],
        dest: '../DisplayObjects/mobile/assets'
    },
    copyIndexContent: {
        src: ['{{SRC}}/index.html', '{{SRC}}/env.json'],
        dest: '../DisplayObjects/mobile/'
    },
    copyServiceWorker:{
        src: ['{{SRC}}/service-worker.js'],
        dest: '../DisplayObjects/mobile/'
    },
    copyPolyfills: {
        src: ['{{ROOT}}/node_modules/ionic-angular/polyfills/polyfills.js'],
        dest: '../DisplayObjects/mobile/build/'
    },
    copyFonts :{
        src: ['{{ROOT}}/node_modules/ionicons/dist/fonts/*', '{{ROOT}}/node_modules/ionic-angular/fonts/*'],
        dest: '../DisplayObjects/mobile/assets/fonts'
    },
    copyAnimations :{
        src: ['{{ROOT}}/node_modules/animate.css/animate.min.css'],
        dest: '../DisplayObjects/mobile/assets/css'
    },
    copyFlashUpdate: {
        src: ['../Flashupdate/index-fu.html', '../Flashupdate/flashupdate.js'],
        dest: '../DisplayObjects/mobile/'
    },
    copyManifest: {
        src: ['{{SRC}}/manifest.json'],
        dest: '../DisplayObjects/mobile/'
    },
    copyCdvPlugins:
    {
        src: '{{SRC}}/plugins.txt',
        dest: '../DisplayObjects/mobile/'
    },
    copySwToolbox:
    {
        src: '{{ROOT}}/node_modules/sw-toolbox/sw-toolbox.js',
        dest: '../DisplayObjects/mobile/build/'
    },
    copySwAng:
    {
        src: '{{ROOT}}/node_modules/@angular/service-worker/ngsw-worker.js',
        dest: '../DisplayObjects/mobile/'
    }
};
