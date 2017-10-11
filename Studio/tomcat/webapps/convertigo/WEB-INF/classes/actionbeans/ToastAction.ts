this.routerProvider.toastCtrl.create({message: $message$, duration: $duration$, position: $position$}).present()
.then((res:any) => {
/*=c8o_Then*/
}, (error: any) => {console.log("[MB] ToastAction : ", error.message);throw new Error(error);})