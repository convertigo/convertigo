new Promise((resolve, reject) => {
    let r:string = $requestable$; let v:string = $verb$; let m:string = $marker$;
    let rvm:string = r + '.' + v + (m != '' ? '#':'')+ m;
    this.call(rvm,this.merge({},{/*=c8o_Vars*/}),null,500)
    .then((res:any) => {resolve(res)}).catch((error:any) => {reject(error)})
})
.then((res:any) => {
/*=c8o_Then*/
}, (error: any) => {console.log("[MB] CallFullSync : ", error.message);throw new Error(error);})