import './theme/variables.scss';
import './theme/ionic.scss';
import './app/app.scss';
import { platformBrowserDynamic } from '@angular/platform-browser-dynamic';
import { AppModule } from './app/app.module';

platformBrowserDynamic().bootstrapModule(AppModule)
.then(() => {
    /*=c8o_PwaWorker*/
})
.catch(err => console.log(err));
