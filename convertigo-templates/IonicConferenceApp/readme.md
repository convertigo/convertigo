# template_Ionic2 for Ionic 3.3.0 #
This convertigo project template can be used to start a Ionic 3.3.0/ Angular 4.1.2 project with Convertigo.

## Prerequisites ##
You must install [node.js V 6.5.0](https://nodejs.org/dist/v6.5.0/node-v6.5.0-x64.msi "Node JS") or later on your workstation. 

## Usage ##
To use this template import it in Convertigo Studio (File->Import->Convertigo->Convertigo Project).

When the project is imported switch to the Project Explorer view tab and explore the structure :

    template_Ionic2
	  	[_private]
		[DisplayObject]
		[ionic]
		[Traces]
		[wsdl]
		[xsd]
		index.html
		readme.md
		template_Ionic2.xml

Your development files are in the [ionic] directory

	ionic
		[src]
			[app]
			[assets]
			[pages]	
			[providers]	
			[theme]	
			app.html
			declaration.d.ts
			env.json
			service-worker.js
		[hooks]
		[plugins]
		[resources]
		[typings]
		config.xml
		package.json
		tsconfig.json
		typing.json


You can develop your app by modifying the **src/app/app.module.ts** teh **src/app/app.component.ts** and the pages under the the **src/pages** directory.

## Installing ionic and Angular modules dependencies ##

The template contains the **package.json** describing all the dependencies need to run the ionic / anular 2 mobile app. To install the dependencies with a command shell go in the /ionic directory and use:

	>npm install

This will install all the needed runtime and development modules for Ionic 2 and Angular 2. If you need more modules just use the standard :

	>npm install <module_name> --save

To add it to you dependencies.

## Running the app in a Web Browser ##
You can test and run the app in a modern web browser such as Chrome. To do that you must use the standard NPM scripts 

	>npm run ionic:serve

This will result to :

	[15:00:18]  ionic-app-scripts 1.3.0 
    [15:00:18]  watch started ...
    [15:00:18]  build dev started ...
    [15:00:18]  clean started ...
    [15:00:18]  clean finished in 4 ms
    [15:00:18]  copy started ...
    [15:00:18]  transpile started ...
    [15:00:23]  transpile finished in 4.33 s
    [15:00:23]  preprocess started ...
    [15:00:23]  deeplinks started ...
    [15:00:23]  deeplinks finished in 17 ms
    [15:00:23]  preprocess finished in 18 ms
    [15:00:23]  webpack started ...
    [15:00:23]  copy finished in 4.59 s
    [15:00:33]  webpack finished in 10.46 s
    [15:00:33]  sass started ...
    [15:00:35]  sass finished in 1.41 s
    [15:00:35]  postprocess started ...
    [15:00:35]  postprocess finished in 75 ms
    [15:00:35]  lint started ...
    [15:00:35]  build dev finished in 16.33 s
    [15:00:38]  dev server running: http://localhost:8100/ 

In a windows/unix shell console. This will build a Web app (tsify, sass, and webpack bundler...) in the standard Convertigo <project>/DisplayObjects/mobile directory. The watch monitors changes in any of the ionic/app files and regenerates an app in the DisplayObjects mobile directory.

This way each time you modify a file, a valid app is ready to be tested in the web browser or built with local or Convertigo cloud build.

Each time the watch task is finished, you will be able to :

- Run the mobile app in the Convertigo test platform as usual (Right Click on MobileApplication Object->Launch in test Platform)
- Run the mobile app Full Screen (Right Click on MobileApplication Object->Launch FullScreen)
- Deploy the project on a Convertigo server with right click -> Deploy. And then use Cloud build to generate your App using the test platform. (Click on the desired platform Build Device)  
- You can also use local Build to build a APK/IPA/XAP (Right click on a Mobile platform -> Cordova->Local build)

## Deploy your project on a integration/production server ##

As in the Convertigo project you have an [ionic] directory with the **node\_modules** installed, If you use Convertigo Studio 7.4.3, Convertigo Studio will automatically ignore all the node\_modules directory in a project for exporting or deploying .CAR files.

## Adding ionic native plugins ##
If you want to use some Ionic native plugins do not use the ionic CLI to add them as their description will end up in <project>/ionic/config.xml. This file is not used by the Convertigo remote build or local build system. This is why you must add the plugins in the
 
	<Project>/DisplayObjects/Platforms/<platform>/config.xml

## Introducing Convertigo Angular Framework (CAF)
CAF brings to Angular 2 / Ionic 2 the same functionalities that CTF (Convertigo Templating Framework)  brings to JQuery.  The goal is to add Convertigo back end support directly in to the Ionic HTML templates without having to program complex TypeScript. For example calling a Convertigo sequence from a button is as simple as that:

	<button ion-button full (click)="call('.Login')">Click Me</button>

CAF will automatically handle the sequence call, using the configured endpoint. Displaying data from a Sequence response, for example assuming that sequence returns a 

	{
		"login": "some data"
	}

Object, is as simple as that:

	<ion-item>{{listen([".Login"])?.login}}</ion-item>

See? No other TypeScript to Write !

## CAF Forms ##
CAF also handles forms easily. To submit a form to a Convertigo sequence (for example "Login") just use:

	<form (ngSubmit)="call('.Login')">
		....
		<input type="text" [(ngModel)]="form.user">
		....
		<input type="text" [(ngModel)]="form.password">
		...
		<button type="submit">
	</form>

The login sequence will be called and automatically will receive a 'user' and a 'password' variable populated by the user input.

## CAF routing table ##
CAF also helps by providing a page navigation routing table just as CTF doses. The concept is to configure in the table the pages that must be displayed when a Convertigo Sequence response is received by the mobile app.

CAF routing table is built using CAF TypeScript Objects so you can use IDE completion to write your routing.

The template provides a sample routing table you may use and extend for your app. The routing table is held in the app.component.ts file :

	this.router.addRouteListener(new C8oRouteListener([".Login"])           // When a response comes from ".Login" requestable,
    	.addRoute(
            new C8oRoute(
                (data:any)=>{                                               // and that login == "ok",
                    return data.login == "ok" ? true : false
                },
                tableOptions                                                // Use optional routing tables options defined higher,
            )
            .setTarget("root", Page1)                                       // and route( set as root on stack to display page) to Page1.
        )
        .addRoute(
            new C8oRoute(
                (data:any)=>{
                    return data.login == "ko"                               // If instead login == "ko",
                }
            )
            .setTarget("toast")                                             // Display a Toast with the following options.
            .setToastMesage("Your login or password is incorrect")
            .setToastDuration(5000)
            .setToastPosition("bottom")
        )
        .addFailedRoute(                                                    // When a requestable fails (Network error for example),
            new C8oRoute(
                (exception:any)=>{
                    return true                                             // In any case,
                }
            )
            .setTarget("toast")                                             // Display a Toast with the following options.
            .setToastMesage("No network connection")
            .setToastDuration(5000)
            .setToastPosition("bottom")
        )
    )

CAF is still under development but RC5 brings most of the functionnalities. Please use support forum as documentation is not yet ready.

