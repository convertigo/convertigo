import { platformBrowserDynamic } from '@angular/platform-browser-dynamic';

import { AppModule } from './app.module';

platformBrowserDynamic().bootstrapModule(AppModule)
.then(() => {
    /*=c8o_PwaWorker*/
})
.catch(err => console.log(err));
