var watch = require('@ionic/app-scripts/dist/watch');
var copy = require('@ionic/app-scripts/dist/copy');
var copyConfig = require('./copy.config');

// this is a custom dictionary to make it easy to extend/override
// provide a name for an entry, it can be anything such as 'srcFiles' or 'copyConfig'
// then provide an object with the paths, options, and callback fields populated per the Chokidar docs
// https://www.npmjs.com/package/chokidar

module.exports = {
  srcFiles: {
    paths: ['{{SRC}}/**/*.(ts|html|s(c|a)ss)'],
    options: { ignored: ['{{SRC}}/**/*.spec.ts', '{{SRC}}/**/*.temp.ts', '{{SRC}}/**/*.e2e.ts', '**/*.DS_Store'] },
    callback: watch.buildUpdate
  },
  copyConfig: copy.copyConfigToWatchConfig()
};
