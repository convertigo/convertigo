new Promise((resolve, reject) => {
    let q:string = $page$; // qname of page
    let p:string = q.substring(q.lastIndexOf('.')+1);
    this.routerProvider.setRoot(this.getPageByName(p),{},{animate: true, duration: 250})
    .then((res:any) => {resolve(res)}).catch((error:any) => {reject(error)})
})
.then((res:any) => {
/*=c8o_Then*/
}, (error: any) => {console.log("[MB] PushPage : ", error.message);throw new Error(error);})